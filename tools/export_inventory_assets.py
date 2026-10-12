"""Extract the supplied Figma SVG export into standalone UI assets.

Usage: python tools/export_inventory_assets.py /path/to/BlockZone.zip
Requires rsvg-convert. No screenshot or world background is included.
"""
import copy
import pathlib
import subprocess
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
png_dir = root / 'textures/gui/inventory'
svg_dir.mkdir(parents=True, exist_ok=True)
png_dir.mkdir(parents=True, exist_ok=True)


def export(name, indices, bounds, enabled=False):
    x, y, width, height = bounds
    svg = ET.Element(f'{{{NS}}}svg', width=str(width), height=str(height),
                     viewBox=f'{x} {y} {width} {height}', fill='none')
    for index in indices:
        node = copy.deepcopy(nodes[index])
        if enabled:
            node.attrib.pop('opacity', None)
        svg.append(node)
    # Include only referenced definitions, never the embedded world screenshot.
    definitions = ET.SubElement(svg, f'{{{NS}}}defs')
    if name == 'rifle':
        # The export uses lighten blending over the panel. Represent its black
        # background as transparency for Minecraft's standard alpha blending.
        svg[0].attrib.pop('style', None)
        svg[0].set('filter', 'url(#rifle-alpha)')
        effect = ET.SubElement(definitions, f'{{{NS}}}filter', id='rifle-alpha',
                               **{'color-interpolation-filters': 'sRGB'})
        ET.SubElement(effect, f'{{{NS}}}feColorMatrix', type='matrix',
                      values='0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  .242 .815 .082 0 -.06')
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
    subprocess.run(['rsvg-convert', '-w', str(round(width * 2)), '-h', str(round(height * 2)),
                    '-o', str(png_dir / f'{name}.png'), str(svg_path)], check=True)


for name, index, bounds in [
    ('backpack', 7, (955, 990, 40, 40)),
    ('armor_plates', 10, (1077, 990, 40, 40)),
    ('helmet', 13, (1199, 990, 40, 40)),
    ('vest', 16, (1321, 990, 40, 40)),
    ('shield', 19, (1443, 990, 40, 40)),
    ('plus', 22, (1565, 990, 40, 40)),
    ('rifle', 25, (797, 1077.77, 156, 56.4663)),
    ('grenade', 35, (1298.22, 1083, 21.5625, 46)),
    ('battery', 39, (1431, 1086, 40, 42)),
    ('medkit', 43, (1573, 1086, 40, 42)),
    ('plates', 47, (1715, 1086, 40, 42)),
    ('ammunition', 51, (967.2, 1187, 40, 40)),
]:
    export(name, [index], bounds, enabled=True)
# Preserve the exact outlined typography from the supplied design as a separate layer.
export('labels', [4, 26, 27, 31, 32, 36, 40, 44, 48, 52, 56, 60, 64, 68] + list(range(69, 81)),
       (760, 932, 1040, 410))
