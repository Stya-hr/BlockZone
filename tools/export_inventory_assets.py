"""Extract the supplied Figma SVG export into standalone UI assets.

Usage: python tools/export_inventory_assets.py /path/to/BlockZone.zip
No screenshot or world background is included.
The hand-drawn rifle.svg and grenade.svg are preserved.
"""
import copy
import pathlib
import sys
import zipfile
import xml.etree.ElementTree as ET

NS = 'http://www.w3.org/2000/svg'
ET.register_namespace('', NS)
ET.register_namespace('xlink', 'http://www.w3.org/1999/xlink')
with zipfile.ZipFile(sys.argv[1]) as archive:
    source = ET.fromstring(archive.read('HUD/Screen/Viewport=QHD.svg'))
nodes = list(source[0])
defs = {node.get('id'): node for node in source[1]}
root = pathlib.Path(__file__).resolve().parents[1] / 'src/main/resources/assets/blockzone'
svg_dir = root / 'gui/inventory'
svg_dir.mkdir(parents=True, exist_ok=True)


def export(name, indices, bounds, enabled=False):
    x, y, width, height = bounds
    svg = ET.Element(f'{{{NS}}}svg', width=str(width), height=str(height),
                     viewBox=f'{x} {y} {width} {height}', fill='none')
    for index in indices:
        node = copy.deepcopy(nodes[index])
        if enabled:
            node.attrib.pop('opacity', None)
        if name == 'labels':
            label_index = indices.index(index)
            # Enlarge outlined text about its alignment anchor, preserving slot alignment.
            anchors = [(1280, 946), (770, 1160), (978, 1160), (1012, 1160),
                       (1220, 1160), (1309, 1159), (1451, 1159), (1593, 1159),
                       (1735, 1159), (987, 1248), (1134, 1248), (1280, 1248),
                       (1426, 1248), (1573, 1248)]
            ax, ay = anchors[label_index] if label_index < 14 else (1280, 1329)
            factor = 1.25 if label_index < 14 else 1.15
            group = ET.SubElement(svg, f'{{{NS}}}g',
                                  transform=f'translate({ax} {ay}) scale({factor}) translate({-ax} {-ay})')
            group.append(node)
        else:
            svg.append(node)
    # Include only referenced definitions, never the embedded world screenshot.
    definitions = ET.SubElement(svg, f'{{{NS}}}defs')
    pending = ET.tostring(svg).decode()
    included = set()
    while True:
        matches = [key for key in defs if key not in included and f'#{key}' in pending]
        if not matches:
            break
        for key in matches:
            included.add(key)
            node = copy.deepcopy(defs[key])
            definitions.append(node)
            pending += ET.tostring(node).decode()
    svg_path = svg_dir / f'{name}.svg'
    ET.ElementTree(svg).write(svg_path, encoding='utf-8', xml_declaration=True)



for name, index, bounds in [
    ('backpack', 7, (955, 990, 40, 40)),
    ('armor_plates', 10, (1077, 990, 40, 40)),
    ('helmet', 13, (1199, 990, 40, 40)),
    ('vest', 16, (1321, 990, 40, 40)),
    ('shield', 19, (1443, 990, 40, 40)),
    ('plus', 22, (1565, 990, 40, 40)),
    ('battery', 39, (1431, 1086, 40, 42)),
    ('medkit', 43, (1573, 1086, 40, 42)),
    ('plates', 47, (1715, 1086, 40, 42)),
    ('ammunition', 51, (967.2, 1187, 40, 40)),
]:
    export(name, [index], bounds, enabled=True)
# Preserve the exact outlined typography from the supplied design as a separate layer.
export('labels', [4, 26, 27, 31, 32, 36, 40, 44, 48, 52, 56, 60, 64, 68] + list(range(69, 81)),
       (760, 932, 1040, 410))
