"""Draws the garage doors' textures and writes their block states, models, loot tables and recipes.

Run from the repository root: python3 tools/garage/assets.py
"""
import json
import os
from PIL import Image

ROOT = "src/main/resources"
ASSETS = f"{ROOT}/assets/reikocars"
DATA = f"{ROOT}/data/reikocars"


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def gray(v, a=255):
    return (v, v, v, a)


# ---- textures ----

def sectional_front(window=False):
    img = Image.new("RGBA", (16, 16), gray(226))
    for x in range(16):
        img.putpixel((x, 0), gray(244))   # the top lip
        img.putpixel((x, 15), gray(150))  # the joint below
        img.putpixel((x, 14), gray(205))
    if window:
        # a frame with two panes (holes: the glass is drawn behind)
        for x in range(2, 14):
            for y in range(3, 13):
                img.putpixel((x, y), gray(236))
        for x in range(3, 13):
            for y in range(4, 12):
                img.putpixel((x, y), (0, 0, 0, 0))
        for y in range(4, 12):
            img.putpixel((7, y), gray(236))
            img.putpixel((8, y), gray(228))
        for x in range(2, 14):
            img.putpixel((x, 3), gray(250))
            img.putpixel((x, 12), gray(190))
        return img
    # a raised panel
    for x in range(2, 14):
        for y in range(3, 12):
            img.putpixel((x, y), gray(238))
    for x in range(2, 14):
        img.putpixel((x, 3), gray(252))
        img.putpixel((x, 11), gray(182))
    for y in range(3, 12):
        img.putpixel((2, y), gray(248))
        img.putpixel((13, y), gray(190))
    for x in range(4, 12):
        for y in range(5, 10):
            img.putpixel((x, y), gray(232))
    return img


def sectional_back(window=False):
    img = Image.new("RGBA", (16, 16), gray(196))
    for x in range(16):
        img.putpixel((x, 0), gray(178))
        img.putpixel((x, 15), gray(150))
        img.putpixel((x, 8), gray(172))
        img.putpixel((x, 7), gray(208))
    for y in range(16):
        img.putpixel((0, y), gray(182))
        img.putpixel((15, y), gray(182))
    if window:
        for x in range(3, 13):
            for y in range(4, 12):
                img.putpixel((x, y), (0, 0, 0, 0))
        for y in range(4, 12):
            img.putpixel((7, y), gray(200))
            img.putpixel((8, y), gray(192))
    return img


def glass():
    img = Image.new("RGBA", (16, 16), (58, 70, 84, 255))
    for i in range(16):
        for d in (0, 1):
            x = i + d - 6
            y = 15 - i
            if 0 <= x < 16:
                img.putpixel((x, y), (112, 128, 146, 255))
        x = i + 4
        if 0 <= x < 16:
            img.putpixel((x, 15 - i), (92, 106, 122, 255))
    return img


def shutter():
    img = Image.new("RGBA", (16, 16), gray(200))
    for y in range(16):
        v = 228 if y % 2 == 0 else 168
        for x in range(16):
            img.putpixel((x, y), gray(v))
    return img


def button():
    img = Image.new("RGBA", (16, 16), (222, 220, 212, 255))
    for i in range(16):
        img.putpixel((i, 0), (200, 198, 190, 255))
        img.putpixel((i, 15), (176, 174, 166, 255))
        img.putpixel((0, i), (206, 204, 196, 255))
        img.putpixel((15, i), (186, 184, 176, 255))
    # the button: a dark rocker
    for x in range(5, 11):
        for y in range(5, 11):
            img.putpixel((x, y), (66, 70, 76, 255))
    for x in range(5, 11):
        img.putpixel((x, 5), (96, 100, 108, 255))
    # a little green light
    img.putpixel((7, 2), (96, 220, 110, 255))
    img.putpixel((8, 2), (96, 220, 110, 255))
    return img


def button_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    plate = (222, 220, 212, 255)
    for x in range(4, 12):
        for y in range(1, 15):
            img.putpixel((x, y), plate)
    for x in range(4, 12):
        img.putpixel((x, 1), (236, 234, 228, 255))
        img.putpixel((x, 14), (168, 166, 158, 255))
    for y in range(1, 15):
        img.putpixel((4, y), (200, 198, 190, 255))
        img.putpixel((11, y), (176, 174, 166, 255))
    for x in range(6, 10):
        for y in range(6, 11):
            img.putpixel((x, y), (66, 70, 76, 255))
    for x in range(6, 10):
        img.putpixel((x, 6), (100, 104, 112, 255))
    img.putpixel((7, 3), (96, 220, 110, 255))
    img.putpixel((8, 3), (96, 220, 110, 255))
    return img


def remote():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body = (52, 56, 62, 255)
    edge = (30, 32, 36, 255)
    for x in range(4, 12):
        for y in range(3, 15):
            img.putpixel((x, y), body)
    for x in range(4, 12):
        img.putpixel((x, 3), edge)
        img.putpixel((x, 14), edge)
    for y in range(3, 15):
        img.putpixel((4, y), edge)
        img.putpixel((11, y), edge)
    for corner in ((4, 3), (11, 3), (4, 14), (11, 14)):
        img.putpixel(corner, (0, 0, 0, 0))
    # the keyring loop
    for p in ((7, 0), (8, 0), (6, 1), (9, 1), (6, 2), (9, 2)):
        img.putpixel(p, (170, 172, 178, 255))
    # the red light, and two big buttons
    img.putpixel((7, 5), (230, 60, 50, 255))
    img.putpixel((8, 5), (230, 60, 50, 255))
    for top in (7, 10):
        for x in range(6, 10):
            for y in range(top, top + 2):
                img.putpixel((x, y), (190, 194, 200, 255))
        for x in range(6, 10):
            img.putpixel((x, top + 1), (150, 154, 160, 255))
    return img


# ---- models and states ----

def door_models(name, front, back):
    face = lambda tex, uv=None: dict({"texture": tex, "tintindex": 0}, **({"uv": uv} if uv else {}))
    write(f"{ASSETS}/models/block/{name}.json", {
        "parent": "block/block",
        "textures": {"front": f"reikocars:block/{front}", "back": f"reikocars:block/{back}", "particle": f"reikocars:block/{front}"},
        "elements": [{
            "from": [0, 0, 6.5], "to": [16, 16, 9.5],
            "faces": {
                "south": face("#front"),
                "north": face("#back"),
                "up": face("#back", [0, 0, 16, 3]),
                "down": face("#back", [0, 13, 16, 16]),
                "east": face("#back", [0, 0, 3, 16]),
                "west": face("#back", [13, 0, 16, 16]),
            },
        }],
    })
    # A panel of a finished door: drawn by the door, so no faces of its own.
    write(f"{ASSETS}/models/block/{name}_part.json", {"textures": {"particle": f"reikocars:block/{front}"}})
    rot = {"south": 0, "west": 90, "north": 180, "east": 270}
    variants = {}
    for facing, y in rot.items():
        for part in ("single", "anchor", "member"):
            model = f"reikocars:block/{name}" if part == "single" else f"reikocars:block/{name}_part"
            v = {"model": model}
            if y and part == "single":
                v["y"] = y
            variants[f"facing={facing},part={part}"] = v
    write(f"{ASSETS}/blockstates/{name}.json", {"variants": variants})
    # Flat in the hand and the inventory, as a door is: a panel's face, painted.
    write(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"reikocars:block/{front}"}})


def button_models():
    def elements(depth):
        # facing north: on the wall to its south, sticking out north
        return [
            {"from": [5, 3, 15], "to": [11, 13, 16], "faces": {
                "north": {"texture": "#plate", "uv": [3, 0, 13, 16]},
                "south": {"texture": "#plate", "uv": [3, 0, 13, 16], "cullface": "south"},
                "up": {"texture": "#plate", "uv": [3, 0, 13, 1]},
                "down": {"texture": "#plate", "uv": [3, 15, 13, 16]},
                "east": {"texture": "#plate", "uv": [0, 0, 1, 16]},
                "west": {"texture": "#plate", "uv": [15, 0, 16, 16]},
            }},
            {"from": [6.5, 6, 15 - depth], "to": [9.5, 10, 15], "faces": {
                "north": {"texture": "#plate", "uv": [5, 5, 11, 11]},
                "up": {"texture": "#plate", "uv": [5, 5, 11, 6]},
                "down": {"texture": "#plate", "uv": [5, 10, 11, 11]},
                "east": {"texture": "#plate", "uv": [5, 5, 6, 11]},
                "west": {"texture": "#plate", "uv": [10, 5, 11, 11]},
            }},
        ]
    tex = {"plate": "reikocars:block/garage_button", "particle": "reikocars:block/garage_button"}
    write(f"{ASSETS}/models/block/garage_button.json", {"parent": "block/block", "textures": tex, "elements": elements(1)})
    write(f"{ASSETS}/models/block/garage_button_pressed.json", {"parent": "block/block", "textures": tex, "elements": elements(0.25)})
    write(f"{ASSETS}/models/item/garage_button.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "reikocars:item/garage_button"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for facing, y in rot.items():
        for powered in ("false", "true"):
            v = {"model": "reikocars:block/garage_button" + ("_pressed" if powered == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={facing},powered={powered}"] = v
    write(f"{ASSETS}/blockstates/garage_button.json", {"variants": variants})


def loot(name):
    write(f"{DATA}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"reikocars:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"reikocars:blocks/{name}",
    })


def recipes():
    write(f"{DATA}/recipe/garage_door.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["IPI", "IPI"],
        "key": {"I": {"tag": "c:ingots/iron"}, "P": {"tag": "minecraft:planks"}},
        "result": {"id": "reikocars:garage_door", "count": 4},
    })
    write(f"{DATA}/recipe/roller_shutter.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["NIN", "NIN"],
        "key": {"I": {"tag": "c:ingots/iron"}, "N": {"tag": "c:nuggets/iron"}},
        "result": {"id": "reikocars:roller_shutter", "count": 4},
    })
    write(f"{DATA}/recipe/garage_remote.json", {
        "type": "minecraft:crafting_shapeless", "category": "redstone",
        "ingredients": [{"tag": "c:ingots/iron"}, {"tag": "c:dusts/redstone"}, {"tag": "minecraft:buttons"}],
        "result": {"id": "reikocars:garage_remote", "count": 1},
    })
    write(f"{DATA}/recipe/garage_button.json", {
        "type": "minecraft:crafting_shapeless", "category": "redstone",
        "ingredients": [{"item": "minecraft:stone_button"}, {"tag": "c:dusts/redstone"}, {"tag": "c:nuggets/iron"}],
        "result": {"id": "reikocars:garage_button", "count": 1},
    })


def main():
    save(sectional_front(), f"{ASSETS}/textures/block/garage_door.png")
    save(sectional_back(), f"{ASSETS}/textures/block/garage_door_back.png")
    save(shutter(), f"{ASSETS}/textures/block/roller_shutter.png")
    save(button(), f"{ASSETS}/textures/block/garage_button.png")
    save(sectional_front(), f"{ASSETS}/textures/entity/garage/sectional.png")
    save(sectional_back(), f"{ASSETS}/textures/entity/garage/sectional_back.png")
    save(sectional_front(True), f"{ASSETS}/textures/entity/garage/sectional_window.png")
    save(sectional_back(True), f"{ASSETS}/textures/entity/garage/sectional_window_back.png")
    save(glass(), f"{ASSETS}/textures/entity/garage/glass.png")
    save(shutter(), f"{ASSETS}/textures/entity/garage/shutter.png")
    save(remote(), f"{ASSETS}/textures/item/garage_remote.png")
    save(button_item(), f"{ASSETS}/textures/item/garage_button.png")
    door_models("garage_door", "garage_door", "garage_door_back")
    door_models("roller_shutter", "roller_shutter", "roller_shutter")
    button_models()
    write(f"{ASSETS}/models/item/garage_remote.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "reikocars:item/garage_remote"}})
    for name in ("garage_door", "roller_shutter", "garage_button"):
        loot(name)
    recipes()


if __name__ == "__main__":
    main()
