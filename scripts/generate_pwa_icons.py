"""
Generate crisp, modern, high-res PWA PNG & SVG icons for HealthTrack AI using pure Python (zlib & struct).
"""

import math
import zlib
import struct
import os

def create_png(width, height, get_pixel_rgba, filename):
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # Filter type 0 (None)
        for x in range(width):
            r, g, b, a = get_pixel_rgba(x, y, width, height)
            raw_data.extend([int(r), int(g), int(b), int(a)])

    compressed = zlib.compress(bytes(raw_data), 9)

    png = bytearray(b'\x89PNG\r\n\x1a\n')

    # IHDR Chunk
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr_crc = zlib.crc32(b'IHDR' + ihdr_data)
    png.extend(struct.pack('>I', len(ihdr_data)) + b'IHDR' + ihdr_data + struct.pack('>I', ihdr_crc))

    # IDAT Chunk
    idat_crc = zlib.crc32(b'IDAT' + compressed)
    png.extend(struct.pack('>I', len(compressed)) + b'IDAT' + compressed + struct.pack('>I', idat_crc))

    # IEND Chunk
    iend_crc = zlib.crc32(b'IEND')
    png.extend(struct.pack('>I', 0) + b'IEND' + struct.pack('>I', iend_crc))

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(png)
    print(f"Generated {filename} ({width}x{height})")


def healthtrack_icon_pixel(x, y, width, height):
    # Normalized coordinates (-1.0 to 1.0)
    nx = (x / width) * 2.0 - 1.0
    ny = (y / height) * 2.0 - 1.0
    dist = math.sqrt(nx * nx + ny * ny)

    # Rounded squircle background
    r_sq = (nx**4 + ny**4)**0.25
    if r_sq > 0.95:
        # Anti-aliased boundary
        alpha = max(0, min(255, int((1.0 - r_sq) * 2550)))
        if alpha <= 0:
            return 0, 0, 0, 0
    else:
        alpha = 255

    # Background gradient: Dark Slate Blue to Emerald/Cyan accent
    grad_t = (nx + ny + 1.4) / 2.8
    bg_r = int(15 * (1 - grad_t) + 16 * grad_t)
    bg_g = int(23 * (1 - grad_t) + 185 * grad_t * 0.4)
    bg_b = int(42 * (1 - grad_t) + 129 * grad_t * 0.4)

    # Pulse / Heartbeat Glow + Lightning Bolt / Spark
    # Central lightning / pulse bolt shape
    # Points defining bolt
    # Upper wedge: from (-0.05, -0.65) to (0.35, -0.05) to (-0.05, 0.05)
    # Lower wedge: from (0.05, -0.05) to (-0.35, 0.65) to (0.05, 0.05)
    
    in_bolt = False
    # Check if point is inside stylized lightning bolt
    if -0.65 <= ny <= 0.65 and -0.45 <= nx <= 0.45:
        # Simple geometric lightning bolt test
        # Top segment: ny from -0.6 to 0.05, nx from -0.1 to 0.35
        # Bottom segment: ny from -0.05 to 0.6, nx from -0.35 to 0.1
        if ny < 0:
            # Top half
            t = (ny + 0.6) / 0.6  # 0 at top, 1 at middle
            left_bound = -0.12 + t * 0.15
            right_bound = 0.15 + t * 0.22
            if left_bound <= nx <= right_bound:
                in_bolt = True
        else:
            # Bottom half
            t = ny / 0.6  # 0 at middle, 1 at bottom
            left_bound = -0.35 + t * 0.23
            right_bound = 0.08 - t * 0.18
            if left_bound <= nx <= right_bound:
                in_bolt = True

    # Glow around bolt
    if in_bolt:
        # Bright emerald-to-cyan gradient for the bolt
        bolt_t = (ny + 0.6) / 1.2
        r = int(16 * (1 - bolt_t) + 52 * bolt_t)
        g = int(245 * (1 - bolt_t) + 211 * bolt_t)
        b = int(180 * (1 - bolt_t) + 245 * bolt_t)
        return r, g, b, alpha
    elif dist < 0.65:
        # Subtle emerald aura
        glow = max(0.0, 1.0 - dist / 0.65) * 0.35
        r = int(bg_r * (1 - glow) + 16 * glow)
        g = int(bg_g * (1 - glow) + 185 * glow)
        b = int(bg_b * (1 - glow) + 129 * glow)
        return r, g, b, alpha
    else:
        return bg_r, bg_g, bg_b, alpha


if __name__ == "__main__":
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target_dir = os.path.join(base_dir, "frontend", "static", "images")
    create_png(192, 192, healthtrack_icon_pixel, os.path.join(target_dir, "icon-192.png"))
    create_png(512, 512, healthtrack_icon_pixel, os.path.join(target_dir, "icon-512.png"))
    create_png(192, 192, healthtrack_icon_pixel, os.path.join(target_dir, "icon-maskable-192.png"))
    create_png(512, 512, healthtrack_icon_pixel, os.path.join(target_dir, "icon-maskable-512.png"))
    print("All PWA icons generated successfully!")
