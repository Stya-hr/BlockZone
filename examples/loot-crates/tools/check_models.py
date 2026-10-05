#!/usr/bin/env python3
"""Check export/source parity, atlas UVs and visible coplanar face overlaps.

Touching internal faces are ignored only when opaque geometry covers the entire
intersection. Exposed same-plane overlaps are reported as potential Z-fighting.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MODELS = ROOT/'src/main/resources/assets/blockzone/models/block/loot_crates'
AXIS = {'west':(0,-1), 'east':(0,1), 'down':(1,-1), 'up':(1,1), 'north':(2,-1), 'south':(2,1)}
EPS = 1e-5

def intersect(a, b):
    rect = (max(a[0],b[0]), max(a[1],b[1]), min(a[2],b[2]), min(a[3],b[3]))
    return rect if rect[2]-rect[0]>EPS and rect[3]-rect[1]>EPS else None

def subtract(a, b):
    hit = intersect(a,b)
    if hit is None: return [a]
    x1,y1,x2,y2 = hit
    return [r for r in [(a[0],a[1],x1,a[3]),(x2,a[1],a[2],a[3]),(x1,a[1],x2,y1),(x1,y2,x2,a[3])]
            if r[2]-r[0]>EPS and r[3]-r[1]>EPS]

def exposed(rect, axis, sign, plane, cubes):
    remaining = [rect]
    other = [i for i in range(3) if i != axis]
    for cube in cubes:
        lo, hi = cube['from'], cube['to']
        # Geometry strictly beyond this surface hides the face, including contacting internal seams.
        covers = lo[axis] <= plane+EPS and hi[axis] >= plane-EPS
        beyond = hi[axis] > plane+EPS if sign == 1 else lo[axis] < plane-EPS
        if covers and beyond:
            blocker = (lo[other[0]],lo[other[1]],hi[other[0]],hi[other[1]])
            remaining = [piece for r in remaining for piece in subtract(r,blocker)]
    return remaining

def check(path):
    model = json.loads(path.read_text()); cubes = model['elements']
    source = json.loads((ROOT/'examples/loot-crates/blockbench'/(path.stem+'.bbmodel')).read_text())
    expected = {e['name']:e for e in source['elements']}
    assert len(cubes) == len(expected), (path.stem,'cube count')
    faces = []
    for index,cube in enumerate(cubes):
        assert all(-16 <= a <= b <= 32 for a,b in zip(cube['from'],cube['to'])), (path.stem,cube['name'],'bounds')
        match = expected[cube['name']]
        assert all(abs(a-b) <= EPS for key in ['from','to'] for a,b in zip(cube[key],match[key])), (path.stem,cube['name'],'export mismatch')
        for face,data in cube['faces'].items():
            assert all(0 <= value <= 16 for value in data['uv']), (path.stem,cube['name'],'UV')
            axis,sign = AXIS[face]; plane = cube['to'][axis] if sign == 1 else cube['from'][axis]
            other = [i for i in range(3) if i != axis]
            rect = tuple(cube['from'][i] for i in other)+tuple(cube['to'][i] for i in other)
            faces.append((index,face,axis,sign,plane,rect))
    conflicts = []
    for i,a in enumerate(faces):
        for b in faces[i+1:]:
            if a[0] == b[0] or a[2] != b[2] or abs(a[4]-b[4]) > EPS: continue
            overlap = intersect(a[5],b[5])
            if overlap is None: continue
            av = exposed(overlap,a[2],a[3],a[4],cubes)
            bv = exposed(overlap,b[2],b[3],b[4],cubes)
            if any(intersect(x,y) for x in av for y in bv):
                conflicts.append((cubes[a[0]]['name'],a[1],cubes[b[0]]['name'],b[1]))
    if conflicts:
        raise AssertionError((path.stem,'exposed coplanar faces',conflicts))
    print(f'PASS {path.stem}: {len(cubes)} cubes; source matches; UV/bounds valid; no exposed coplanar overlaps')

if __name__ == '__main__':
    for path in sorted(MODELS.glob('*.json')): check(path)
