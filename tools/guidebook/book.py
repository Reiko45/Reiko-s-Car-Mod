"""Writes the Car Manual (a Patchouli book) into the mod: python3 tools/guidebook/book.py

Book: data/reikocars/patchouli_books/car_manual/book.json
Contents: assets/reikocars/patchouli_books/car_manual/en_us/{categories,entries}/...
Vehicle entries are generated from the workbench recipes and the README's car table; the rest is written below.
"""
import json, os, re, shutil

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = REPO + '/src/main/resources'
BOOK = 'car_manual'
OUT = f'{RES}/assets/reikocars/patchouli_books/{BOOK}/en_us'
LANG = json.load(open(f'{RES}/assets/reikocars/lang/en_us.json'))
RECIPES = {f[:-5] for f in os.listdir(f'{RES}/data/reikocars/recipe')}
IMAGES = set()


def wide(title):
    """About how wide a title is, in screenshot pixels; a page is 232."""
    return sum(6 if c in "il.,'!:;|" else 8 if c in ' tIfk' else 11.5 for c in title)


def fits(title):
    return wide(title) <= 228


def output(recipe):
    """The name of what a recipe makes, as a crafting page titles it; None if it isn't one of ours."""
    if ':' in recipe:
        return None
    result = json.load(open(f'{RES}/data/reikocars/recipe/{recipe}.json')).get('result', {}).get('id', '')
    return name(result.split(':')[1]) if result.startswith('reikocars:') else None


def name(item):
    return LANG.get(f'item.reikocars.{item}') or LANG.get(f'block.reikocars.{item}') or item


def I(item):
    """An item's name, highlighted."""
    return f'$(item){name(item)}$()'


def K(key):
    """A key binding, as the player has it set."""
    return f'$(k:{key if "." in key else "key.reikocars." + key})'


def L(entry, text):
    return f'$(l:{entry}){text}$(/l)'


# ---- pages ----

def text(t, title=None):
    p = {'type': 'patchouli:text', 'text': t}
    if title:
        p['title'] = title
    return p


def craft(recipe, recipe2=None, t=None, title=None):
    for r in (recipe, recipe2):
        if r and r.split(':')[-1] not in RECIPES and not r.startswith('minecraft:'):
            raise SystemExit('no recipe ' + r)
    p = {'type': 'patchouli:crafting', 'recipe': 'reikocars:' + recipe if ':' not in recipe else recipe}
    for r in (recipe, recipe2):
        if r and not title and output(r) and not fits(output(r)):
            raise SystemExit(f'{output(r)}: too wide for a page title')
    if recipe2:
        if t:
            raise SystemExit(f'{recipe}/{recipe2}: a page with two recipes shows no text')
        p['recipe2'] = 'reikocars:' + recipe2 if ':' not in recipe2 else recipe2
    if t:
        p['text'] = t
    if title:
        p['title'] = title
    return p


def spot(item, t, title=None):
    p = {'type': 'patchouli:spotlight', 'item': 'reikocars:' + item if ':' not in item else item, 'text': t}
    if title:
        p['title'] = title
    return p


def image(img, t=None, title=None):
    imgs = img if isinstance(img, list) else [img]
    for i in imgs:
        IMAGES.add(i)
    p = {'type': 'patchouli:image', 'images': [f'reikocars:textures/gui/guide/{i}.png' for i in imgs], 'border': True}
    if t:
        p['text'] = t
    if title:
        p['title'] = title
    return p


def entity(eid, scale, t, offset=0.0, name_=None):
    p = {'type': 'patchouli:entity', 'entity': 'reikocars:' + eid, 'scale': round(scale, 3), 'rotate': True, 'text': t}
    if offset:
        p['offset'] = offset
    if name_:
        p['name'] = name_
    return p


# ---- the book ----

CATEGORIES = []   # (id, name, description, icon, sortnum, parent)
ENTRIES = []      # (category, id, name, icon, sortnum, pages, extra)


def category(cid, title, desc, icon, sort, parent=None):
    CATEGORIES.append((cid, title, desc, icon, sort, parent))


def entry(cat, eid, title, icon, pages, sort=0, priority=False):
    ENTRIES.append((cat, eid, title, icon, sort, pages, priority))


category('getting_started', 'Getting Started', 'What the mod is, building your first car, and the Car Workbench.', 'reikocars:car_workbench', 0)
category('driving', 'Driving', 'The controls, keys, turn signals, the radio, the GPS and the weather.', 'reikocars:car_key', 1)
category('vehicles', 'Vehicles', 'Every car, truck, motorcycle and trailer, and how to build each one.', 'reikocars:sedan', 2)
category('vehicles/cars', 'Cars and Pickups', 'Everyday cars, SUVs and pickups.', 'reikocars:sedan', 0, 'vehicles')
category('vehicles/electric', 'Electric Vehicles', 'Cars that run on BuildCraft MJ.', 'reikocars:city_ev', 1, 'vehicles')
category('vehicles/motorcycles', 'Motorcycles', 'Two wheels: they lean into turns.', 'reikocars:sport_bike', 2, 'vehicles')
category('vehicles/trucks', 'Work Vehicles', 'Diesel trucks, tow trucks, vans and the tractor.', 'reikocars:semi_truck', 3, 'vehicles')
category('vehicles/service', 'Public Service', 'The taxi, bus, police car, ambulance and fire truck.', 'reikocars:city_bus', 4, 'vehicles')
category('vehicles/trailers', 'Trailers', 'The semi\'s trailers, campers and livestock trailers.', 'reikocars:box_trailer', 5, 'vehicles')
category('fuel', 'Fuel and Charging', 'Gasoline, diesel and electricity: where they come from and how to sell them.', 'reikocars:gas_pump', 3)
category('care', 'Repairs and Washing', 'Parts wear out; fix them at a Repair Bench, tow broken cars, wash dirty ones.', 'reikocars:repair_bench', 4)
category('trailers', 'Towing', 'Hitching trailers to a semi or a pickup, campers and livestock.', 'reikocars:box_trailer', 5)
category('roads', 'Roads and Traffic', 'Asphalt, markings, traffic lights, cameras, parking meters and tolls.', 'reikocars:traffic_light', 6)
category('garages', 'Garages', 'Garage doors any size, remotes and buttons, keeping cars safe, and parking garages.', 'reikocars:garage_door', 7)
category('business', 'Money and Business', 'Selling cars, dealerships, loans, rentals, impound lots, insurance, the DMV and jobs.', 'reikocars:dealership', 8)
category('services', 'Public Services', 'Bus routes, mail, emergency vehicles and the work trucks.', 'reikocars:bus_stop_sign', 9)
category('tuning', 'Tuning and Racing', 'Upgrade kits, liveries, and race tracks.', 'reikocars:tuning_bench', 10)

def off_roaders():
    """The vehicles that keep their speed off-road (CarType's off-road column), by name."""
    src = open(f'{REPO}/src/main/java/com/reiko/cars/car/CarType.java').read()
    found = []
    for m in re.finditer(r'^\s{4}([A-Z_]+)\(([^)]*\))', src, re.M):
        values = [v.strip() for v in m.group(2).split(',')]
        if len(values) > 13 and values[13] == 'true':
            found.append(name(m.group(1).lower()))
    return found


def listed(names):
    names = ['the ' + n.lower() for n in names]
    return ', '.join(names[:-1]) + ' and ' + names[-1]


OFF_ROAD = off_roaders()

# -- Getting started --

entry('getting_started', 'welcome', 'Welcome', 'reikocars:sedan', [
    text(f'$(l)Reiko\'s Car Mod$() adds over thirty drivable cars, trucks and motorcycles, and trailers to tow behind them. '
         'They run on $(thing)BuildCraft Reborn$() fuels (or its MJ, for the electric ones), and everything you buy is paid in '
         '$(thing)Reiko\'s Economy Mod$() money, by card or cash.'),
    image('welcome', 'Cars wear out, get dirty, need keys, fuel and insurance, and earn their keep.'),
    text('This book has a chapter for each part of the mod. Start with ' + L('getting_started/first_car', 'Your First Car') + ', then look things up '
         'as you need them.$(br2)Most blocks in this mod belong to whoever puts them down: only they (or an admin) can break them, '
         'and they set the prices and keep the money.'),
    craft('car_manual', None, 'Lost this book? Make another from a book and an iron ingot, or open it from the Car Workbench\'s Guide button.'),
], sort=0, priority=True)

entry('getting_started', 'first_car', 'Your First Car', 'reikocars:sedan', [
    text(f'$(li)Craft a {I("car_workbench")} and put it down.'
         f'$(li)Craft the car\'s three parts: its $(thing)chassis$(), $(thing)body$() and $(thing)interior$() (see '
         + L('vehicles', 'Vehicles') + ' for each car\'s).'
         f'$(li)Craft an engine (a {I("car_engine")} for most cars) and four {I("tire")}s.'),
    text('$(li)Build the car at the workbench (see ' + L('getting_started/workbench', 'The Car Workbench') + ') and put it down.'
         f'$(li)Cut a key: sneak and right-click the car with a {I("blank_key")} (' + L('driving/keys', 'Car Keys') + ').'
         '$(li)Fill it up at a pump (' + L('fuel/pumps', 'Gas and Diesel Pumps') + ') and drive off (' + L('driving/controls', 'Controls') + ').'),
    craft('tire', None, 'Tires are made from dried kelp and black dye.'),
    craft('car_engine', None, 'The engine is built round a BuildCraft Combustion Engine.'),
], sort=1, priority=True)

entry('getting_started', 'workbench', 'The Car Workbench', 'reikocars:car_workbench', [
    text('Cars and trailers are put together at a $(item)Car Workbench$(). Right-click it: the buttons along the top are the '
         'vehicles it builds. Hover over one to see which parts you have; click it to put every part you carry into place. '
         'A green tick means you carry them all.'),
    craft('car_workbench'),
    image('workbench', 'Slots laid out like the car from above; parts not fitted yet show as blue ghosts.'),
    text('Every car takes a $(thing)chassis$(), a $(thing)body$(), an $(thing)interior$(), an engine and four tires; a motorcycle '
         'two tires. Diesel trucks take a $(item)Diesel Engine$(), motorcycles a $(item)Motorcycle Engine$() and electric cars an '
         '$(item)Electric Motor$(). A trailer takes the $(item)Trailer Chassis$(), its own body and four tires.$(br2)'
         'Parts left in the workbench go back to you when you close it. With $(thing)JEI$(), each vehicle has a recipe page too.'),
    craft('diesel_engine', 'motorcycle_engine'),
    craft('electric_motor', 'trailer_chassis'),
], sort=2)

# -- Driving --

entry('driving', 'controls', 'Controls', 'reikocars:car_key', [
    text('Right-click a car to get in: the first one in drives, with one of its keys in your inventory. '
         '$(k:forward) $(k:left) $(k:back) $(k:right) drive and steer, $(k:jump) is the handbrake and $(k:sneak) gets out.'
         f'$(br2){K("horn")} horn$(br){K("lock")} lock or unlock$(br){K("hitch")} hitch, load, tow, or a work vehicle\'s job'),
    text(f'{K("siren")} lights and siren (police, ambulance, fire)$(br){K("signal_left")} and {K("signal_right")} turn signals$(br)'
         f'{K("hazards")} hazards$(br){K("radio")} radio$(br){K("gps")} GPS$(br2)'
         'All of these can be changed under Controls, in the Reiko\'s Car Mod section.'),
    image('dashboard', 'The dashboard shows your speed, fuel, the road and any warning lights.'),
    text('$(l)Owning a car:$() whoever puts it down owns it. New cars start locked. Paint one by right-clicking it with a dye. '
         'Punch it a few times to pick it up (owner only): it keeps its fuel, paint, locks and wear.$(br2)'
         '$(l)Off-road:$() grass, sand, snow and dirt slow every car but ' + listed(OFF_ROAD) + '.'),
    text('$(l)Headlights$() come on with the engine and really light the road in the dark, about twelve blocks ahead.$(br2)'
         '$(l)Running things over$() hurts mobs and knocks them aside; players only with PvP on. Cars climb a full block, so curbs and '
         'stairs don\'t stop them, and can\'t be blown up or burned.'),
], sort=0, priority=True)

entry('driving', 'keys', 'Car Keys', 'reikocars:car_key', [
    text('Every vehicle needs one of its keys to start, $(l)even for its owner$(). With none in your inventory the dashboard says '
         f'$(4)NO KEY$().$(br2)Craft {I("blank_key")}s, then sneak and right-click your car with one to cut a key for it. Only the '
         'owner can cut keys; cut as many as you like and hand them out.'),
    craft('blank_key'),
    text('A key lets whoever carries it start and drive the car, get in when it\'s locked, open its trunk and lock or unlock it. '
         'Right-click with a key to lock its car from up to 32 blocks away.$(br2)$(l)Lost one?$() Sneak and right-click the car twice '
         'with one of its keys: only that key fits from then on. Trailers take keys too.'),
], sort=1)

entry('driving', 'signals', 'Signals and Alarms', 'minecraft:redstone_torch', [
    text(f'{K("signal_left")} and {K("signal_right")} signal a turn: that side\'s lamps blink amber and an arrow blinks on the dashboard. '
         f'A signal cancels itself after the turn. {K("hazards")} blinks both sides; a car that breaks down puts its hazards on by itself.'),
    text('$(l)Car alarms:$() a locked car\'s alarm goes off when someone without its key hits it or tries to get in. It sounds for '
         'half a minute with every light flashing, and its owner is told where and who. Stop it with ' + K('lock') + ', with its key, '
         'or by getting in.'),
], sort=2)

entry('driving', 'radio', 'Car Radio', 'minecraft:jukebox', [
    text(f'Everyone riding can press {K("radio")} to open the radio: power, three stations and a disc slot.$(br)'
         '$(li)$(l)88.1 Neon FM$(): synthwave$(li)$(l)94.5 Chill FM$(): lo-fi beats$(li)$(l)101.3 Dusty Roads$(): country guitar$(br2)'
         'The stations play round the clock, so every car on one hears the same moment of the same song.'),
    image('radio', 'DISC plays the music disc in the slot, on repeat. People outside hear it too.'),
], sort=3)

entry('driving', 'gps', 'GPS', 'minecraft:compass', [
    text(f'Press {K("gps")}, in a car or on foot, and pick somewhere to go:$(li)$(l)Places$(): gas stations, chargers, mechanics, '
         'car washes, dealerships and every other business, nearest first, with their prices. Type to search.'
         '$(li)$(l)My Cars$(): your cars, wherever you left them.$(li)$(l)Coordinates$(): any X and Z.$(br2)A chime tells you when you\'ve arrived.'),
    image('gps_places', 'The Places list, with prices.'),
    image('gps_driving', 'An arrow at the top points the way, with the distance and time to go.'),
    text('$(l)With Xaero\'s Minimap$(), the destination shows as a $(thing)GPS$() waypoint on the minimap and World Map, your cars '
         'show on the minimap, and a $(l)Waypoints$() tab lists your waypoints to head for.'),
], sort=4)

entry('driving', 'weather', 'Weather', 'minecraft:snowball', [
    text('Rain, snow and storms take away grip: a car is slower to get going, takes longer to stop, and slides wide in hard '
         'turns.$(br)$(li)Rain: 80% grip$(li)Thunderstorm: 65%$(li)Snow: 55%$(li)Ice: like glass$(br2)'
         'The dashboard shows WET, STORM, SNOW or ICE.'),
    image('rain', 'Wipers sweep the windshield in rain and snow; water sprays up behind the wheels.'),
    image('snow_on_cars', 'Snow covers parked cars, and blows off once they\'re driven.'),
    text('Lightning sometimes strikes a car in a storm, but the people inside are safe.$(br2)$(item)Winter Tires$(), fitted at a ' + L('tuning/tuning', 'Tuning Bench') + ', win back most of the grip on snow and ice '
         'and half of it in the rain. Headlights come on by themselves in bad weather.'),
], sort=5)

# -- Fuel --

entry('fuel', 'fuels', 'Gasoline and Diesel', 'reikocars:diesel_pump', [
    text('Cars run on $(thing)gasoline$(): BuildCraft\'s $(item)Light Fuel$() (and the Mixed Light and Gaseous Fuels), or other mods\' '
         'gasoline. Diesel trucks run on $(thing)diesel$(): BuildCraft\'s $(item)Dense Fuel$() and Mixed Heavy Fuels, or other mods\' '
         'diesel. Neither takes the other\'s fuel, and nothing runs on crude oil.'),
    text('A sedan goes about 600 blocks on a bucket, a semi about 250. A car burns fuel for every block it drives, and a little '
         'while idling. Its tooltip says what it runs on.$(br2)Refine BuildCraft oil into fuel, pipe it into a pump, and sell it: '
         'see ' + L('fuel/pumps', 'Gas and Diesel Pumps') + '.'),
], sort=0)

entry('fuel', 'pumps', 'Gas and Diesel Pumps', 'reikocars:gas_pump', [
    text('The $(item)Gas Pump$() sells gasoline and the $(item)Diesel Pump$() diesel. Each is two blocks tall and holds 16 buckets. '
         'Pipe fuel into either half from any side (nothing comes back out), or fill it from a bucket.'),
    craft('gas_pump', 'diesel_pump'),
    image('pump', 'Right-click a pump to fill the car at its hose (within 6 blocks) or your buckets.'),
    text('Pay by card or cash. The money goes straight to the pump\'s owner, even while they\'re offline. The owner fills up free, and sets the price '
         'per bucket at the bottom of the screen; the display on the pump shows it, or EMPTY.'),
], sort=1)

entry('fuel', 'charging', 'Electric Cars', 'reikocars:charging_station', [
    text('Five vehicles run on electricity: the City EV, Electric Sedan, Electric Pickup, Electric Van and Electric Moped. '
         'Their batteries are charged at a $(item)Charging Station$(), in BuildCraft MJ. They don\'t idle, they need no oil, and '
         'braking puts some charge back.'),
    craft('charging_station'),
    text('A charging station stores 500,000 MJ, fed by BuildCraft engines or kinesis pipes on either half (or other mods\' cables). '
         'Park within 6 blocks, right-click it and pay by card or cash for as much charge as you want. A full charge costs far less '
         'than a tank of gasoline.'),
], sort=2)

# -- Care --

entry('care', 'wear', 'Wear and Tear', 'reikocars:engine_parts', [
    text('Each car has five parts, from 100% down to 0%:$(li)$(l)Oil$(): an oil change every 50,000 blocks or so$(li)$(l)Engine$(): '
         'slowly, and in hard crashes$(li)$(l)Tires$(): by distance, faster off-road$(li)$(l)Brakes$(): every stop$(li)$(l)Body$(): crashes'),
    text('Worn parts make a car slower, thirstier and slower to stop. The dashboard\'s warning lights turn amber, then orange, and '
         'flash red when a part is gone: at 0% the car $(4)breaks down$() and won\'t run until it\'s repaired. Cars are never '
         'destroyed.$(br2)Trailers wear their tires, brakes and body too. Electric cars have no oil.'),
], sort=0)

entry('care', 'repair_bench', 'The Repair Bench', 'reikocars:repair_bench', [
    text('Park within 6 blocks of a $(item)Repair Bench$() and right-click it. It shows the car part by part. $(l)Fit$() puts in '
         'parts you carry, free; the $(l)price$() button has the bench fit parts from its own stock, paid to its owner by card or cash.'),
    craft('repair_bench'),
    text('An oil change takes a $(item)Motor Oil$(), brakes a set of $(item)Brake Pads$(). $(item)Engine Parts$() put back a quarter '
         'of the engine, $(item)Body Panel$()s a fifth of the body, and tires take a full set of $(item)Tire$()s. Diesel trucks and '
         'trailers take two of each.'),
    craft('motor_oil', 'brake_pads'),
    craft('engine_parts', 'body_panel'),
    text('The bench\'s owner gets '
         '$(l)Stock$() and $(l)Prices$() tabs, to run it as a mechanic\'s shop: hoppers and pipes can fill its stock. Fitting parts to '
         'someone else\'s car pays the $(thing)Mechanic$() job.'),
], sort=1)

entry('care', 'washing', 'Dirt and Car Washes', 'reikocars:car_wash', [
    text('Cars get dirty as they\'re driven, more off-road, most in mud, twice as fast in the rain. A filthy car sells for '
         'less and its body slowly $(4)rusts$().$(br2)Right-click a car with a $(item)Water Bucket$() to wash some of it off, or use a car wash.'),
    image('car_wash', 'The Car Wash: drive up, press ' + K('hitch') + ' to pay, then drive through slowly.'),
    craft('car_wash', 'wash_bay'),
    image('wash_bay', 'The Wash Bay rents you a pressure washer: hold right-click to spray a car clean.'),
], sort=2)

entry('care', 'towing', 'Tow Trucks', 'reikocars:flatbed', [
    text(f'Two diesel tow trucks fetch broken-down cars. Back up to the car and press {K("hitch")}:'
         '$(li)The $(l)flatbed$() winches it up onto its deck.$(li)The $(l)wrecker$() lifts it by its front wheels; it swings out behind on bends.$(br2)'
         f'{K("hitch")} again puts it down.'),
    text('They take any car, van or pickup up to about 4.8 blocks long (the flatbed takes motorcycles too), if it\'s yours or '
         'unlocked and nobody\'s in it. Towing someone else\'s broken-down car to a Repair Bench pays the Mechanic job. '
         'Cars at an expired parking meter can be towed to an ' + L('business/impound', 'impound lot') + ', locked or not.'),
], sort=3)

# -- Towing --

entry('trailers', 'semis', 'Semi Trailers', 'reikocars:box_trailer', [
    text('The $(item)Semi Truck$() pulls one trailer at a time:$(li)$(l)Box Trailer$(): 54 slots of cargo$(li)$(l)Tanker Trailer$(): '
         '64 buckets of any fluid$(li)$(l)Car Hauler$(): two cars$(li)and the big $(l)Fifth-Wheel Camper$() and $(l)Semi Livestock Trailer$()'),
    image('semi', 'Put a trailer down facing the way you\'ll drive, back the semi under its front and press ' + K('hitch') + '.'),
    text('The trailer swings round behind you and cuts corners, so take them wide. Pulling one, the semi is slower and burns '
         'more fuel. Back up too far round and it $(4)jackknifes$().$(br2)Trailers are solid, and the rig stops if the trailer would '
         'hit a wall.'),
    text('$(l)The tanker:$() fill it with a bucket, or park it beside tanks and right-click it with an empty hand to load from '
         'them all, or unload into them: a row of pumps, say.$(br2)$(l)The car hauler:$() drive a car up to its back and press '
         + K('hitch') + ' to drive on; ' + K('hitch') + ' again drives off.'),
], sort=0)

entry('trailers', 'tow_ball', 'The Tow Ball', 'reikocars:pickup_truck', [
    text('The pickups, the Off-Roader, the Minivan and the Farm Tractor have a $(l)tow ball$(). They pull the $(item)Camper$() and the '
         '$(item)Livestock Trailer$().$(br2)Put the trailer down, back up to its tongue so the ball is under the coupler, '
         'and press ' + K('hitch') + '. Press it again to unhitch.'),
    image('pickup_camper', 'A camper on a pickup\'s ball: it swings round behind on bends.'),
], sort=1)

entry('trailers', 'campers', 'Campers', 'reikocars:camper', [
    text('Right-click a camper to go inside: $(l)cupboards$() for your things, a $(l)stove$() that cooks food as a smoker does (only '
         'while it\'s parked), and a $(l)Sleep$() button.'),
    image('camper_screen', 'Food on top, fuel underneath, and what\'s cooked on the right.'),
    text('$(l)Sleeping$() works like a bed: it sets your spawn outside the door, and once enough players are asleep the night '
         'passes. Only in a parked camper, at night, with no monsters close by. Each camper sleeps two; you wake outside.'),
    image('camp', 'Parked, the camp unfolds: awning, lights, steps, jacks, and chairs to sit in.'),
    image('camp_night', 'At night the lights and the windows glow. Hitch it and everything folds away.'),
], sort=2)

entry('trailers', 'livestock', 'Livestock', 'reikocars:livestock_trailer', [
    text('Livestock trailers carry cows, sheep, pigs, goats, horses, donkeys, mules, llamas and chickens. Each pen holds one '
         'horse, two cows, or four chickens.$(br2)Parked, the back gate drops down as a $(l)ramp$().'),
    image('livestock_ramp', 'Lead animals up the ramp on leads and they walk in. You get the leads back.'),
    text('$(li)Right-click the trailer with animals following on leads to load them all.$(li)Right-click with an empty hand to let '
         'them all out down the ramp.$(li)Right-click with a lead to take the last one out on it.$(br2)Inside, they ride along '
         'between the slats.'),
    image('livestock', 'Animals ride wherever the trailer goes.'),
], sort=3)

# -- Roads --

entry('roads', 'roads', 'Roads', 'reikocars:asphalt', [
    text('Cars go $(l)15% faster$() on $(item)Asphalt$(), and their tires wear slower. $(item)Road Paint$() marks it: right-click the air '
         'to pick a marking (center lines, lane and edge lines, stop lines, crosswalks, arrows), then the asphalt. Paint the same '
         'thing again to scrape it off.'),
    craft('asphalt', 'road_paint'),
    image('roads', 'Street Poles hold up traffic lights, signs and street lamps, which come on at dusk.'),
    craft('street_pole', 'street_lamp'),
    craft('stop_sign'),
], sort=0)

entry('roads', 'traffic_lights', 'Traffic Lights', 'reikocars:traffic_light', [
    text('All $(item)Traffic Light$()s run on one clock: green, yellow, red, with north-south and east-west taking turns. Put one up '
         'facing each road into a crossing. Redstone holds a light on red.'),
    craft('traffic_light', 'speed_sign'),
    text('$(l)Cameras:$() a light\'s owner right-clicks it to set a fine for running its red; a $(item)Speed Limit Sign$()\'s owner sets '
         'the limit and a speeding fine. Fines come out of the car owner\'s bank account, go to the camera\'s owner, and put penalty '
         'points on the driver\'s ' + L('business/dmv', 'license') + '.'),
], sort=1)

entry('roads', 'parking', 'Parking Meters', 'reikocars:parking_meter', [
    text('A $(item)Parking Meter$() charges the car parked beside it (within 3 blocks). Right-click it, choose 10 to 120 minutes, and '
         'pay by card or cash to its owner.$(br2)Overstay and, after a minute\'s grace, the car\'s owner gets a $(4)parking ticket$(), '
         'and another every 15 minutes.'),
    craft('parking_meter'),
    text('The owner\'s cars park free. The $(l)Settings$() tab sets the rate and the fine. A car left at an expired meter can be '
         'towed to an ' + L('business/impound', 'impound lot') + '.'),
], sort=2)

entry('roads', 'tolls', 'Toll Gates', 'reikocars:toll_gate', [
    text('A $(item)Toll Gate$() closes the road until the toll is paid. Drive up and press ' + K('hitch') + ' to pay by card or cash, '
         'and the arm swings up. A $(item)Toll Booth$() within 12 blocks sets the toll; its owner gets the money and drives through free.'),
    craft('toll_gate', 'toll_booth'),
], sort=3)

# -- Garages --

entry('garages', 'doors', 'Garage Doors', 'reikocars:garage_door', [
    text('Build a garage door any size from panels: fill a doorway with $(item)Garage Door$()s or $(item)Roller Shutter$()s and, '
         'once they make a full rectangle, they join into one door, up to 16 wide and 10 high.$(br2)Right-click it to open or '
         'close it. It won\'t come down on anything in the way.'),
    image('garage_opening', 'A Garage Door\'s panels roll up into the garage, under the ceiling if there\'s room.'),
    craft('garage_door', 'roller_shutter'),
    image('shutter', 'A Roller Shutter\'s slats coil up into a box over the opening.'),
    text('$(l)Paint$() a door with any dye. A Garage Door has a row of $(l)windows$() along its top.$(br2)Sneak-right-click a door '
         'with an empty hand for its screen: its name, windows on or off, opening for cars, and which side\'s the garage (it tells '
         'by the roof). Only its owner, or an admin, can paint it, set it up or break it.'),
    image('garage_inside', 'Up under the ceiling, on its rails.'),
], sort=0)

entry('garages', 'opening', 'Remotes and Buttons', 'reikocars:garage_remote', [
    text('A door opens for its owner, an admin, and anyone carrying a $(item)Garage Remote$() linked to it. From inside, anyone '
         'can open it: nobody\'s shut in.$(br2)Drive up to it and it $(l)opens by itself$(), then shuts behind you once you\'re '
         'through. ' + K('hitch') + ' opens or closes it from the car, too.'),
    image('opens_for_car', 'Its owner\'s cars open it, whoever\'s driving.'),
    text('$(l)Remotes:$() sneak-right-click your door with one to link it (up to 4 doors) and give it to whoever you want to let '
         'in. Right-click it, even driving, to open or close the nearest of its doors within 64 blocks. $(l)Reset remotes$() on the '
         'door\'s screen stops them all working.$(br2)A $(item)Garage Door Button$() on a wall works the nearest door within 16 blocks.'),
    craft('garage_remote', 'garage_button'),
    text('$(l)Safe inside:$() a car shut in a garage, behind a closed door and under a roof, can\'t be stolen, towed or impounded. '
         'Locked or not, only its owner and whoever has its key can get in it or tow it away.$(br2)It doesn\'t rust while it\'s in '
         'there, and no snow settles on it.'),
    image('garage_shut', 'Shut, painted, windows along the top: whatever\'s inside is safe.'),
], sort=1)

entry('garages', 'parking_garages', 'Parking Garages', 'reikocars:roller_shutter', [
    text('On a door\'s screen, turn on $(l)Parking garage$() and set its prices: an hour and a day.$(br2)Drivers pull up and press '
         + K('hitch') + ' to pay for 1 to 12 hours or 1 to 7 days, by card or cash, to you. The door then opens for their car, '
         'coming and going, until its time\'s up.'),
    image('parking_screen', 'Pull up, press ' + K('hitch') + ', and pay.'),
    text('A car still inside when its time\'s up pays for the hours it\'s overstayed to get out. Your own cars park free, and '
         'anyone with a remote.$(br2)Parking garages are on the ' + L('driving/gps', 'GPS') + ', with their hourly price. An hour is a '
         'Minecraft hour (50 seconds), a day a Minecraft day.'),
    image('garage_evening', 'Open for business.'),
], sort=2)

# -- Business --

entry('business', 'selling', 'Buying and Selling', 'reikocars:for_sale_sign', [
    text('Right-click your car with a $(item)For Sale Sign$() to put it up for sale at a price. Anyone else right-clicks it to buy, '
         'by card or cash, and the money goes to you. The buyer gets a key for new locks, so your keys stop fitting.'),
    craft('for_sale_sign'),
], sort=0)

entry('business', 'dealerships', 'Dealerships and Loans', 'reikocars:dealership', [
    text('A $(item)Car Dealership$()\'s owner parks cars within 10 blocks of it to make a lot, prices them on the $(l)Lot$() tab, and '
         'sets what it pays for trade-ins. Customers buy from the lot, or sell their own car to the dealership.'),
    craft('dealership'),
    image('finance', 'Buy on $(l)finance$(): 20% down, then ten daily payments, with 10% interest.'),
    text('Until a loan\'s paid off, the car can\'t be sold or picked up. Miss three payments in a row and it\'s $(4)repossessed$() '
         'to the nearest impound lot. Pay off the loan early from the dealership\'s Trade in tab.'),
], sort=1)

entry('business', 'rentals', 'Car Rentals', 'reikocars:rental_desk', [
    text('Park your cars within 10 blocks of a $(item)Car Rental Desk$() to make a fleet. Customers pick a car and rent it for 1 to 7 '
         'days (a day is 20 minutes), paid to the desk\'s owner.$(br2)A renter drives it without a key. When the time\'s up, it '
         'drives itself back to the lot once nobody\'s in it.'),
    craft('rental_desk'),
    image('rentals', 'A rental lot.'),
], sort=2)

entry('business', 'impound', 'Impound Lots', 'reikocars:impound_office', [
    text('An $(item)Impound Office$()\'s lot is everything within 10 blocks. Cars towed in from expired parking meters, and cars '
         'repossessed over missed loan payments, are held there under an $(4)IMPOUNDED$() sign.$(br2)Right-click the office to pay '
         'the release fee and get your car back.'),
    craft('impound_office'),
    image('impound', 'An impound lot.'),
], sort=3)

entry('business', 'insurance', 'Insurance', 'reikocars:insurance_office', [
    text('Park within 10 blocks of an $(item)Insurance Office$() and buy cover: a term of 7 Minecraft days. Then when crash damage '
         'to the body is repaired at a Repair Bench, the insurer pays back what it cost over the deductible, up to the claim limit.'
         '$(br2)Wear isn\'t covered. The office\'s owner sets the prices and pays the claims.'),
    craft('insurance_office'),
], sort=4)

entry('business', 'dmv', 'Licenses and the DMV', 'reikocars:dmv_office', [
    text('Every car gets its own $(l)license plate$(). At a $(item)DMV Office$() you can pay for the $(l)driving test$(), buy a new license '
         'card, or buy a $(l)custom plate$() for a car parked nearby.'),
    craft('dmv_office', 'traffic_cone'),
    image('dmv', 'The test: drive through the DMV\'s numbered Traffic Cones in order, inside 5 minutes.'),
    text('Red-light and speed cameras put $(l)penalty points$() on your license; 12 at once suspends it for 3 days. Without a valid '
         'license, cameras add a $25 fine, and a $(thing)Police Officer$() with a $(item)Ticket Book$() can fine you.'),
    craft('ticket_book'),
], sort=5)

entry('business', 'fares', 'Taxi and Bus Fares', 'reikocars:taxi', [
    text('$(l)The bus:$() sit down while someone\'s driving and pay the fare, by card or cash.$(br2)$(l)The taxi:$() get in and the '
         'meter runs; when you get out the fare comes out of your bank account. Both are paid to the driver.$(br2)The owner sets '
         'the fares by sneaking and right-clicking the vehicle with an empty hand.'),
], sort=6)

entry('business', 'jobs', 'Car Jobs', 'minecraft:emerald', [
    text('Reiko\'s Economy Mod\'s job board has seven car jobs, which pay as you work:$(li)$(l)Driver$(): taxi rides, bus passengers, '
         'bus stops$(li)$(l)Mail Carrier$(): parcels$(li)$(l)Mechanic$(): repairs, tuning, tow-ins'),
    text('$(li)$(l)Trucker$(): fuel delivered by tanker, and hauling a loaded trailer$(li)$(l)Garbage Collector$(): trash cans emptied'
         '$(li)$(l)Firefighter$(): fires put out$(li)$(l)Police Officer$(): unlicensed drivers ticketed'),
], sort=7)

# -- Services --

entry('services', 'bus_stops', 'Bus Stops', 'reikocars:bus_stop_sign', [
    text('A $(item)Bus Stop Sign$() or a $(item)Bus Shelter$() makes a stop. Its owner right-clicks it to name it and put it on up to '
         'three $(l)routes$(), with its stop number on each.'),
    craft('bus_stop_sign', 'bus_shelter'),
    image('bus_shelter', 'Right-click a stop to wait there, or sit on a shelter\'s bench.'),
    text('Give a bus a route where its fare is set. Stopped at a stop, its driver presses ' + K('hitch') + ' to open the doors: '
         'whoever rang gets off and whoever\'s waiting gets on. Passengers press ' + K('hitch') + ' to ring for the next stop. '
         'Villagers ride too, and pay.'),
    image('bus_route', 'Everyone aboard is told each stop, and the next one.'),
], sort=0)

entry('services', 'mail', 'Mail', 'reikocars:mailbox', [
    text('$(item)Mailbox$()es send parcels between players. Right-click one, type who it\'s for, put things in and pay the postage: '
         'the parcel waits in the mailbox and its flag goes up.$(br2)The $(item)Mail Van$() carries the mail: drive up beside a mailbox '
         'and press ' + K('hitch') + ' to collect and deliver. The driver earns each parcel\'s postage.'),
    craft('mailbox'),
], sort=1)

entry('services', 'emergency', 'Police and Ambulance', 'reikocars:police_car', [
    text(f'The driver\'s {K("siren")} turns on the $(l)light bar and siren$(). The $(item)Ambulance$() heals everyone riding in the back. '
         'A $(thing)Police Officer$() checks plates and licenses with a $(item)Ticket Book$() (see ' + L('business/dmv', 'the DMV') + ').'),
], sort=2)

entry('services', 'fire', 'Fire Trucks', 'reikocars:fire_truck', [
    text('The $(item)Fire Truck$() has a $(l)water tank$(), a $(l)hose$() and an $(l)aerial ladder$(). Hold right-click with a '
         '$(item)Fire Hose$() within 16 blocks of it to spray water up to twenty blocks: it puts out fire, campfires and burning mobs.'),
    image('hose', 'Every fire put out pays the Firefighter job.'),
    text('Fill the tank at a $(item)Fire Hydrant$() (park within 6 blocks), from open water beside the truck, or by the bucket.$(br2)'
         'Park and press ' + K('hitch') + ': the ladder swings up behind the truck, up to 16 blocks. Climb it like scaffolding.'),
    craft('fire_hose', 'fire_hydrant'),
    image('ladder', 'The ladder runs up a wall when the truck backs up to a building.'),
], sort=3)

entry('services', 'work_trucks', 'Work Trucks', 'reikocars:garbage_truck', [
    text('$(l)Garbage truck:$() back its hopper up to $(item)Trash Can$()s and press ' + K('hitch') + ' to empty them.$(br2)'
         '$(l)Dump truck:$() load its bed (sneak + right-click), then ' + K('hitch') + ' tips it out.$(br2)'
         '$(l)Cement mixer:$() its drum sets concrete powder into concrete while the engine runs; ' + K('hitch') + ' pours it.'),
    craft('trash_can'),
    text('$(l)Ice cream truck:$() a shop on wheels. Stock its freezer (sneak + right-click), price each kind at the serving hatch, '
         'and everyone else buys from it by card or cash. Its horn plays a jingle.'),
    craft('vanilla_ice_cream', 'chocolate_ice_cream'),
], sort=4)

# -- Tuning --

entry('tuning', 'tuning', 'The Tuning Bench', 'reikocars:tuning_bench', [
    text('Park within 6 blocks of a $(item)Tuning Bench$() and right-click it to fit upgrade kits: fit your own free, or buy them '
         'from the bench\'s stock. Only a car\'s owner can have it tuned.'),
    craft('tuning_bench'),
    image('tune_tab', 'The Tune tab shows each kit and how far the car has it.'),
    text('$(li)$(l)Engine Tune Kit$(): +6% speed a stage (up to 3)$(li)$(l)Turbocharger$(): +25% acceleration$(li)$(l)Sport Tires$(): '
         'turn sharper$(li)$(l)Off-Road Tires$(): less slowed off-road$(li)$(l)Winter Tires$(): grip on snow and ice'),
    text('$(li)$(l)Performance Brakes$(): stop harder$(li)$(l)Lowering Springs$(): turn sharper$(li)$(l)Lift Kit$(): better off-road'
         '$(li)$(l)Extended Fuel Tank$() or $(l)Extra Battery Module$(): go further$(br2)Each costs something: more fuel, more wear, '
         'or a little speed.'),
    craft('engine_tune_kit', 'turbocharger'),
    craft('sport_tires', 'off_road_tires'),
    craft('winter_tires', 'performance_brakes'),
    craft('lowering_springs', 'lift_kit'),
    craft('extended_fuel_tank', 'battery_module'),
], sort=0)

entry('tuning', 'livery', 'Liveries', 'minecraft:white_dye', [
    text('The Tuning Bench\'s $(l)Livery$() tab puts a $(l)race number$() on both doors and $(l)racing stripes$() in any color along '
         'the hood, roof and trunk. It\'s free.'),
    image('livery', 'A car with a race number and stripes.'),
], sort=1)

entry('tuning', 'racing', 'Racing', 'reikocars:race_control', [
    text('A $(item)Race Control$() is the start and finish line. Put down $(item)Race Flag$()s and right-click each to number it: '
         'a lap is every flag in order and back over the line.$(br2)$(l)Time trials$() are free. $(l)Races$() take an entry fee '
         'into a pot, and the winner takes it.'),
    craft('race_control', 'race_flag'),
    image('racing', 'Racing: the clock, your lap, the next checkpoint and your place.'),
], sort=2)

# -- Vehicles, from the workbench recipes, CarType/TrailerType and the README's car table --

def car_dimensions():
    src = open(f'{REPO}/src/main/java/com/reiko/cars/car/CarType.java').read()
    dims = {}
    for m in re.finditer(r'^\s{4}([A-Z_]+)\(\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,', src, re.M):
        dims[m.group(1).lower()] = tuple(float(m.group(i)) for i in (2, 3, 4))  # width, height, length
    return dims


def trailer_dimensions():
    src = open(f'{REPO}/src/main/java/com/reiko/cars/trailer/TrailerType.java').read()
    dims = {}
    for m in re.finditer(r'^\s{4}([A-Z_]+)\(\s*Hitch\.\w+,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,', src, re.M):
        dims[m.group(1).lower()] = (float(m.group(3)), float(m.group(4)), float(m.group(2)))  # width, height, length
    return dims


def readme_cars():
    rows = {}
    for line in open(f'{REPO}/README.md'):
        m = re.match(r'^\| ([A-Z][^|]+?) \| (\d+) \| (\d+ mph) \| ([^|]+?) \| ([^|]+?) \|$', line)
        if m:
            rows[m.group(1)] = m.group(2, 3, 4, 5)
    return rows


def plain(md):
    """README markdown to plain book text: links and bold dropped."""
    md = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', md)
    md = re.sub(r'\s*\(see [^)]*\)', '', md)
    return md.replace('**', '').strip()


def scaled(width, height, length):
    size = max(1.0, width, height)
    default = 100.0 / size * 0.8
    target = 82.0 / max(length, height * 1.25, 1.0)
    offset = height / 2.0 - max(height, size) / 2.0
    return target / default, round(offset, 3)


def lines(t, width=30):
    """About how many lines text takes on a page."""
    t = re.sub(r'\$\(k:[^)]+\)', 'G', t)
    t = re.sub(r'\$\(br2\)', '\n\n', t)
    t = re.sub(r'\$\((br|li)\)', '\n', t)
    t = re.sub(r'\$\([^)]*\)', '', t)
    n = 0
    for para in t.split('\n'):
        line = 0
        n += 1
        for word in para.split(' '):
            if line and line + 1 + len(word) > width:
                n += 1
                line = len(word)
            else:
                line += (1 if line else 0) + len(word)
    return n


DIMS = car_dimensions()
TDIMS = trailer_dimensions()
TABLE = readme_cars()

GROUPS = {
    'vehicles/cars': ['sedan', 'hatchback', 'sports_car', 'muscle_car', 'convertible', 'minivan', 'off_roader', 'pickup_truck', 'heavy_pickup'],
    'vehicles/electric': ['city_ev', 'electric_sedan', 'electric_pickup', 'electric_van', 'electric_moped'],
    'vehicles/motorcycles': ['sport_bike', 'cruiser', 'dirt_bike', 'scooter'],
    'vehicles/trucks': ['semi_truck', 'delivery_van', 'farm_tractor', 'flatbed', 'wrecker', 'garbage_truck', 'dump_truck', 'cement_mixer',
                        'ice_cream_truck', 'mail_van'],
    'vehicles/service': ['taxi', 'city_bus', 'police_car', 'ambulance', 'fire_truck'],
}
TRAILERS = {
    'box_trailer': '54 slots of cargo, opened like a double chest.',
    'tanker_trailer': '64 buckets of any one fluid, loaded and unloaded at tanks.',
    'car_hauler': 'Carries two cars: drive them up to its back and press ' + K('hitch') + '.',
    'camper': 'On a tow ball. Sleeps 2; 27 cupboard slots, a stove, and a camp when parked.',
    'livestock_trailer': 'Bumper-pull, on a tow ball. Pens for 6 cows, 3 horses or 12 chickens.',
    'fifth_wheel_camper': 'For the semi. Sleeps 2; 45 cupboard slots, a stove, a slide-out and a camp.',
    'semi_livestock_trailer': 'For the semi. Pens for 12 cows, 6 horses or 24 chickens.',
}
SHORT = {  # shorter notes, to fit under the 3D view; the README has the full ones
    'Heavy-Duty Pickup': 'Lifted 4x4: full speed off-road, tow ball',
    'Farm Tractor': 'Full speed on grass, dirt and farmland; tow ball',
    'Wrecker Tow Truck': 'Lifts a car by its front wheels; 18-slot toolboxes',
    'Cement Mixer': 'Its drum sets concrete powder into concrete',
    'Ice Cream Truck': 'A shop on wheels, with a 27-slot freezer',
    'Mail Van': 'Collects and delivers parcels; 36 slots of mail',
    'City Bus': 'A driver and nine paying passengers; swing wide',
    'Ambulance': 'Siren; heals whoever rides in the back',
    'Fire Truck': 'Siren, water tank, fire hose and a ladder',
}
ENGINE = {}
for f in os.listdir(f'{RES}/data/reikocars/recipe'):
    d = json.load(open(f'{RES}/data/reikocars/recipe/{f}'))
    if d.get('type') == 'reikocars:car_assembly':
        ENGINE[f[:-5]] = (d.get('engine', {}).get('item', '').split(':')[-1], d.get('wheels', 4))

entry('vehicles', 'building', 'Building a Vehicle', 'reikocars:car_workbench', [
    text('Every vehicle in this chapter is built at the ' + L('getting_started/workbench', 'Car Workbench') + ' from its own parts. '
         'Each entry shows the vehicle turning in 3D, what it does, and how to craft its parts.'),
], sort=-1, priority=True)

for cat, cars in GROUPS.items():
    for n, car in enumerate(cars):
        title = name(car)
        seats, speed, tank, notes = TABLE[title]
        w, h, l = DIMS[car]
        scale, offset = scaled(w, h, l)
        engine, wheels = ENGINE[car]
        stats = f'$(l){seats}$() seat{"s" if seats != "1" else ""} · $(l){speed}$() · {tank}'
        note = SHORT.get(title) or plain(notes)
        later = ''
        if lines(stats + '$(br)' + note) <= 3:
            stats += '$(br)' + note
        else:
            later = plain(notes) + '.$(br2)'  # the whole note, on the interior's page where there's room
        pages = [entity(car, scale, stats, offset)]
        chassis, body, interior = f'{car}_chassis', f'{car}_body', f'{car}_interior'
        if fits(output(chassis)) and fits(output(body)):
            pages.append(craft(chassis, body))
        else:
            pages.append(craft(chassis, None, None, None if fits(output(chassis)) else 'Chassis'))
            pages.append(craft(body, None, None, None if fits(output(body)) else 'Body'))
        tires = 'two Tires' if wheels == 2 else 'four Tires'
        pages.append(craft(interior, None, later + f'Then at the Car Workbench, with {I(engine)} and {tires}.',
                           None if fits(output(interior)) else 'Interior'))
        entry(cat, car, title, f'reikocars:{car}', pages, sort=n)

for n, (trailer, what) in enumerate(TRAILERS.items()):
    w, h, l = TDIMS[trailer]
    scale, offset = scaled(w, h, l)
    pages = [entity(trailer, scale, what, offset),
             craft(f'{trailer}_body', None, f'Then at the Car Workbench, on a {I("trailer_chassis")} with four Tires.',
                   None if fits(output(f'{trailer}_body')) else 'Body')]
    entry('vehicles/trailers', trailer, name(trailer), f'reikocars:{trailer}', pages, sort=n)

entry('vehicles/motorcycles', 'riding', 'Riding Motorcycles', 'reikocars:motorcycle_engine', [
    text('Motorcycles ride like cars (same keys, fuel, locks and pumps), but turn tighter and $(l)lean into turns$(), more the '
         'faster you go, and so do their riders. Parked, a bike stands on its kickstand.$(br2)They\'re built with a '
         '$(item)Motorcycle Engine$() and two tires: the front wheel in the front left slot, the back wheel in the rear left.'),
], sort=-1)


# ---- writing it out ----

FITS = {'patchouli:image': 4, 'patchouli:entity': 3}  # text starts 120 and 115 px down a 156 px page

def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    write(f'{RES}/data/reikocars/patchouli_books/{BOOK}/book.json', {
        'name': 'Car Manual',
        'landing_text': 'Everything about $(l)Reiko\'s Car Mod$(): building and driving cars, keeping them running, and making money '
                        'with them.$(br2)Open a chapter on the right, or search for anything.',
        'subtitle': 'Reiko\'s Car Mod',
        'use_resource_pack': True,
        'model': 'reikocars:car_manual',
        'creative_tab': 'reikocars:cars',
        'show_progress': False,
        'text_overflow_mode': 'resize',
        'index_icon': 'reikocars:sedan',
        'header_color': '7A1F1F',
        'nameplate_color': 'E8C060',
    })
    for cid, title, desc, icon, sort, parent in CATEGORIES:
        data = {'name': title, 'description': desc, 'icon': icon, 'sortnum': sort}
        if parent:
            data['parent'] = 'reikocars:' + parent
        write(f'{OUT}/categories/{cid}.json', data)
    ids = set()
    for cat, eid, title, icon, sort, pages, priority in ENTRIES:
        path = f'{cat}/{eid}'
        assert path not in ids, path
        ids.add(path)
        data = {'name': title, 'icon': icon, 'category': 'reikocars:' + cat, 'sortnum': sort, 'pages': pages}
        if priority:
            data['priority'] = True
        write(f'{OUT}/entries/{path}.json', data)
    # Titles must fit across the page.
    for cid, title, *_ in CATEGORIES:
        assert fits(title), title
    for cat, eid, title, icon, sort, pages, priority in ENTRIES:
        assert fits(title), title
        for page in pages:
            assert fits(page.get('title', '')), page['title']
    # Text under a picture must fit.
    for cat, eid, title, icon, sort, pages, priority in ENTRIES:
        for page in pages:
            room = FITS.get(page['type'])
            if room and lines(page.get('text', '')) > room:
                raise SystemExit(f'{eid}: too much text under the picture: {page["text"]}')
    # Every link must go somewhere.
    for cat, eid, title, icon, sort, pages, priority in ENTRIES:
        for page in pages:
            for link in re.findall(r'\$\(l:([^)]+)\)', page.get('text', '')):
                assert link in ids or link in {c[0] for c in CATEGORIES}, (eid, link)
    # Every spread, for the dev showcase that reads the whole book (./gradlew runClient -Pshowcase=book).
    os.makedirs(f'{REPO}/run', exist_ok=True)
    with open(f'{REPO}/run/guide-spreads.txt', 'w') as f:
        for cat, eid, title, icon, sort, pages, priority in ENTRIES:
            for page in range(0, len(pages), 2):
                f.write(f'{cat}/{eid} {page}\n')
    print(len(CATEGORIES), 'categories,', len(ENTRIES), 'entries,', sum(len(e[5]) for e in ENTRIES), 'pages')
    print('images:', ' '.join(sorted(IMAGES)))


main()
