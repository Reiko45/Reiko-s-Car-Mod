"""Draws the Car Window and Car Light item icons and writes their models and recipes.

Run from the repository root: python3 tools/damage/assets.py
"""
import json
import os
from PIL import Image

ASSETS = "src/main/resources/assets/reikocars"
DATA = "src/main/resources/data/reikocars"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def window():
    """A windshield: a curved pane of blue-grey glass in a black rubber seal, with a highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    seal = (28, 30, 34, 255)
    for y in range(3, 13):
        inset = max(0, 3 - (y - 3)) // 2 if y < 6 else 0
        x0, x1 = 1 + inset, 14 - inset
        for x in range(x0, x1 + 1):
            edge = y in (3, 12) or x in (x0, x1)
            if edge:
                img.putpixel((x, y), seal)
            else:
                t = (y - 4) / 8
                img.putpixel((x, y), (int(120 + 40 * t), int(160 + 30 * t), int(190 + 25 * t), 235))
    # a highlight streak
    for i in range(5):
        x, y = 4 + i, 9 - i
        if img.getpixel((x, y))[3] == 235:
            img.putpixel((x, y), (225, 238, 248, 255))
        if img.getpixel((x + 1, y))[3] == 235:
            img.putpixel((x + 1, y), (200, 222, 240, 255))
    return img


def light():
    """A headlight: a chrome rim round a clear lens over a bright reflector."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 7.5, 6.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > r:
                continue
            if d > r - 1.3:
                shade = 170 + int(60 * (1 - (x + y) / 30))
                img.putpixel((x, y), (shade, shade, shade + 6, 255))
            elif d > r - 2.2:
                img.putpixel((x, y), (90, 92, 98, 255))
            else:
                glow = 1 - d / (r - 2.2)
                img.putpixel((x, y), (int(215 + 40 * glow), int(215 + 38 * glow), int(190 + 50 * glow), 255))
    for p in ((5, 4), (4, 5), (5, 5), (6, 4)):
        img.putpixel(p, (255, 255, 255, 255))
    return img


def main():
    os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
    window().save(f"{ASSETS}/textures/item/car_window.png")
    light().save(f"{ASSETS}/textures/item/car_light.png")
    for name in ("car_window", "car_light"):
        write(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"reikocars:item/{name}"}})
    write(f"{DATA}/recipe/car_window.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:glass_panes"}, {"tag": "c:glass_panes"}, {"tag": "c:glass_panes"}, {"tag": "c:nuggets/iron"}],
        "result": {"id": "reikocars:car_window", "count": 1},
    })
    write(f"{DATA}/recipe/car_light.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:glass_panes"}, {"tag": "c:dusts/glowstone"}, {"tag": "c:nuggets/iron"}],
        "result": {"id": "reikocars:car_light", "count": 1},
    })


if __name__ == "__main__":
    main()
