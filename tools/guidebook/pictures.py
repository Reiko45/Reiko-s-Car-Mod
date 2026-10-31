"""Crops the showcase screenshots into the Car Manual's pictures: python3 tools/guidebook/pictures.py

The screenshots come from the showcases (./gradlew runClient -Pshowcase=guide, and the others) in run/screenshots/.

Each picture is a 256x256 texture with the picture in its top-left 200x200, as Patchouli's image pages draw it.
"""
from PIL import Image, ImageStat
import os, sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHOTS = f'{REPO}/run/screenshots'
OUT = f'{REPO}/src/main/resources/assets/reikocars/textures/gui/guide'

# picture: (screenshot, crop box x0, y0, x1, y1) on the 854x480 screenshot
CROPS = {
    'bus_route': ('338_bus_villagers_aboard', 188, 20, 568, 400),
    'bus_shelter': ('331_bus_shelter', 200, 40, 600, 440),
    'camp': ('344_camper_camp', 232, 50, 592, 410),
    'camp_night': ('354_camper_camp_night', 268, 30, 568, 330),
    'camper_screen': ('352_camper_screen', 254, 30, 600, 376),
    'car_wash': ('279_washing_2', 100, 20, 540, 460),
    'dashboard': ('158_hud_wide', 646, 266, 854, 474),
    'dmv': ('230_dmv_and_route', 260, 80, 620, 440),
    'finance': ('205_sale_finance', 236, 40, 616, 420),
    'gps_driving': ('315_gps_driving', 230, 0, 650, 420),
    'gps_places': ('310_gps_places', 140, 0, 706, 480),
    'hose': ('189_hose_spray', 180, 40, 600, 460),
    'impound': ('202_impound_lot', 206, 40, 646, 480),
    'ladder': ('186_ladder_hero', 180, 20, 620, 460),
    'livery': ('255_race_number_side', 278, 75, 578, 375),
    'livestock': ('345_livestock_trailer', 230, 10, 650, 430),
    'livestock_ramp': ('346_livestock_ramp', 260, 20, 660, 420),
    'racing': ('264_racing', 200, 0, 590, 390),
    'rain': ('295_tire_spray', 295, 100, 655, 460),
    'rentals': ('200_rental_lot', 330, 0, 810, 480),
    'snow_on_cars': ('296_snow_on_cars', 80, 15, 430, 365),
    'tune_tab': ('251_tune_tab', 242, 10, 612, 380),
    'wash_bay': ('282_bay_spraying', 240, 40, 620, 420),
    'welcome': ('g_welcome', 200, 100, 560, 460),
    'workbench': ('g_workbench', 185, 0, 665, 480),
    'pump': ('g_pump', 136, 0, 616, 480),
    'radio': ('g_radio', 130, 0, 610, 480),
    'roads': ('g_roads', 266, 65, 566, 365),
    'semi': ('g_semi', 310, 130, 630, 450),
    'pickup_camper': ('g_pickup_camper', 176, 60, 596, 480),
}


def picture(shot, box):
    crop = Image.open(f'{SHOTS}/{shot}.png').convert('RGB').crop(box)
    w, h = crop.size
    scale = 200.0 / max(w, h)
    small = crop.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    frame = Image.new('RGB', (200, 200), tuple(int(c) for c in ImageStat.Stat(crop).mean))
    frame.paste(small, ((200 - small.width) // 2, (200 - small.height) // 2))
    texture = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    texture.paste(frame, (0, 0))
    return texture


def main(wanted):
    os.makedirs(OUT, exist_ok=True)
    for name, (shot, *box) in CROPS.items():
        # 256 colors is plenty for a picture this size, and a sixth of the file
        picture(shot, tuple(box)).quantize(256, method=Image.Quantize.FASTOCTREE).save(f'{OUT}/{name}.png', optimize=True)
    names = sorted(CROPS)
    sheet = Image.new('RGB', (6 * 204, ((len(names) + 5) // 6) * 204), (40, 40, 40))
    for i, name in enumerate(names):
        sheet.paste(Image.open(f'{OUT}/{name}.png').convert('RGB').crop((0, 0, 200, 200)), ((i % 6) * 204 + 2, (i // 6) * 204 + 2))
    os.makedirs(f'{REPO}/build', exist_ok=True)
    sheet.save(f'{REPO}/build/guide-pictures.png')  # all of them at a glance
    missing = set(wanted) - set(CROPS)
    if missing:
        raise SystemExit('no picture for ' + ' '.join(sorted(missing)))
    print(len(CROPS), 'pictures')


main(sys.argv[1:])
