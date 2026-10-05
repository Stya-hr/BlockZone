#!/usr/bin/env python3
"""Build the four editable Java Block/Item crate projects and their pixel atlases.

Uses only Python's standard library. Game JSON is also emitted as a reproducible
baseline; the projects can be loaded and exported with Blockbench's Java codec.
"""
import base64
import copy
import json
import random
import struct
import uuid
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/blockzone'
EXAMPLES = ROOT / 'examples/loot-crates'
FACES = ('north', 'east', 'south', 'west', 'up', 'down')
TILES = {'body': 0, 'metal': 1, 'inside': 2, 'accent': 3, 'wood': 4,
         'label': 5, 'rubber': 6, 'panel': 7, 'rust': 8, 'bolt': 9,
         'hazard': 10, 'strap': 11, 'steel': 12, 'lid': 13, 'repair': 14, 'mark': 15, 'white': 16}
THEMES = {
    'military': {'block': 'loot_crate', 'body': '#555e43', 'metal': '#323a35', 'accent': '#b9a574', 'panel': '#454e38', 'label': 'BZ04', 'top': 9, 'z': (1, 15), 'size': (-4,20,0,16,8,2.5)},
    'tactical': {'block': 'tactical_loot_crate', 'body': '#3e464d', 'metal': '#22292f', 'accent': '#bc9a6e', 'panel': '#696354', 'label': 'AMMO', 'top': 6, 'z': (3, 13), 'size': (-8,24,1,15,6,2)},
    'medical': {'block': 'medical_loot_crate', 'body': '#d4dbd7', 'metal': '#596d74', 'accent': '#398b79', 'panel': '#4a7d72', 'label': 'MED1', 'top': 11, 'z': (1, 15), 'size': (-1,17,1,15,9,3)},
    'weathered': {'block': 'weathered_loot_crate', 'body': '#666b45', 'metal': '#49443c', 'accent': '#b6a37a', 'panel': '#53583b', 'label': 'BZ07', 'top': 8, 'z': (1, 15), 'size': (-3,19,0,16,7,2.5)},
}
GLYPHS = {
    'D': ['110','101','101','101','110'],
    'A': ['010','101','111','101','101'], 'B': ['110','101','110','101','110'],
    'Z': ['111','001','010','100','111'], 'M': ['101','111','111','101','101'],
    'O': ['010','101','101','101','010'], 'E': ['111','100','110','100','111'],
    'Q': ['010','101','101','111','011'], '0': ['111','101','101','101','111'],
    '1': ['010','110','010','010','111'], '2': ['110','001','010','100','111'],
    '4': ['101','101','111','001','001'], '7': ['111','001','010','010','010'],
}

def color(value):
    return tuple(int(value[i:i+2], 16) for i in (1, 3, 5))

def tint(rgb, n):
    return tuple(max(0, min(255, c+n)) for c in rgb)

def png(pixels, width=128, height=128):
    def chunk(name, payload):
        return struct.pack('>I', len(payload)) + name + payload + struct.pack('>I', zlib.crc32(name+payload) & 0xffffffff)
    raw = b''.join(b'\0' + bytes(sum((list(p) for p in row), [])) for row in pixels)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')

def atlas(style, theme):
    rng = random.Random(style)
    bases = {'body': color(theme['body']), 'metal': color(theme['metal']), 'inside': (27, 31, 30),
             'accent': color(theme['accent']), 'wood': (104, 86, 60), 'label': color(theme['panel']),
             'rubber': (24, 29, 30), 'panel': color(theme['panel']), 'rust': (120, 73, 43),
             'bolt': (111, 118, 113), 'hazard': color(theme['accent']), 'strap': (69, 74, 56),
             'steel': (100, 111, 111), 'lid': tint(color(theme['body']), 8), 'repair': (83, 81, 66), 'mark': color(theme['panel']), 'white': (221,235,226)}
    pixels = [[(30, 32, 30) for _ in range(128)] for _ in range(128)]
    for material, index in TILES.items():
        ox, oy = index % 8 * 16, index // 8 * 16
        base = bases[material]
        for y in range(16):
            for x in range(16):
                shade = rng.choice([-3, -1, 0, 0, 0, 1, 2]) if style != 'medical' else rng.choice([-1,0,0,0,1])
                if x == 0 or y == 0: shade += 8
                if x == 15 or y == 15: shade -= 12
                if material == 'wood': shade += (x % 5 - 2) * 4
                if material == 'inside': shade -= 8 if (x+y) % 4 == 0 else 0
                if material == 'hazard' and (x+y) % 10 < 4: shade = -110
                if material == 'strap' and x % 4 == 0: shade += 9
                if material == 'rust' and rng.random() < .25: shade += rng.choice([-20, 18])
                pixels[oy+y][ox+x] = tint(base, shade)
        # Sparse chips on exposed panels, with stronger damage on the old crate.
        if style != 'medical' and material in ('body', 'lid', 'metal', 'wood', 'repair'):
            for _ in range(14 if style == 'weathered' else 4):
                x, y = rng.randrange(1, 15), rng.randrange(1, 15)
                pixels[oy+y][ox+x] = (131, 96, 61) if style == 'weathered' else tint(base, 21)
        if material == 'bolt':
            for y in range(5, 11):
                for x in range(5, 11): pixels[oy+y][ox+x] = (154, 159, 148) if (x+y)%2 else (90, 101, 97)
            for x in range(6, 10): pixels[oy+8][ox+x] = (40, 45, 42)
        if material == 'label':
            for i, letter in enumerate(theme['label']):
                for y, line in enumerate(GLYPHS[letter]):
                    for x, bit in enumerate(line):
                        if bit == '1': pixels[oy+5+y][ox+i*4+x] = (211, 209, 180)
        if material == 'mark':
            for y in range(3, 13):
                for x in range(3, 13):
                    if (abs(x-8) <= 1 or abs(y-8) <= 1) if style == 'medical' else abs(x-8)+abs(y-8) < 5:
                        pixels[oy+y][ox+x] = (221,235,226) if style == 'medical' else color(theme['accent'])
    return png(pixels)

def identifier(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'blockzone:loot_crates/' + name))

def cube(name, start, end, material='body', group='body', face_materials=None):
    faces = {}
    for face in FACES:
        tile = TILES[(face_materials or {}).get(face, material)]
        u, v = tile % 8 * 16, tile // 8 * 16
        faces[face] = {'uv': [u, v, u+16, v+16], 'texture': 0}
    return {'name': name, 'from': start, 'to': end, 'faces': faces, '_group': group}

def geometry(style, theme):
    parts = []
    top = theme['top']; z1, z2 = theme['z']
    def add(name, start, end, material='body', group='body', faces=None):
        parts.append(cube(name, start, end, material, group, faces))
    def decal(name, start, end, face, material, group='body'):
        part = cube(name, start, end, material, group)
        part['faces'] = {direction: (data if direction == face else {**data, 'texture': None})
                         for direction, data in part['faces'].items()}
        parts.append(part)
    # Open cavity: independent walls and recessed interior, never a solid box under the lid.
    add('base_skid', [1, 0, z1], [15, 1.5, z2], 'rubber' if style == 'tactical' else 'metal')
    add('interior_floor', [2, 1.5, z1+1], [14, 2, z2-1], 'inside')
    wall = 'wood' if style == 'weathered' else 'body'
    add('front_frame_left', [1,1.5,z1], [3,top,z1+1], wall)
    add('front_frame_right', [13,1.5,z1], [15,top,z1+1], wall)
    add('front_frame_lower', [3,1.5,z1], [13,2.2,z1+1], wall)
    add('front_frame_upper', [3,top-.8,z1], [13,top,z1+1], wall)
    add('recessed_front_panel', [3,2.2,z1+.28], [13,top-.8,z1+1], wall, faces={'south':'inside'})
    add('back_wall', [1, 1.5, z2-1], [15, top, z2], wall, faces={'north': 'inside'})
    for side, outer, inner, inward in [('left',1,2,.25), ('right',14,15,-.25)]:
        add(side+'_wall_front', [outer,1.5,z1+1], [inner,top,z1+3], wall)
        add(side+'_wall_back', [outer,1.5,z2-3], [inner,top,z2-1], wall)
        add(side+'_wall_lower', [outer,1.5,z1+3], [inner,2.2,z2-3], wall)
        add(side+'_wall_upper', [outer,top-.8,z1+3], [inner,top,z2-3], wall)
        add(side+'_recessed_panel', [outer+(inward if side=='left' else 0),2.2,z1+3],
            [inner+(inward if side=='right' else 0),top-.8,z2-3], wall)
    add('lid_shell', [1,top,z1], [15,top+2.2,z2], 'lid', 'lid', {'down':'inside'})
    add('lid_frame_front', [1,top+2.2,z1], [15,top+3,z1+1], 'metal', 'lid')
    add('lid_frame_back', [1,top+2.2,z2-1], [15,top+3,z2], 'metal', 'lid')
    add('lid_frame_left', [1,top+2.2,z1+1], [2,top+3,z2-1], 'metal', 'lid')
    add('lid_frame_right', [14,top+2.2,z1+1], [15,top+3,z2-1], 'metal', 'lid')
    add('recessed_lid_panel', [2,top+2.2,z1+1], [14,top+2.55,z2-1], 'lid', 'lid')
    decal('front_id_decal', [6,top-3,z1+.25], [10,top-.8,z1+.25], 'north', 'label')
    for x in (3, 11):
        add('latch_' + str(x), [x, top-2.5, z1-.15], [x+2, top+1, z1+.6], 'metal')
        add('latch_pin_' + str(x), [x+.6, top-1.8, z1-.19], [x+1.4, top-.2, z1-.15], 'bolt')
    # Hinge barrels and side handles distinguish a supply case from a vanilla chest.
    for x in (3, 10.5): add('hinge_' + str(x), [x, top-.5, z2-.8], [x+2.5, top+.8, z2+.15], 'metal')
    for side, x in [('left', 1), ('right', 14.5)]:
        mid = (z1+z2)/2
        add(side+'_handle_front', [x, 3.2, mid-2], [x+.5, 4.4, mid-1.4], 'metal')
        add(side+'_handle_back', [x, 3.2, mid+1.4], [x+.5, 4.4, mid+2], 'metal')
        add(side+'_handle_grip', [x, 4.4, mid-2], [x+.5, 5, mid+2], 'rubber')
    if style == 'military':
        for x in (1, 13):
            for z in (z1, z2-2):
                add('armored_corner_'+str(x)+'_'+str(z), [x, 1.5, z], [x+2, top, z+2], 'metal')
                add('lid_corner_'+str(x)+'_'+str(z), [x, top, z], [x+2, top+3, z+2], 'metal', 'lid')
        for x in (4, 10):
            add('lid_webbing_'+str(x), [x, top+3, z1+.5], [x+2, top+3.2, z2-.5], 'strap', 'lid')
            add('front_webbing_'+str(x), [x, 2, z1-.03], [x+2, top-3, z1], 'strap')
        decal('lid_supply_decal', [4,top+3.23,6], [6,top+3.23,10], 'up', 'mark', 'lid')
    elif style == 'tactical':
        for x in (1, 13):
            for z in (z1, z2-2):
                add('rubber_corner_'+str(x)+'_'+str(z), [x, 1, z], [x+2, top, z+2], 'rubber')
                add('lid_rubber_corner_'+str(x)+'_'+str(z), [x, top, z], [x+2, top+3, z+2], 'rubber', 'lid')
        for x in (3, 6.5, 10): add('hard_shell_rib_'+str(x), [x, top+2.4, z1+.8], [x+1.5, top+3.8, z2-.8], 'panel', 'lid')
        add('lid_tan_band', [1.5, top+.8, z1-.025], [14.5, top+1.5, z1], 'accent', 'lid')
        add('foam_divider', [7.5, 2, z1+1], [8.5, 3.2, z2-1], 'inside')
        add('foam_insert_left', [2.5, 2, z1+1.5], [6.5, 2.5, z2-1.5], 'inside')
        add('foam_insert_right', [9, 2, z1+1.5], [13.5, 2.5, z2-1.5], 'inside')
    elif style == 'medical':
        for x in (1, 13):
            for z in (z1, z2-2):
                add('steel_corner_'+str(x)+'_'+str(z), [x, 1, z], [x+2, top, z+2], 'steel')
                add('lid_steel_corner_'+str(x)+'_'+str(z), [x, top, z], [x+2, top+3, z+2], 'steel', 'lid')
        for z in (3.5, 9.5): add('lid_cross_brace_'+str(z), [2, top+3, z], [14, top+3.25, z+2], 'steel', 'lid')
        add('front_medical_band', [3, 2.5, z1-.035], [13, 4, z1], 'accent')
        add('lid_medical_band', [3, top+.8, z1-.035], [13, top+2, z1], 'accent', 'lid')
        decal('front_medical_decal', [6,4,z1+.25], [10,7,z1+.25], 'north', 'mark')
        decal('lid_medical_decal', [5.5,top+2.58,5.7], [10.5,top+2.58,9.3], 'up', 'mark', 'lid')
    else:
        for x in (1, 6, 11):
            add('lid_wood_plank_'+str(x), [x, top+2, z1], [min(15,x+3.8), top+3.1, z2], 'wood', 'lid')
        parts = [p for p in parts if p['name'] != 'recessed_front_panel']
        add('wood_panel_backer', [3,2.2,z1+.42], [13,top-.8,z1+1], 'inside')
        for i,(y1,y2) in enumerate([(2.2,3),(3.17,5.4),(5.57,top-.8)]):
            add('front_wood_plank_'+str(i), [3,y1,z1+.28], [13,y2,z1+.45], 'wood')
        for x in (1, 13):
            add('front_rust_strap_'+str(x), [x, 1.3, z1-.08], [x+2, top, z1], 'rust')
            add('lid_rust_strap_'+str(x), [x, top+3.1, z1], [x+2, top+3.3, z2], 'rust', 'lid')
        add('field_repair_plate', [8, 2.6, z1+.16], [11, 4.8, z1+.29], 'repair')
        for x in (8.3, 10.2):
            for y in (2.8, 4.0): add('repair_bolt_'+str(x)+'_'+str(y), [x,y,z1+.1], [x+.6,y+.6,z1+.18], 'bolt')
    if style == 'military':
        for x in (3,7.7,12.4): add('raised_lid_rib_'+str(x), [x,top+2.4,z1+1.5], [x+.6,top+3.6,z2-1.5], 'body','lid')
    if style != 'weathered':
        for x in (3.4,12.0): add('front_panel_rib_'+str(x), [x,2.5,z1+.12], [x+.6,top-1.2,z1+.35], 'metal')
    for x in (3.2,12.1):
        for y in (2.65,top-.5): add('front_rivet_'+str(x)+'_'+str(y), [x,y,z1-.065], [x+.7,y+.4,z1+.06], 'bolt')
    # Raised guards/boards must not share an exposed plane with their base shell.
    for part in parts:
        name = part['name']
        if 'corner' in name:
            part['from'][0] -= .06; part['to'][0] += .06
            part['from'][2] -= .06; part['to'][2] += .06
            if part['_group'] == 'lid':
                part['from'][1] = top+.08; part['to'][1] += .06
            else: part['to'][1] = top-.2
        if 'lid_wood_plank' in name:
            part['from'][0] -= .06; part['to'][0] += .06
            part['from'][2] -= .06; part['to'][2] += .06
        if name.startswith('left_handle'): part['from'][0], part['to'][0] = .5,1.5
        if name.startswith('right_handle'): part['from'][0], part['to'][0] = 14.5,15.5
        if name == 'front_id_plate': part['from'][2], part['to'][2] = z1+.12,z1+.30
    # Scale structural dimensions, keeping each model's own proportions and details.
    x1, x2, target_z1, target_z2, height, lid = theme['size']
    def position(point):
        x, y, z = point
        return [round(x1+(x-1)*(x2-x1)/14, 5),
                round(y*height/top if y <= top else height+(y-top)*lid/3, 5),
                round(target_z1+(z-z1)*(target_z2-target_z1)/(z2-z1), 5)]
    for part in parts:
        part['from'] = position(part['from']); part['to'] = position(part['to'])
    return parts

def opened(parts, theme):
    result = copy.deepcopy(parts)
    pivot_y, pivot_z = theme['size'][4]+theme['size'][5]/2, theme['size'][3]-theme['size'][5]/2
    remap = {'north':'up', 'up':'south', 'south':'down', 'down':'north', 'east':'east', 'west':'west'}
    for part in result:
        if part['_group'] != 'lid': continue
        a, b = part['from'], part['to']
        part['from'] = [a[0], pivot_y+pivot_z-b[2], pivot_z+a[1]-pivot_y]
        part['to'] = [b[0], pivot_y+pivot_z-a[2], pivot_z+b[1]-pivot_y]
        old = part['faces']; part['faces'] = {remap[face]: data for face,data in old.items()}
        for face in ('east','west'): part['faces'][face]['rotation'] = 90 if face == 'east' else 270
    return result

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')

def emit(style, theme):
    texture = atlas(style, theme)
    target = ASSETS / 'textures/block/loot_crates' / (style+'.png')
    target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(texture)
    closed = geometry(style, theme)
    for is_open, parts in [(False, closed), (True, opened(closed, theme))]:
        name = style + ('_open' if is_open else '')
        elements = []
        groups = {'body': [], 'lid': []}
        for part in parts:
            entry = {key: copy.deepcopy(value) for key,value in part.items() if key != '_group'}
            entry.update({'uuid': identifier(name+'/'+part['name']), 'type': 'cube', 'box_uv': False,
                          'autouv': 0, 'shade': True, 'origin': [8, theme['size'][4]+theme['size'][5]/2, theme['size'][3]-theme['size'][5]/2]})
            elements.append(entry); groups[part['_group']].append(entry['uuid'])
        project = {'meta': {'format_version':'5.0','model_format':'java_block','box_uv':False},
                   'name': name, 'java_block_version': '1.9.0', 'parent':'minecraft:block/block',
                   'credit':'Blockzone scene loot crates', 'resolution': {'width':128,'height':128},
                   'ambientocclusion':True, 'elements':elements,
                   'groups':[{'name':group, 'uuid':identifier(name+'/group/'+group), 'origin':[8,theme['size'][4]+theme['size'][5]/2,theme['size'][3]-theme['size'][5]/2]} for group in groups],
                   'outliner':[{'uuid':identifier(name+'/group/'+group),'isOpen':True,'children':children} for group,children in groups.items()],
                   'textures':[{'name':style+'.png','id':'crate','uuid':identifier(style+'/texture'),
                                'folder':'block/loot_crates','namespace':'blockzone','uv_width':128,'uv_height':128,
                                'particle':True,'source':'data:image/png;base64,'+base64.b64encode(texture).decode()}],
                   'display':{'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.8,.8,.8]},
                              'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.5,.5,.5]}}}
        write_json(EXAMPLES/'blockbench'/ (name+'.bbmodel'), project)
        game_elements = []
        for part in parts:
            faces = copy.deepcopy(part['faces'])
            faces = {direction: data for direction,data in faces.items() if data.get('texture') is not None}
            for face in faces.values():
                face['uv'] = [v/8 for v in face['uv']]; face['texture'] = '#crate'
            game_elements.append({'name':part['name'],'from':part['from'],'to':part['to'],'faces':faces})
        write_json(ASSETS/'models/block/loot_crates'/ (name+'.json'),
                   {'credit':'Made for Blockzone; editable Blockbench project included', 'parent':'minecraft:block/block',
                    'textures':{'particle':'blockzone:block/loot_crates/'+style,'crate':'blockzone:block/loot_crates/'+style},
                    'elements':game_elements,'display':project['display']})
    block = theme['block']
    variants = {}
    for direction, angle in [('north',0),('east',90),('south',180),('west',270)]:
        for is_open in (False,True): variants[f'facing={direction},open={str(is_open).lower()}'] = {'model': ('blockzone:block/loot_crate' if style == 'military' else 'blockzone:block/loot_crates/'+style)+('_open' if is_open else ''),'y':angle}
    write_json(ASSETS/'blockstates'/ (block+'.json'), {'variants':variants})
    write_json(ASSETS/'models/item'/ (block+'.json'), {'parent':'blockzone:block/loot_crates/'+style})
    for suffix in ('','_open'):
        if style == 'military': write_json(ASSETS/'models/block'/ ('loot_crate'+suffix+'.json'), {'parent':'blockzone:block/loot_crates/'+style+suffix})
    print(f'{style}: {len(closed)} cubes, closed/open projects, 128x128 atlas')

if __name__ == '__main__':
    for style, theme in THEMES.items(): emit(style, theme)
