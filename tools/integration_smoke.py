#!/usr/bin/env python3
"""Run real optional-provider contracts in an isolated online-mode Fabric server.
Requires verified JARs in build/integration-mods and explicit EULA acceptance.
Synthetic online players exercise provider APIs; this does not test authenticated clients.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import selectors
import subprocess
import sys
import tempfile
import time

parser = argparse.ArgumentParser()
parser.add_argument('--accept-eula', action='store_true', required=True)
parser.parse_args()
root = Path(__file__).resolve().parent.parent
reports = root / 'build/integration-smoke'
reports.mkdir(parents=True, exist_ok=True)
(reports / 'result.json').unlink(missing_ok=True)
checks = []
with tempfile.TemporaryDirectory(prefix='runtime-', dir=reports) as tmp:
    runtime = Path(tmp)
    (runtime / 'eula.txt').write_text('eula=true\n')
    (runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=60000\n')
    command = [sys.executable, str(root / 'tools/gradle_cloud.py'), '-PintegrationSmoke', '-PprofileSmoke',
               '-PshirokaiRunDir=' + os.path.relpath(runtime, root), '-Dorg.gradle.jvmargs=-Xmx4G', '--max-workers=2', 'runServer', '--args=nogui']
    process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    output = ''
    def wait(token, cursor=0, timeout=600):
        global output
        deadline = time.monotonic() + timeout
        while token not in output[cursor:]:
            if time.monotonic() > deadline:
                raise RuntimeError(f'Timeout waiting for {token}; see {reports / "server.log"}')
            for key, _ in selector.select(0.2):
                chunk = os.read(key.fileobj.fileno(), 65536)
                if chunk:
                    output += chunk.decode(errors='replace')
                    (reports / 'server.log').write_text(output)
                else:
                    selector.unregister(key.fileobj)
            if 'INTEGRATIONS_SMOKE_FAIL' in output[cursor:] and token not in ('BUILD SUCCESSFUL', 'Stopping server'):
                raise RuntimeError('Optional-provider assertions failed; see build/integration-smoke/server.log')
            if process.poll() is not None and token not in output[cursor:]:
                raise RuntimeError(f'Server exited {process.returncode} before {token}; see server.log')
    def send(command, expected, timeout=90):
        cursor = len(output)
        process.stdin.write((command + '\n').encode()); process.stdin.flush()
        wait(expected, cursor, timeout)
    try:
        wait('Done (')
        checks.append('Fabric startup with Cobblemon 1.8.3, Impactor 5.3.5, PAPI 2.4.2, LuckPerms 5.4.140 and Shirokai Dex')
        send('shirokaicore integrations', 'currencies=impactor:dollars,impactor:tokens')
        checks.append('Core optional-provider lifecycle and diagnostics')
        send('shirokaicore clans list', 'Clãs: []')
        checks.append('console clan administration list with permission and module gating')
        send('shirokai-integrations-smoke', 'INTEGRATIONS_SMOKE_PASS')
        checks += ['actual PAPI import arguments and literal native export', 'native gym objective without display takeover',
                   'StyleEngine consumes external PAPI arguments', 'actual LuckPerms grant revoke prefix and automatic primary rank changes',
                   'persisted style with actual LuckPerms permission loss fallback and recovery',
                   'actual Impactor dollars tokens cash distinct balances zero cash reload and PAPI export without primary fallback',
                   'actual Cobblemon species registry and official acquisition updates Pokedex snapshot',
                   'actual Shirokai Dex expansion and consistent provider switching through PAPI',
                   'missing accounts display zero for dollars tokens cash without creation; missing currency remains unavailable', 'PAPI foreign replacement preserved and disconnect cache cleared']
    finally:
        if process.poll() is None:
            try:
                if 'Stopping server' not in output: send('stop', 'Stopping server', 30)
                wait('BUILD SUCCESSFUL', timeout=90)
                process.wait(timeout=20)
            except BaseException:
                process.terminate()
                process.wait(timeout=20)
                raise
        selector.close()
    if process.returncode:
        raise RuntimeError(f'Server exit status {process.returncode}')
    checks.append('clean provider shutdown')
mods = [{'file': p.name, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((root / 'build/integration-mods').glob('*.jar'))]
result = {'passed': checks, 'count': len(checks), 'mods': mods,
          'not_tested': ['authenticated player login', 'real client rendering', 'LuckPerms world contexts with real connected players', 'wild captures or load testing']}
(reports / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
