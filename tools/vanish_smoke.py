#!/usr/bin/env python3
"""Real optional Vanish API/mixins; synthetic clients only, online-mode unchanged."""
import argparse,json,os,selectors,subprocess,sys,tempfile,time
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--accept-eula',action='store_true',required=True);parser.parse_args()
root=Path(__file__).resolve().parent.parent
reports=root/'build/vanish-smoke';reports.mkdir(parents=True,exist_ok=True)
(reports/'result.json').unlink(missing_ok=True)
with tempfile.TemporaryDirectory(prefix='runtime-',dir=reports) as tmp:
    runtime=Path(tmp);(runtime/'eula.txt').write_text('eula=true\n')
    (runtime/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=60000\n')
    config=runtime/'config/shirokai-core/tab.json';config.parent.mkdir(parents=True,exist_ok=True)
    config.write_text(json.dumps({'schemaVersion':1,'updateTicks':20,'managePlayerNames':True,'manageTeams':False,'nameTemplate':'%shirokai:player% %shirokai:online%','layouts':[{'id':'default','priority':0,'condition':{'worlds':[]},'header':['Online: %shirokai:online%'],'footer':[]}],'nameRules':[]}))
    command=[sys.executable,str(root/'tools/gradle_cloud.py'),'-PvanishSmoke','-PprofileSmoke','-PshirokaiRunDir='+os.path.relpath(runtime,root),'--max-workers=2','runServer','--args=nogui']
    process=subprocess.Popen(command,cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    selector=selectors.DefaultSelector();selector.register(process.stdout,selectors.EVENT_READ);output=''
    def wait(token,cursor=0,timeout=240):
        global output
        deadline=time.monotonic()+timeout
        while token not in output[cursor:]:
            if time.monotonic()>deadline:raise RuntimeError('Timeout '+token+'; see build/vanish-smoke/server.log')
            for key,_ in selector.select(.2):
                chunk=os.read(key.fileobj.fileno(),65536)
                if chunk:output+=chunk.decode(errors='replace');(reports/'server.log').write_text(output)
                else:selector.unregister(key.fileobj)
            if 'VANISH_SMOKE_FAIL' in output[cursor:] and token not in ('Stopping server','BUILD SUCCESSFUL'):raise RuntimeError('Vanish assertions failed; see build/vanish-smoke/server.log')
            if process.poll() is not None and token not in output[cursor:]:raise RuntimeError('Server exited before '+token)
    def send(command,token):
        cursor=len(output);process.stdin.write((command+'\n').encode());process.stdin.flush();wait(token,cursor)
    try:
        wait('Done (');send('shirokaicore tab','vanish=READY');send('shirokai-vanish-smoke','VANISH_SMOKE_PASS')
    finally:
        if process.poll() is None:
            try:send('stop','Stopping server');wait('BUILD SUCCESSFUL');process.wait(timeout=30)
            except BaseException:process.terminate();process.wait(timeout=30);raise
        selector.close()
    if process.returncode:raise RuntimeError('Vanish server exit status '+str(process.returncode))
result={'count':7,'passed':['audited Vanish1.5.9+1.21.1 actual API and server mixins loaded','actual entity tracking and invisibility gates', 'roster removal from normal viewer and seer visibility','Core chat audiences, private-message privacy, clan autocomplete and member labels','Core TAB excludes hidden names and per-viewer online count','hidden players excluded from global Core teams','unvanish restores visible count and clean shutdown'],'not_tested':['authenticated player login','real client/entity rendering','all third-party mods or network side channels']}
(reports/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
