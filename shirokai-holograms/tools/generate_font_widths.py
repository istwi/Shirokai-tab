#!/usr/bin/env python3
"""Optional maintainer tool. Needs Pillow; reads official 1.21.1 client JAR and verified assets.
Only numeric advances are emitted; no Minecraft textures or font bitmaps are redistributed.
"""
import hashlib, io, json, re, sys, urllib.request, zipfile
from pathlib import Path
from PIL import Image
client = Path(sys.argv[1])
if hashlib.sha256(client.read_bytes()).hexdigest() != '499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99':
    raise SystemExit('Expected official Minecraft 1.21.1 client JAR')
def asset(digest):
    data = urllib.request.urlopen('https://resources.download.minecraft.net/'+digest[:2]+'/'+digest).read()
    assert hashlib.sha1(data).hexdigest() == digest, 'Asset hash mismatch'
    return data
widths = {32:[4,1],8204:[0,1]}
with zipfile.ZipFile(client) as archive:
    for provider in json.loads(archive.read('assets/minecraft/font/include/default.json'))['providers']:
        image = Image.open(io.BytesIO(archive.read('assets/minecraft/textures/'+provider['file'].split(':')[1]))).convert('RGBA')
        rows=provider['chars'];cols=len(rows[0]);cw=image.width//cols;ch=image.height//len(rows);scale=provider.get('height',8)/ch
        for row,text in enumerate(rows):
            for col,char in enumerate(text):
                cp=ord(char)
                if cp==0 or cp in widths: continue
                bounds=image.crop((col*cw,row*ch,(col+1)*cw,(row+1)*ch)).getchannel('A').getbbox()
                widths[cp]=[int((0 if bounds is None else bounds[2])*scale+.5)+1,1]
providers=json.loads(re.sub(r',\s*]',']',asset('f40266672fa6cd5c678cbfd5a7fdcfe06e30c629').decode()))['providers']
provider=next(p for p in providers if p['hex_file']=='minecraft:font/unifont.zip')
with zipfile.ZipFile(io.BytesIO(asset('666999116c467f10622db1527a06ddc0a6efad2a'))) as archive:
    for name in archive.namelist():
        if not name.endswith('.hex'): continue
        for line in archive.read(name).decode().splitlines():
            cp_hex,bitmap=line.split(':'); cp=int(cp_hex,16)
            if cp in widths: continue
            digits=len(bitmap)//16;bits=digits*4
            union=0
            for row in range(16): union |= int(bitmap[row*digits:(row+1)*digits],16)
            left=bits-union.bit_length() if union else 0
            right=bits-1-((union & -union).bit_length()-1) if union else -1
            for override in provider['size_overrides']:
                if ord(override['from'])<=cp<=ord(override['to']):left,right=override['left'],override['right']
            widths[cp]=[(right-left+1)//2+1,.5]
output=Path(__file__).resolve().parents[1]/'src/main/resources/vanilla-widths.json'
output.write_text(json.dumps(widths,separators=(',',':'))+'\n')
print(len(widths),'measured glyph advances')
