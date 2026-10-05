"""Generate editable Blockbench projects and Minecraft exports from one geometry source."""
from pathlib import Path
import json, uuid, base64, io, random
import struct, zlib
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/blockzone'
PROJECT=ROOT/'examples/blockbench'
PROJECT.mkdir(parents=True, exist_ok=True)
def write(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
def cube(bone, name, xyz, size, uv=0):
 return dict(bone=bone,name=name,**{'from':xyz},size=size,u=uv,v=0)
helmet=[cube('head','shell',[-4.5,-8.5,-4.5],[9,5,9]),cube('head','rear_rim',[-4.6,-3.5,2.9],[9.2,1.5,1.7],32),cube('head','front_rail',[-3.5,-7.2,-5],[7,1,1],32),cube('head','mount',[-1.2,-6.8,-5.2],[2.4,2,1],64),cube('head','left_ear',[4,-5,-2],[1.4,3,4],32),cube('head','right_ear',[-5.4,-5,-2],[1.4,3,4],32)]
carrier=[cube('body','front_panel',[-4.5,1,-3.1],[9,9,1.5],32),cube('body','rear_panel',[-4.5,1,1.6],[9,9,1.5],32),cube('body','left_strap',[2.5,-.4,-3.1],[2,2,6.2]),cube('body','right_strap',[-4.5,-.4,-3.1],[2,2,6.2]),cube('body','belt',[-4.8,8,-3.3],[9.6,2,6.6],32),cube('body','radio',[-4,2,-4.2],[2,3,1.2],64),cube('body','pouch_left',[-3.8,5,-4.5],[3.3,3.5,1.4]),cube('body','pouch_right',[.5,5,-4.5],[3.3,3.5,1.4]),cube('body','patch',[-1.5,2.2,-3.3],[3,1.6,.3],96)]
advanced=carrier+[cube('body','expansion_back',[-3.8,2,3.1],[7.6,6,1],64),cube('body','expansion_lock',[-2,7,4.1],[4,1.5,.5],96)]
legs=[];boots=[]
for bone in ['left_leg','right_leg']:
 legs.extend([cube(bone,'fabric',[-2.2,-.1,-2.2],[4.4,9,4.4]),cube(bone,'kneepad',[-1.8,4,-3.1],[3.6,3.5,1.1],32),cube(bone,'knee_rail',[-1.5,5,-3.4],[3,1.4,.4],64)])
 boots.extend([cube(bone,'boot',[-2.3,8,-2.4],[4.6,4,5.2],32),cube(bone,'sole',[-2.4,11,-2.6],[4.8,1.1,5.6],64),cube(bone,'toe',[-2.2,9.5,-3.1],[4.4,1.5,1],64)])
models={'helmet':helmet,'carrier':carrier,'advanced_carrier':advanced,'leggings':legs,'boots':boots}
# Pixel textile palette: olive cloth, charcoal polymer, dark metal, amber identification.
pixels=bytearray(128*128*4);rnd=random.Random(17)
for x in range(128):
 for y in range(128):
  c=[(82,91,65),(49,57,58),(30,35,38),(196,153,65)][x//32];n=rnd.choice([-5,-2,0,2,4]);pixels[(y*128+x)*4:(y*128+x)*4+4]=bytes(tuple(max(0,min(255,v+n)) for v in c)+(255,))
tex=ASSETS/'textures/models/armor/tactical.png';tex.parent.mkdir(parents=True,exist_ok=True)
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,128,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+pixels[y*512:(y+1)*512] for y in range(128))))+chunk(b'IEND',b'')
tex.write_bytes(png);itemtex=ASSETS/'textures/item/tactical.png';itemtex.parent.mkdir(parents=True,exist_ok=True);itemtex.write_bytes(png);encoded='data:image/png;base64,'+base64.b64encode(png).decode()
def bb(name,cubes,animated=False):
 elements=[];groups={};origins={'head':[0,24,0],'body':[0,24,0],'left_leg':[1.9,12,0],'right_leg':[-1.9,12,0]}
 for c in cubes:
  identity=str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+c['bone']+'/'+c['name']));origin=origins.get(c['bone'],[0,0,0]);x,y,z=c['from'];w,h,d=c['size'];fr=[origin[0]+x,origin[1]-y-h,origin[2]+z]
  elements.append({'name':c['name'],'type':'cube','uuid':identity,'from':fr,'to':[fr[0]+w,fr[1]+h,fr[2]+d],'origin':origin,'box_uv':True,'uv_offset':[c['u'],c['v']],'faces':{f:{'uv':[c['u'],0,c['u']+w,h],'texture':0} for f in ['north','south','east','west','up','down']}})
  groups.setdefault(c['bone'],[]).append(identity)
 outliner=[{'name':bone,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+bone)),'origin':origins.get(bone,[0,0,0]),'children':children} for bone,children in groups.items()]
 project={'meta':{'format_version':'4.10','model_format':'free','box_uv':True},'name':name,'resolution':{'width':128,'height':128},'elements':elements,'outliner':outliner,'textures':[{'name':'tactical.png','id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'tactical_texture')),'source':encoded,'width':128,'height':128,'uv_width':128,'uv_height':128}]}
 if animated:
  boneid=outliner[0]['uuid'];positions=[(0,[0,-8,0]),(.4,[0,0,0]),(1,[0,0,0]),(1.6,[0,-4,0]),(1.7,[0,-4,0]),(2,[0,-12,0])]
  project['animations']=[{'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'plating')),'name':'animation.blockzone.plating','loop':'once','length':2,'animators':{boneid:{'name':'plate','type':'bone','keyframes':[{'channel':'position','time':t,'interpolation':'catmullrom','data_points':[dict(zip(['x','y','z'],[str(v) for v in pos]))]} for t,pos in positions]}}}]
 write(PROJECT/(name+'.bbmodel'),project)
for name,cubes in models.items():
 write(ASSETS/('models/equipment/'+name+'.json'),{'texture_size':[128,128],'cubes':cubes});bb(name,cubes)
# Item models use the same geometry scaled to fit vanilla item-model limits.
items={'tactical_helmet':'helmet','plate_carrier':'carrier','tactical_leggings':'leggings','tactical_boots':'boots'}
def itemmodel(cubes):
 elements=[]
 for c in cubes:
  x,y,z=c['from'];w,h,d=c['size'];fr=[8+x*.7,12-(y+h)*.7,8+z*.7]
  elements.append({'name':c['name'],'from':fr,'to':[fr[0]+w*.7,fr[1]+h*.7,fr[2]+d*.7],'faces':{f:{'uv':[c['u']/8,0,(c['u']+8)/8,1],'texture':'#cloth'} for f in ['north','south','east','west','up','down']}})
 return {'textures':{'cloth':'blockzone:item/tactical','particle':'blockzone:item/tactical'},'elements':elements,'display':{'gui':{'rotation':[20,-35,0],'scale':[1.1,1.1,1.1]},'ground':{'scale':[.5,.5,.5]},'fixed':{'scale':[1,1,1]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.7,.7,.7]},'firstperson_righthand':{'rotation':[0,-20,0],'translation':[0,0,0],'scale':[.7,.7,.7]}}}
for item,name in items.items():write(ASSETS/('models/item/'+item+'.json'),itemmodel(models[name]))
plate=[cube('plate','ceramic_core',[-4,0,-.5],[8,10,1],64),cube('plate','top_shoulder',[-3,-1,-.5],[6,1,1],64),cube('plate','fabric_edge',[-4.2,9.6,-.6],[8.4,.6,1.2],32),cube('plate','label',[-2.5,3,-.65],[5,2,.2],96),cube('plate','center_rib',[-.5,6,-.7],[1,2.8,.25],32)]
write(ASSETS/'models/item/armor_plate.json',itemmodel(plate));bb('armor_plate',plate,True)
expansion=[cube('attachment','plate_sleeve',[-3.8,0,-.7],[7.6,6,1.4],64),cube('attachment','locking_strap',[-2,5.5,-1],[4,1.5,2],96),cube('attachment','left_clip',[-4.2,1,-.8],[.8,3,1.6],32),cube('attachment','right_clip',[3.4,1,-.8],[.8,3,1.6],32)]
write(ASSETS/'models/item/armor_expansion.json',itemmodel(expansion));bb('armor_expansion',expansion)
