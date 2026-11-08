#!/usr/bin/env python3
"""Exercise a real, isolated Fabric server. Requires explicit EULA acceptance flag."""
import argparse
import json
import os
from pathlib import Path
import selectors
import sqlite3
import subprocess
import sys
import tempfile
import time

parser = argparse.ArgumentParser()
parser.add_argument('--accept-eula', action='store_true', required=True)
parser.parse_args()
root = Path(__file__).resolve().parent.parent
reports = root / 'build/server-smoke'
reports.mkdir(parents=True, exist_ok=True)
(reports / 'result.json').unlink(missing_ok=True)

class Server:
    def __init__(self, runtime, label, profile=False):
        self.label = label
        self.log = reports / (label + '.log')
        self.output = ''
        command = [sys.executable, str(root / 'tools/gradle_cloud.py'),
            '-PshirokaiRunDir=' + os.path.relpath(runtime, root)]
        if profile:
            command += ['-PprofileSmoke']
        command += ['runServer', '--args=nogui']
        self.process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        self.selector = selectors.DefaultSelector()
        self.selector.register(self.process.stdout, selectors.EVENT_READ)
    def wait(self, token, offset=0, timeout=150):
        deadline = time.monotonic() + timeout
        while token not in self.output[offset:]:
            if time.monotonic() > deadline:
                raise RuntimeError(f'{self.label}: timeout waiting for {token}; see {self.log}')
            for key, _ in self.selector.select(0.2):
                chunk = os.read(key.fileobj.fileno(), 65536)
                if chunk:
                    self.output += chunk.decode('utf-8', errors='replace')
                    self.log.write_text(self.output)
                else:
                    self.selector.unregister(key.fileobj)
            if self.process.poll() is not None and token not in self.output[offset:]:
                raise RuntimeError(f'{self.label}: exit {self.process.returncode} before {token}; see {self.log}')
    def command(self, command, expected):
        cursor = len(self.output)
        self.process.stdin.write((command + '\n').encode())
        self.process.stdin.flush()
        self.wait(expected, cursor, 30)
    def stop(self):
        if self.process.poll() is None:
            self.command('stop', 'Stopping server')
            self.wait('BUILD SUCCESSFUL', timeout=60)
            self.process.wait(timeout=20)
        if self.process.returncode != 0:
            raise RuntimeError(f'{self.label}: server did not stop cleanly')
        self.selector.close()

checks = []
with tempfile.TemporaryDirectory(prefix='runtime-', dir=reports) as tmp:
    runtime = Path(tmp)
    (runtime / 'eula.txt').write_text('eula=true\n')
    (runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=60000\n')
    server = Server(runtime, 'first-boot', profile=True)
    try:
        server.wait('Done (')
        server.wait('SQLite pronto:')
        server.command('shirokaicore status', 'SQLite=READY')
        server.command('shirokaicore modules', 'administration=RUNNING')
        server.command('shirokaicore version', 'Shirokai Core 0.13.4-dev')
        server.command('shirokaicore permissions', 'vanilla-op4')
        server.command('shirokaicore placeholders %shirokai:online%/%shirokai:dollars%', '0/N/D')
        server.command('shirokaicore debug', 'SQLite integrity_check=ok')
        checks += ['real Fabric startup', 'module status', 'version', 'permission fallback', 'literal placeholders', 'SQLite integrity']
        server.command('shirokai-profile-smoke', 'PROFILE_GUI_SMOKE_PASS')
        checks.append('native menu server-side interactions with synthetic players')
        server.command('shirokai-clan-menu-smoke', 'CLAN_MENU_SMOKE_PASS')
        checks.append('native clan menus text editor sent invites cancellation roles transfer and two-step deletion')
        server.command('shirokai-clan-smoke', 'CLAN_SMOKE_PASS')
        checks.append('native clan transactions limits transfer and confirmed disband')
        server.command('clan', '/clan exige um jogador')
        checks.append('clan command rejects console')
        server.command('shirokai-teams-smoke', 'TEAM_OWNERSHIP_SMOKE_PASS')
        checks.append('team ownership scenarios run with Fabric registries')
        server.command('shirokai-tab-smoke', 'TAB_SMOKE_PASS')
        server.command('shirokai-tab-extras-smoke', 'TAB_EXTRAS_SMOKE_PASS')
        checks.append('opt-in listed nametags below-name bossbars ownership diffs collisions and cleanup')
        checks.append('TAB packets selected title layouts delta updates teams and animations')
        server.command('shirokaicore tab', 'viewers=')
        server.command('shirokai-scoreboard-smoke', 'SCOREBOARD_SMOKE_PASS')
        checks.append('native scoreboard packets diff shared title visibility and foreign sidebar ownership')
        server.command('shirokaicore scoreboard', 'viewers=')
        server.command('shirokai-style-smoke', 'STYLE_CHOICE_SMOKE_PASS')
        checks.append('persisted style native menu fallback recovery independent clan choices and stale authorization')
        server.command('shirokai-presentation-smoke', 'PRESENTATION_SMOKE_PASS')
        checks.append('rich chat menu title clan presentation permission fallback and filtered mention routing')
        server.command('shirokai-chat-smoke', 'CHAT_SMOKE_PASS')
        server.command('shirokai-chat-appearance-smoke', 'CHAT_APPEARANCE_SMOKE_PASS')
        checks.append('gray global yellow local legacy colors filtering aliases and transformed join quit announcements')
        checks.append('chat transport channels private moderation ignore and literal payloads')
        server.command('shirokai-chat-alias-smoke', 'CHAT_ALIAS_SMOKE_PASS')
        server.command('reload', 'DATAPACK_RELOAD_SMOKE_READY')
        server.command('shirokai-chat-alias-smoke', 'CHAT_ALIAS_SMOKE_PASS')
        checks.append('vanilla private aliases remain moderated after datapack reload')
        subject = '11111111-1111-1111-1111-111111111111'
        server.command('shirokaicore titles list', 'traveler')
        server.command(f'shirokaicore titles grant {subject} traveler', 'grants={traveler=0}')
        server.command(f'shirokaicore titles grant {subject} traveler 120', 'grants={traveler=')
        server.command(f'shirokaicore titles inspect {subject}', 'grants={traveler=')
        server.command(f'shirokaicore titles revoke {subject} traveler', 'grants={}')
        server.command(f'shirokaicore titles grant {subject} traveler', 'grants={traveler=0}')
        server.command('perfil', '/perfil exige um jogador')
        server.command(f'shirokaicore chat mute {subject} 0 Test', 'mute=Mute[expiresAt=0')
        server.command(f'shirokaicore chat inspect {subject}', 'mute=Mute[expiresAt=0')
        checks.append('persistent administrative mute')
        checks += ['title catalog registered', 'permanent and temporary grants', 'revoke persisted', 'profile rejects console']

        config_file = runtime / 'config/shirokai-core/core.json'
        valid = config_file.read_text()
        config = json.loads(valid)
        config['unavailablePlaceholder'] = 'Unavailable'
        customized = json.dumps(config)
        config_file.write_text(customized)
        server.command('shirokaicore reload', 'Configuração recarregada; estados ativos preservados')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        config_file.write_text('{broken JSON')
        server.command('shirokaicore reload', 'Shirokai Core:')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        config_file.write_text(customized)
        titles_file = runtime / 'config/shirokai-core/titles.json'
        original_titles = titles_file.read_text()
        titles_file.write_text('{broken')
        altered_config = dict(config)
        altered_config['unavailablePlaceholder'] = 'MUST_NOT_APPLY'
        config_file.write_text(json.dumps(altered_config))
        server.command('shirokaicore reload', 'Shirokai Core:')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        titles_file.write_text(original_titles)
        config_file.write_text(customized)
        checks.append('invalid titles catalog rejects complete reload atomically')
        style_file = runtime / 'config/shirokai-core/styles.json'
        original_style = style_file.read_text()
        style = json.loads(original_style)
        style['animations']['gold_wave']['frames'] = ["<click:run_command:/op>unsafe</click>"]
        style_file.write_text(json.dumps(style))
        bad_core = dict(config)
        bad_core['unavailablePlaceholder'] = 'MUST_NOT_APPLY_STYLE'
        config_file.write_text(json.dumps(bad_core))
        server.command('shirokaicore reload', 'Tag não permitida')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        style_file.write_text(original_style)
        config_file.write_text(customized)
        tab_file = runtime / 'config/shirokai-core/tab.json'
        original_tab = tab_file.read_text()
        tab = json.loads(original_tab)
        tab['nameTemplate'] = '%animation:missing%'
        tab_file.write_text(json.dumps(tab))
        server.command('shirokaicore reload', 'Animação desconhecida')
        tab = json.loads(original_tab)
        tab['manageTeams'] = True
        tab_file.write_text(json.dumps(tab))
        server.command('shirokaicore reload', 'ownership do TAB exige reinício')
        tab_file.write_text(original_tab)
        checks += ['styles and TAB validation preserves active reload', 'TAB ownership change requires restart']
        scoreboard_file = runtime / 'config/shirokai-core/scoreboard.json'
        original_scoreboard = scoreboard_file.read_text()
        scoreboard = json.loads(original_scoreboard)
        scoreboard['layouts'][0]['title'] = '%animation:missing%'
        scoreboard_file.write_text(json.dumps(scoreboard))
        rejected = dict(config)
        rejected['unavailablePlaceholder'] = 'MUST_NOT_APPLY_SCOREBOARD'
        config_file.write_text(json.dumps(rejected))
        server.command('shirokaicore reload', 'Animação desconhecida')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        scoreboard_file.write_text(original_scoreboard)
        config_file.write_text(customized)
        checks.append('invalid scoreboard candidate rejects complete reload atomically')
        presentation_file = runtime / 'config/shirokai-core/presentation.json'
        original_presentation = presentation_file.read_text()
        presentation = json.loads(original_presentation)
        presentation['templates']['TITLE'] = '<click:run_command:/op>%visual:value%</click>'
        presentation_file.write_text(json.dumps(presentation))
        rejected_visual = dict(config)
        rejected_visual['unavailablePlaceholder'] = 'MUST_NOT_APPLY_PRESENTATION'
        config_file.write_text(json.dumps(rejected_visual))
        server.command('shirokaicore reload', 'Tag não permitida')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        presentation_file.write_text(original_presentation)
        config_file.write_text(customized)
        checks.append('invalid presentation rejects all reload snapshots atomically')
        integrations_file = runtime / 'config/shirokai-core/integrations.json'
        original_integrations = integrations_file.read_text()
        integrations = json.loads(original_integrations)
        integrations['tokensCurrency'] = integrations['dollarsCurrency']
        integrations_file.write_text(json.dumps(integrations))
        server.command('shirokaicore reload', 'Configuração de integrações inválida')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        integrations = json.loads(original_integrations)
        integrations['impactor'] = False
        integrations_file.write_text(json.dumps(integrations))
        server.command('shirokaicore reload', 'Ativação de integrações exige reinício')
        integrations_file.write_text(original_integrations)
        checks.append('invalid integration mappings and enablement changes preserve complete reload')
        chat_file = runtime / 'config/shirokai-core/chat.json'
        original_chat = chat_file.read_text()
        chat_file.write_text('{broken')
        server.command('shirokaicore reload', 'Shirokai Core:')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        chat = json.loads(original_chat)
        chat['takeVanillaPrivateCommands'] = False
        chat_file.write_text(json.dumps(chat))
        server.command('shirokaicore reload', 'takeVanillaPrivateCommands exige reinício')
        server.command('shirokai-chat-alias-smoke', 'CHAT_ALIAS_SMOKE_PASS')
        chat_file.write_text(original_chat)
        checks += ['invalid chat config preserves active reload', 'private command ownership change requires restart']
        presets_file = runtime / 'config/shirokai-core/presets.json'
        original_presets = presets_file.read_text()
        invalid_presets = json.loads(original_presets)
        invalid_presets['presets']['basic']['template'] = '<aqua>%visual:value%%visual:value%</aqua>'
        presets_file.write_text(json.dumps(invalid_presets))
        server.command('shirokaicore reload', 'Preset exige um único')
        server.command('shirokaicore status', 'SQLite=READY')
        presets_file.write_text(original_presets)
        server.command('shirokaicore reload', 'Configuração recarregada')
        checks.append('invalid preset catalog rejected atomically and corrected catalog reloads')
        extras_file = runtime / 'config/shirokai-core/tab-extras.json'
        original_extras = extras_file.read_text()
        invalid_extras = json.loads(original_extras)
        invalid_extras['bossbar']['progress'] = 'NaN'
        extras_file.write_text(json.dumps(invalid_extras))
        server.command('shirokaicore reload', 'Progresso exige decimal')
        extras_file.write_text(original_extras)
        ownership_extras = json.loads(original_extras)
        ownership_extras['bossbar']['enabled'] = True
        extras_file.write_text(json.dumps(ownership_extras))
        server.command('shirokaicore reload', 'Ownership de extras do TAB exige reinício')
        extras_file.write_text(original_extras)
        server.command('shirokaicore reload', 'Configuração recarregada')
        checks.append('invalid extra numeric config and ownership reload rejected atomically')
        appearance_file = runtime / 'config/shirokai-core/chat-appearance.json'
        original_appearance = appearance_file.read_text()
        bad_appearance = json.loads(original_appearance)
        bad_appearance['bodyColors']['GLOBAL'] = 'rainbow'
        appearance_file.write_text(json.dumps(bad_appearance))
        server.command('shirokaicore reload', 'Cor inválida para GLOBAL')
        appearance_file.write_text(original_appearance)
        server.command('shirokaicore reload', 'Configuração recarregada')
        checks.append('invalid chat appearance rejected atomically and corrected appearance reloads')
        config['modules']['storage'] = False
        config_file.write_text(json.dumps(config))
        server.command('shirokaicore reload', 'exige reinício; configuração ativa preservada')
        server.command('shirokaicore status', 'SQLite=READY')
        config_file.write_text(customized)
        server.command('shirokaicore backup', 'Backup criado:')
        checks += ['valid reload', 'invalid reload preserves active state', 'restart-only change rejected', 'consistent backup']
    finally:
        server.stop()
    checks.append('clean shutdown')
    database = runtime / 'shirokai-core/shirokai.db'
    with sqlite3.connect(database) as connection:
        assert connection.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
        assert connection.execute('PRAGMA user_version').fetchone()[0] == 6
        assert connection.execute('SELECT count(*) FROM admin_audit').fetchone()[0] >= 2
    backups = list((runtime / 'shirokai-core/backups').glob('*.db'))
    assert len(backups) == 1
    with sqlite3.connect(backups[0]) as connection:
        assert connection.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    server = Server(runtime, 'restart')
    try:
        server.wait('Done (')
        server.wait('SQLite pronto:')
        server.command('shirokaicore status', 'SQLite=READY')
        server.command('shirokaicore placeholders %shirokai:dollars%', 'Unavailable')
        server.command('shirokaicore debug', 'SQLite integrity_check=ok')
        server.command(f'shirokaicore titles inspect {subject}', 'grants={traveler=0}')
        server.command(f'shirokaicore chat inspect {subject}', 'mute=Mute[expiresAt=0')
        server.command(f'shirokaicore chat unmute {subject}', 'mute=null')
        checks.append('mute survives server restart and unmute persists')
        checks.append('title grant survives server restart')
    finally:
        server.stop()
    checks += ['database survives restart', 'custom config survives restart', 'backup independently readable', 'audit persisted']
    with sqlite3.connect(database) as connection:
        connection.execute('PRAGMA user_version=42')
    server = Server(runtime, 'storage-failure')
    try:
        server.wait('Done (')
        server.wait('SQLite indisponível; demais módulos independentes continuam')
        server.command('shirokaicore status', 'SQLite=FAILED')
        server.command('shirokaicore placeholders %shirokai:online%', ') 0')
        server.command('shirokaicore debug', 'Banco mais recente que o mod: schema 42')
    finally:
        server.stop()
    with sqlite3.connect(database) as connection:
        assert connection.execute('PRAGMA user_version').fetchone()[0] == 42
        assert connection.execute('SELECT count(*) FROM admin_audit').fetchone()[0] >= 2
    checks += ['storage failure isolated from server and placeholders', 'future schema preserved']

result = {'passed': checks, 'count': len(checks), 'not_tested': ['player connections', 'LuckPerms runtime', 'Cobblemon', 'Impactor', 'visual GUI and packets from a real client', 'visual TAB order and scoreboard rendering']}
(reports / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
