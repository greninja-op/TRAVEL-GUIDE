from pathlib import Path

from PIL import Image

site = Path(__file__).resolve().parents[1]
tiles = site / "assets" / "img" / "osm_tiles"
zoom = 16
xs = range(46650, 46653)
ys = range(30944, 30947)

canvas = Image.new("RGB", (256 * 3, 256 * 3))
for ix, x in enumerate(xs):
    for iy, y in enumerate(ys):
        tile = tiles / f"{zoom}-{x}-{y}.png"
        if tile.exists():
            canvas.paste(Image.open(tile).convert("RGB"), (ix * 256, iy * 256))

out = site / "assets" / "img" / "fort-kochi-map.png"
canvas.save(out, optimize=True)
print("saved", out, canvas.size, out.stat().st_size)
