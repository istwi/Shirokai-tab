#!/usr/bin/env python3
"""Pinned official Cobblemon, byte-preserving extraction of nested dependencies for Loom dev only."""
import hashlib,io,json,urllib.request,zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
filename='fabric-1.8.3+1.21.1.jar'
digest='4bc3b09d25b93b74a5822dc35b06bd6f5fa8bae36ed43a8d2b3904156a61269c'
cache=Path('/workspace/.gradle-shirokai/caches/modules-2/files-2.1/com.cobblemon/fabric/1.8.3+1.21.1')
source=next((p for p in cache.rglob(filename)),None) if cache.exists() else None
if source:data=source.read_bytes()
else:data=urllib.request.urlopen('https://artefacts.cobblemon.com/releases/com/cobblemon/fabric/1.8.3+1.21.1/'+filename).read()
assert hashlib.sha256(data).hexdigest()==digest,'Cobblemon SHA-256 mismatch'
records=[];seen=set()
def extract(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        meta=json.loads(archive.read('fabric.mod.json')) if 'fabric.mod.json' in archive.namelist() else {}
        for entry in meta.get('jars',[]):
            child=archive.read(entry['file']);sha=hashlib.sha256(child).hexdigest()
            if sha in seen:continue
            seen.add(sha)
            with zipfile.ZipFile(io.BytesIO(child)) as jar:mod='fabric.mod.json' in jar.namelist()
            directory=root/'build'/('cobblemon-mods' if mod else 'cobblemon-libraries');directory.mkdir(parents=True,exist_ok=True)
            target=directory/Path(entry['file']).name
            if target.exists():assert hashlib.sha256(target.read_bytes()).hexdigest()==sha,'Existing artifact differs'
            else:target.write_bytes(child)
            records.append({'file':target.name,'sha256':sha,'mod':mod})
            if mod:extract(child)
extract(data)
(root/'build/cobblemon-dependencies.json').write_text(json.dumps(records,indent=2)+'\n')
print('Verified Cobblemon 1.8.3 and',len(records),'unchanged nested dependencies')
