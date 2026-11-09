#!/usr/bin/env python3
"""Download the audited optional Vanish artifact and extract its unchanged nested dependencies."""
import hashlib, json, subprocess, zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
jar=root/'build/vanish-mod/vanish-1.5.9+1.21.1.jar'
jar.parent.mkdir(parents=True,exist_ok=True)
if not jar.exists():
    subprocess.run(['gh','release','download','1.5.9+1.21.1','--repo','DrexHD/Vanish','--pattern',jar.name,'--dir',str(jar.parent)],check=True)
expected='728fcc7c39e9c045924c58f50004f4f42d1ea1dc3995da34556d83441a0bc9a1'
if hashlib.sha256(jar.read_bytes()).hexdigest()!=expected:raise SystemExit('Vanish checksum mismatch')
with zipfile.ZipFile(jar) as z:
    metadata=json.loads(z.read('fabric.mod.json'))
    assert metadata['id']=='melius-vanish' and metadata['version']=='1.5.9+1.21.1'
    for name in z.namelist():
        if name.startswith('META-INF/jars/') and name.endswith('.jar'):
            data=z.read(name)
            import io
            with zipfile.ZipFile(io.BytesIO(data)) as nested:
                folder='mods' if 'fabric.mod.json' in nested.namelist() else 'libraries'
            destination=root/'build/vanish-deps-1.5.9'/folder/Path(name).name
            destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(data)
print('Verified Vanish1.5.9+1.21.1 and extracted unchanged nested dependencies; test runtime requires Fabric Loader0.17.2')
