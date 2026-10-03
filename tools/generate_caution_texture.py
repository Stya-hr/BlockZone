"""Author the tile from vector rectangles; Python 3 standard library only."""
from pathlib import Path
import struct
import zlib

WIDTH, HEIGHT = 320, 60
GLYPHS = (
    (14,17,16,16,16,17,14), (14,17,17,31,17,17,17),
    (17,17,17,17,17,17,14), (31,4,4,4,4,4,4),
    (14,4,4,4,4,4,14), (14,17,17,17,17,17,14),
    (17,25,25,21,19,19,17),
)
pixels = bytearray(WIDTH * HEIGHT * 4)
def rectangle(x, y, width, height):
    for row in range(y, y + height):
        for col in range(x, x + width):
            offset = (row * WIDTH + col) * 4
            pixels[offset:offset+4] = bytes((255, 35, 45, 235))
rectangle(0, 0, WIDTH, 4)
rectangle(0, HEIGHT - 4, WIDTH, 4)
for letter, rows in enumerate(GLYPHS):
    for row, bits in enumerate(rows):
        for col in range(5):
            if bits & (1 << (4 - col)):
                rectangle(57 + letter * 30 + col * 5, 12 + row * 5, 5, 5)
def chunk(kind, payload):
    return struct.pack('>I', len(payload)) + kind + payload + struct.pack('>I', zlib.crc32(kind + payload))
raw = b''.join(b'\0' + pixels[y*WIDTH*4:(y+1)*WIDTH*4] for y in range(HEIGHT))
png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', WIDTH, HEIGHT, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')
path = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/blockzone/textures/effect/battlezone_warning_fence.png'
path.write_bytes(png)
print(path)
