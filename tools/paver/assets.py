"""The road paver's assets (1.38.0): the diagonal road markings (textures, block models, the asphalt and asphalt slab
block states), the paver's item models and part recipes, and the blocks it fills with (the paver_fill tags). Its model,
texture and icons come from the model scripts, as every vehicle's do.

Usage: python3 tools/paver/assets.py
"""
import json
import os
import random

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'src/main/resources/assets/reikocars')
DATA = os.path.join(REPO, 'src/main/resources/data/reikocars')
WHITE = (232, 232, 224)
YELLOW = (242, 190, 44)
# The markings, as the block states know them: the straight ones vroads.py made, then the diagonals made here.
MARKINGS = ['none', 'center_dashed', 'center_solid', 'lane_dashed', 'edge', 'stop_line', 'crosswalk',
            'arrow_straight', 'arrow_left', 'arrow_right',
            'center_dashed_diagonal', 'center_solid_diagonal', 'lane_dashed_diagonal', 'edge_diagonal']
DIAGONALS = MARKINGS[10:]
FACINGS = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def diagonal_mask(marking):
    """The painted texels of a diagonal marking, facing north: a line from the south-west corner (x 0, y 15) to the
    north-east one (x 15, y 0), so a row of these blocks along a 45-degree road draws one straight line. Dashed lines
    are painted in the block's north-east half, so they alternate dash and gap along the road."""
    m = {}
    for y in range(16):
        for x in range(16):
            d = x + y - 15  # 0 on the diagonal; negative toward the north-west corner
            if marking in ('center_dashed_diagonal', 'lane_dashed_diagonal'):
                if abs(d) <= 1 and x >= 8 and y <= 8:
                    m[(x, y)] = YELLOW if marking.startswith('center') else WHITE
            elif marking == 'center_solid_diagonal':
                if d in (-3, -2) or d in (2, 3):
                    m[(x, y)] = YELLOW
            elif marking == 'edge_diagonal':
                if abs(d) <= 1:
                    m[(x, y)] = WHITE
    return m


def marked(base, mask, seed):
    img = base.copy()
    px = img.load()
    rng = random.Random(seed)
    for (x, y), c in mask.items():
        k = 1.0 if rng.random() > 0.12 else 0.82  # a little worn, like the straight ones
        px[x, y] = tuple(int(v * k) for v in c) + (255,)
    return img


def markings():
    tex = os.path.join(ASSETS, 'textures/block')
    base = Image.open(os.path.join(tex, 'asphalt.png')).convert('RGBA')
    for i, marking in enumerate(DIAGONALS):
        name = f'asphalt_{marking}'
        marked(base, diagonal_mask(marking), 31 + i).save(os.path.join(tex, name + '.png'))
        top = f'reikocars:block/{name}'
        side = 'reikocars:block/asphalt'
        write(os.path.join(ASSETS, f'models/block/{name}.json'),
              {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': top, 'side': side, 'bottom': side}})
        sname = f'asphalt_slab_{marking}'
        write(os.path.join(ASSETS, f'models/block/{sname}.json'),
              {'parent': 'minecraft:block/slab', 'textures': {'top': top, 'side': side, 'bottom': side}})
        write(os.path.join(ASSETS, f'models/block/{sname}_top.json'),
              {'parent': 'minecraft:block/slab_top', 'textures': {'top': top, 'side': side, 'bottom': side}})
    # The block states, every marking: plain asphalt turns at random so the stone doesn't tile; markings turn with
    # their facing (as vroads.py wrote them, with the diagonals added).
    variants = {}
    slab = {}
    for facing, y in FACINGS.items():
        for marking in MARKINGS:
            name = 'asphalt' if marking == 'none' else f'asphalt_{marking}'
            sname = 'asphalt_slab' if marking == 'none' else f'asphalt_slab_{marking}'
            if marking == 'none':
                variants[f'facing={facing},marking=none'] = [{'model': f'reikocars:block/{name}', 'y': r} for r in (0, 90, 180, 270)]
            else:
                variants[f'facing={facing},marking={marking}'] = {'model': f'reikocars:block/{name}', **({'y': y} if y else {})}
            for kind, suffix in (('bottom', ''), ('top', '_top'), ('double', None)):
                model = f'reikocars:block/{name}' if suffix is None else f'reikocars:block/{sname}{suffix}'
                slab[f'facing={facing},marking={marking},type={kind}'] = {'model': model, **({'y': y} if y and marking != 'none' else {})}
    write(os.path.join(ASSETS, 'blockstates/asphalt.json'), {'variants': variants})
    write(os.path.join(ASSETS, 'blockstates/asphalt_slab.json'), {'variants': slab})


# Blocks the paver digs into its hopper and fills with: earth and stone that stays put (no sand or gravel, which would
# fall out from under a bridge).
FILL = ['#minecraft:dirt', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:stone', 'minecraft:granite',
        'minecraft:diorite', 'minecraft:andesite', 'minecraft:tuff', 'minecraft:deepslate', 'minecraft:cobbled_deepslate',
        'minecraft:calcite', 'minecraft:dripstone_block', 'minecraft:clay', '#minecraft:terracotta', 'minecraft:sandstone',
        'minecraft:red_sandstone', 'minecraft:netherrack', 'minecraft:blackstone', 'minecraft:basalt', 'minecraft:smooth_basalt',
        'minecraft:soul_soil', 'minecraft:end_stone', 'minecraft:stone_bricks', 'minecraft:packed_mud']
# The items it fills with (dirt's tag has grass blocks and the like, which come out as dirt).
FILL_ITEMS = ['minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:rooted_dirt'] + [f for f in FILL if f != '#minecraft:dirt']


def paver():
    write(os.path.join(DATA, 'tags/block/paver_fill.json'), {'values': FILL})
    write(os.path.join(DATA, 'tags/item/paver_fill.json'), {'values': FILL_ITEMS})
    models = os.path.join(ASSETS, 'models/item')
    write(os.path.join(models, 'road_paver.json'), {'parent': 'minecraft:item/generated',
                                                    'textures': {'layer0': 'reikocars:item/road_paver_paint', 'layer1': 'reikocars:item/road_paver_trim'}})
    for part in ('chassis', 'body', 'interior'):
        write(os.path.join(models, f'road_paver_{part}.json'), {'parent': 'minecraft:item/generated',
                                                                'textures': {'layer0': f'reikocars:item/road_paver_{part}'}})
    recipes = os.path.join(DATA, 'recipe')
    iron = {'tag': 'c:ingots/iron'}
    write(os.path.join(recipes, 'road_paver_chassis.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['I I', 'BAB', 'I I'],
        'key': {'I': iron, 'B': {'tag': 'c:storage_blocks/iron'}, 'A': {'item': 'minecraft:anvil'}},
        'result': {'count': 1, 'id': 'reikocars:road_paver_chassis'}})
    write(os.path.join(recipes, 'road_paver_body.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['IHI', 'IDI', 'SSS'],
        'key': {'I': iron, 'H': {'item': 'minecraft:hopper'}, 'D': {'tag': 'c:dyes/yellow'}, 'S': {'item': 'minecraft:smooth_stone_slab'}},
        'result': {'count': 1, 'id': 'reikocars:road_paver_body'}})
    write(os.path.join(recipes, 'road_paver_interior.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' K ', 'LRL', 'IAI'],
        'key': {'K': {'item': 'minecraft:clock'}, 'L': {'tag': 'c:leathers'}, 'R': {'tag': 'c:dusts/redstone'}, 'I': iron,
                'A': {'item': 'reikocars:asphalt'}},
        'result': {'count': 1, 'id': 'reikocars:road_paver_interior'}})
    write(os.path.join(recipes, 'road_paver.json'), {
        'type': 'reikocars:car_assembly', 'chassis': {'item': 'reikocars:road_paver_chassis'}, 'body': {'item': 'reikocars:road_paver_body'},
        'interior': {'item': 'reikocars:road_paver_interior'}, 'engine': {'item': 'reikocars:diesel_engine'}, 'wheel': {'item': 'reikocars:tire'},
        'result': {'count': 1, 'id': 'reikocars:road_paver'}})


def main():
    markings()
    paver()


if __name__ == '__main__':
    main()
