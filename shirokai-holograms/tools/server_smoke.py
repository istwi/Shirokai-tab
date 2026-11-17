#!/usr/bin/env python3
"""Real isolated Fabric boots, commands and restart. No authentication/graphics claim."""
import argparse,json,os,selectors,subprocess,sys,tempfile,time
from pathlib import Path
parser=argparse.ArgumentParser()
parser.add_argument('--accept-eula',action='store_true',required=True)
parser.add_argument('--cobblemon',action='store_true')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
reports=root/('build/server-smoke-cobblemon' if args.cobblemon else 'build/server-smoke');reports.mkdir(parents=True,exist_ok=True)
result_file=reports/'result.json';result_file.unlink(missing_ok=True)
class Server:
    def __init__(self,runtime,label):
        self.output='';self.label=label;self.log=reports/(label+'.log')
        self.process=subprocess.Popen([sys.executable,str(root/'tools/gradle_cloud.py'),'-PhologramSmoke',*(['-PhologramCobblemon'] if args.cobblemon else []),'-PhologramRunDir='+os.path.relpath(runtime,root),'runServer','--args=nogui'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        self.selector=selectors.DefaultSelector();self.selector.register(self.process.stdout,selectors.EVENT_READ)
    def wait(self,token,offset=0,timeout=180):
        deadline=time.monotonic()+timeout
        while token not in self.output[offset:]:
            if time.monotonic()>deadline:raise RuntimeError(self.label+': timeout waiting for '+token+'; see '+str(self.log))
            for key,_ in self.selector.select(.2):
                chunk=os.read(key.fileobj.fileno(),65536)
                if chunk:self.output+=chunk.decode(errors='replace');self.log.write_text(self.output)
                else:self.selector.unregister(key.fileobj)
            if 'HOLOGRAM_SMOKE_FAIL' in self.output:raise RuntimeError('Fixture failed; see '+str(self.log))
            if self.process.poll() is not None and token not in self.output[offset:]:raise RuntimeError(self.label+': exit '+str(self.process.returncode)+'; see '+str(self.log))
    def command(self,command,expected):
        offset=len(self.output);self.process.stdin.write((command+'\n').encode());self.process.stdin.flush();self.wait(expected,offset)
    def stop(self):
        if self.process.poll() is None:
            self.command('stop','Stopping server');self.wait('BUILD SUCCESSFUL',timeout=60);self.process.wait(timeout=15)
        if self.process.returncode!=0:raise RuntimeError('Unclean server shutdown')
        self.selector.close()
checks=[]
with tempfile.TemporaryDirectory(prefix='runtime-',dir=reports) as temporary:
    runtime=Path(temporary);(runtime/'eula.txt').write_text('eula=true\n')
    (runtime/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\n')
    config=runtime/'config/shirokai-holograms';config.mkdir(parents=True)
    hologram={'id':'restart','world':'minecraft:overworld','x':0,'y':66,'z':0,'alignment':'CENTER','lines':[{'text':'<gold><bold>Restart OK</bold></gold>','animation':'STATIC'}],'lineSpacing':12,'scale':1,'backgroundColor':0,'backgroundOpacity':0,'shadow':True,'yaw':0,'pitch':0,'billboard':'CENTER'}
    (config/'holograms.json').write_text(json.dumps({'schemaVersion':1,'holograms':[hologram]}))
    first=Server(runtime,'first-boot')
    try:
        first.wait('Done (',timeout=300 if args.cobblemon else 180);first.wait('[holograms] 1 hologramas carregados')
        first.command('hologram edit restart','Restart OK')
        first.command('shirokai-holograms-smoke','HOLOGRAM_SMOKE_PASS')
        checks=json.loads((runtime/'hologram-smoke-result.json').read_text())
        checks.append('first real Fabric boot and JSON definition loaded')
    finally:first.stop()
    second=Server(runtime,'restart')
    try:
        second.wait('Done (',timeout=300 if args.cobblemon else 180);second.wait('[holograms] 1 hologramas carregados')
        second.command('hologram list','[restart]');second.command('hologram edit restart','Restart OK')
        checks.append('second real Fabric boot restores exactly one definition')
        second.command('hologram delete restart','Holograma apagado: restart')
        second.command('hologram reload','Hologramas recarregados')
        second.command('hologram list','Hologramas: []')
        checks.append('persistent deletion and reload do not resurrect definition')
    finally:second.stop()
result_file.write_text(json.dumps({'passed':len(checks),'failed':0,'boots':2,'cobblemon':'1.8.3+1.21.1' if args.cobblemon else None,'checks':checks},indent=2)+'\n')
print('PASS:',len(checks),'checks, 2 Fabric boots;',result_file)
