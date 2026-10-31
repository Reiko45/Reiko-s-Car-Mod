"""Writes the mod's advancements (data/reikocars/advancement/*.json) and their names in en_us.json.

Every advancement has one criterion, the mod's own trigger reikocars:car, with the event it waits for (and, for the
ones that count, the least it has to reach). The events are fired by com.reiko.cars.advancement.CarAdvancements.

Run from the repository root: python3 tools/advancements/advancements.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
DATA = os.path.join(ROOT, 'src/main/resources/data/reikocars/advancement')
LANG = os.path.join(ROOT, 'src/main/resources/assets/reikocars/lang/en_us.json')

# (id, parent, icon, title, description, event, min, frame, experience)
ADVANCEMENTS = [
    ('root', None, 'reikocars:sedan', "Reiko's Cars", 'Get behind the wheel and drive', 'drive', 0, 'task', 0),

    # On the road
    ('first_road_trip', 'root', 'reikocars:convertible', 'First Road Trip', 'Drive 1,000 blocks without getting out', 'trip', 1000, 'task', 0),
    ('cross_country', 'first_road_trip', 'reikocars:off_roader', 'Cross Country', 'Drive 10,000 blocks without getting out', 'trip', 10000,
     'challenge', 100),

    # Fuel
    ('fill_up', 'root', 'reikocars:gas_pump', "Fill 'Er Up", 'Fill up a car at a gas or diesel pump', 'fuel', 0, 'task', 0),
    ('plugged_in', 'fill_up', 'reikocars:charging_station', 'Plugged In', 'Charge an electric car at a charging station', 'charge', 0, 'task', 0),
    ('running_on_empty', 'fill_up', 'minecraft:bucket', 'Running on Empty', 'Run out of fuel (or a flat battery) while driving', 'out_of_fuel', 0,
     'task', 0),

    # Owning cars
    ('off_the_lot', 'root', 'reikocars:dealership', 'Off the Lot', 'Buy a car from a dealership or from its owner', 'buy_car', 0, 'task', 0),
    ('fleet_owner', 'off_the_lot', 'reikocars:car_key', 'Fleet Owner', 'Own five cars at once', 'fleet', 5, 'goal', 0),
    ('open_sesame', 'off_the_lot', 'reikocars:garage_remote', 'Open Sesame', 'Drive up to a garage door that opens for you', 'garage', 0, 'task', 0),

    # The law
    ('licensed', 'root', 'reikocars:drivers_license', 'Licensed to Drive', 'Pass the driving test at a DMV', 'license', 0, 'task', 0),
    ('vanity_plate', 'licensed', 'reikocars:dmv_office', 'Vanity Plate', 'Buy a custom license plate at a DMV', 'plate', 0, 'task', 0),
    ('caught_on_camera', 'licensed', 'reikocars:speed_sign', 'Smile for the Camera', 'Get fined by a red-light or speed camera', 'camera', 0,
     'task', 0),

    # Looking after a car, from a wash to a race
    ('squeaky_clean', 'root', 'reikocars:car_wash', 'Squeaky Clean', 'Wash a dirty car clean, at a car wash, a wash bay or with a bucket of water',
     'wash', 0, 'task', 0),
    ('fender_bender', 'squeaky_clean', 'reikocars:body_panel', 'Fender Bender', 'Dent a car in a crash', 'crash', 0, 'task', 0),
    ('good_as_new', 'fender_bender', 'reikocars:repair_bench', 'Good as New', 'Have a car fixed up at a Repair Bench', 'repair', 0, 'task', 0),
    ('tuned_up', 'good_as_new', 'reikocars:turbocharger', 'Tuned Up', 'Fit a kit to a car at a Tuning Bench', 'tune', 0, 'task', 0),
    ('checkered_flag', 'tuned_up', 'reikocars:race_flag', 'Checkered Flag', 'Win a race', 'race_win', 0, 'goal', 0),

    # Trailers
    ('hitched', 'root', 'reikocars:box_trailer', 'Hitched', 'Hitch up a trailer', 'hitch', 0, 'task', 0),
    ('big_rig', 'hitched', 'reikocars:semi_truck', 'Big Rig', 'Hitch a semi-trailer to a semi truck', 'semi', 0, 'task', 0),
    ('happy_camper', 'hitched', 'reikocars:camper', 'Happy Camper', 'Sleep in a camper', 'camper', 0, 'task', 0),

    # Work
    ('on_the_job', 'root', 'reikocars:delivery_van', 'On the Job', 'Get paid for a driving job: a taxi fare, a bus stop, a delivery, a tow and more',
     'job', 0, 'task', 0),
    ('keep_the_change', 'on_the_job', 'reikocars:taxi', 'Keep the Change', 'Get paid a taxi fare', 'job_taxi_ride', 0, 'task', 0),
    ('next_stop', 'on_the_job', 'reikocars:bus_stop_sign', 'Next Stop', "Stop at the next bus stop on your bus's route", 'job_bus_stop', 0, 'task', 0),
    ('special_delivery', 'on_the_job', 'reikocars:parcel', 'Special Delivery', 'Deliver a parcel from a mail van', 'delivery', 0, 'task', 0),
    ('on_the_hook', 'on_the_job', 'reikocars:wrecker', 'On the Hook', "Tow someone's broken-down car to a Repair Bench", 'job_tow', 0, 'task', 0),
    ('road_crew', 'on_the_job', 'reikocars:road_paver', 'Road Crew', 'Lay 100 blocks of road with a Road Paver in one go', 'pave', 100, 'task', 0),
]


def advancement(entry):
    aid, parent, icon, title, description, event, least, frame, experience = entry
    conditions = {'event': event}
    if least:
        conditions['min'] = least
    display = {
        'icon': {'id': icon},
        'title': {'translate': f'advancements.reikocars.{aid}.title'},
        'description': {'translate': f'advancements.reikocars.{aid}.description'},
        'frame': frame,
        'show_toast': parent is not None,
        'announce_to_chat': parent is not None,
        'hidden': False,
    }
    if parent is None:
        display['background'] = 'reikocars:textures/block/asphalt.png'
    data = {}
    if parent is not None:
        data['parent'] = f'reikocars:{parent}'
    data['display'] = display
    data['criteria'] = {event: {'trigger': 'reikocars:car', 'conditions': conditions}}
    data['requirements'] = [[event]]
    if experience:
        data['rewards'] = {'experience': experience}
    return data


def main():
    os.makedirs(DATA, exist_ok=True)
    ids = {entry[0] for entry in ADVANCEMENTS}
    for name in os.listdir(DATA):
        if name.endswith('.json') and name[:-5] not in ids:
            os.remove(os.path.join(DATA, name))  # one that's gone from the list
    for entry in ADVANCEMENTS:
        assert entry[1] is None or entry[1] in ids, entry
        with open(os.path.join(DATA, entry[0] + '.json'), 'w', encoding='utf-8') as f:
            json.dump(advancement(entry), f, indent=2)
            f.write('\n')
    with open(LANG, encoding='utf-8') as f:
        lang = json.load(f)
    for key in [k for k in lang if k.startswith('advancements.reikocars.')]:
        del lang[key]
    for aid, _, _, title, description, *_ in ADVANCEMENTS:
        lang[f'advancements.reikocars.{aid}.title'] = title
        lang[f'advancements.reikocars.{aid}.description'] = description
    with open(LANG, 'w', encoding='utf-8') as f:
        f.write(json.dumps(lang, indent=2, ensure_ascii=True) + '\n')
    print(f'{len(ADVANCEMENTS)} advancements')


if __name__ == '__main__':
    main()
