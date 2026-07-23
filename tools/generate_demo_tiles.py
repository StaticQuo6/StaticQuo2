"""
Generate a small demo .mbtiles file from OpenStreetMap raster tiles.
Downloads a 3x3 grid of tiles around New York City at zoom levels 10-13.

Usage: python tools/generate_demo_tiles.py
Output: app/src/main/assets/maps/demo.mbtiles
"""

import sqlite3
import urllib.request
import os
import sys
import time

CENTER_LAT = 40.7128
CENTER_LON = -74.0060
ZOOMS = range(10, 14)
GRID_HALF = 1  # 1 tile in each direction = 3x3 grid per zoom
TILE_SERVER = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
USER_AGENT = "StaticQuo-Demo-Generator/1.0"
OUTPUT = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                       "app", "src", "main", "assets", "maps", "demo.mbtiles")


def lon_to_tile_x(lon, zoom):
    import math
    return int(math.floor((lon + 180.0) / 360.0 * (1 << zoom)))


def lat_to_tile_y(lat, zoom):
    import math
    lat_rad = math.radians(lat)
    return int(math.floor((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * (1 << zoom)))


def tms_y(xyz_y, zoom):
    return (1 << zoom) - 1 - xyz_y


def download_tile(z, x, y):
    url = TILE_SERVER.format(z=z, x=x, y=y)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.read()
    except Exception as e:
        print(f"  Failed tile {z}/{x}/{y}: {e}")
        return None


def main():
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)

    if os.path.exists(OUTPUT):
        os.remove(OUTPUT)

    db = sqlite3.connect(OUTPUT)
    db.execute("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
    db.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
    db.execute("CREATE UNIQUE INDEX tile_index ON tiles (zoom_level, tile_column, tile_row)")

    cx, cy = lon_to_tile_x(CENTER_LON, 0), lat_to_tile_y(CENTER_LAT, 0)

    total_tiles = 0
    for z in ZOOMS:
        cx_z = lon_to_tile_x(CENTER_LON, z)
        cy_z = lat_to_tile_y(CENTER_LAT, z)
        print(f"Zoom {z}: center tile {cx_z},{cy_z}")
        for dx in range(-GRID_HALF, GRID_HALF + 1):
            for dy in range(-GRID_HALF, GRID_HALF + 1):
                tx = cx_z + dx
                ty = cy_z + dy
                # Clamp to valid tile range
                max_tile = (1 << z) - 1
                tx = max(0, min(tx, max_tile))
                ty = max(0, min(ty, max_tile))
                print(f"  Downloading {z}/{tx}/{ty}...", end=" ")
                data = download_tile(z, tx, ty)
                if data:
                    row = tms_y(ty, z)
                    db.execute(
                        "INSERT INTO tiles VALUES (?, ?, ?, ?)",
                        (z, tx, row, data)
                    )
                    total_tiles += 1
                    print(f"OK ({len(data)} bytes)")
                else:
                    print("SKIP")
        time.sleep(0.5)  # Be polite to tile server

    # Write metadata
    bounds = f"{-CENTER_LON-0.5:.4f},{CENTER_LAT-0.5:.4f},{-CENTER_LON+0.5:.4f},{CENTER_LAT+0.5:.4f}"
    metadata = [
        ("name", "NYC Demo Region"),
        ("type", "baselayer"),
        ("version", "1.0.0"),
        ("description", "StaticQuo demo tiles for New York City area"),
        ("format", "png"),
        ("bounds", bounds),
        ("center", f"{CENTER_LON:.4f},{CENTER_LAT:.4f},{ZOOMS[0]}"),
    ]
    db.executemany("INSERT INTO metadata VALUES (?, ?)", metadata)
    db.commit()
    db.close()

    size = os.path.getsize(OUTPUT)
    print(f"\nDone! {total_tiles} tiles written to {OUTPUT}")
    print(f"File size: {size / 1024:.1f} KB")


if __name__ == "__main__":
    main()
