# Reiko's Car Mod

Drivable cars, motorcycles and semi trucks for Minecraft 1.21.1 (NeoForge) that run on **BuildCraft Reborn**'s fuel,
trailers for the semis to pull, and gas and diesel pumps that sell the fuel for **cash or card** through **Reiko's Economy Mod**.

Refine oil into fuel with BuildCraft, pipe it into a pump, set a price, and other players pay you to fill up.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.264 or newer (Reiko's Economy Mod 1.0.0 itself needs 21.1.265)
- [BuildCraft Reborn](https://github.com/Reiko45/BuildCraft-Reborn) 2.1.0 or newer
- [Reiko's Economy Mod](https://github.com/Reiko45/Reiko-s-Economy-Mod) 1.0.0 or newer
- Optional: [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 19.21 or newer, for the recipe pages below

## The cars

| Car | Seats | Top speed | Tank | Notes |
| --- | --- | --- | --- | --- |
| Sedan | 4 | 40 mph | 8 buckets | All-rounder |
| Sports Car | 2 | 58 mph | 6 buckets | Fast, quick to accelerate, thirsty |
| Pickup Truck | 2 | 36 mph | 12 buckets | 27-slot truck bed (sneak + use) |
| Off-Roader | 4 | 38 mph | 10 buckets | Keeps full speed on grass, sand and snow |
| Hatchback | 4 | 36 mph | 6 buckets | Small and cheap, about 800 blocks to a bucket |
| Muscle Car | 2 | 54 mph | 8 buckets | Big V8: quick off the line, slow to turn, thirsty |
| Convertible | 2 | 51 mph | 6 buckets | Open-top roadster |
| Minivan | 6 | 34 mph | 10 buckets | Three rows of seats, 9-slot trunk (sneak + use) |
| Semi Truck | 2 | 38 mph | 12 buckets of diesel | Runs on diesel and pulls a trailer (see [Semis and trailers](#semis-and-trailers)) |
| Delivery Van | 2 | 34 mph | 10 buckets of diesel | 54-slot cargo area (sneak + use) |
| City Bus | 10 | 29 mph | 16 buckets of diesel | A driver and nine passengers; turns about its back axle, so swing wide |
| Heavy-Duty Pickup | 4 | 38 mph | 14 buckets of diesel | Lifted crew-cab 4x4: 27-slot truck bed, full speed off-road |
| Farm Tractor | 1 | 20 mph | 6 buckets of diesel | Slow, but at home in a field: full speed on grass, dirt and farmland |
| Sport Bike | 2 | 65 mph | 4 buckets | The fastest thing on the road (see [Motorcycles](#motorcycles)) |
| Cruiser | 2 | 45 mph | 5 buckets | 9-slot saddlebags (sneak + use) |
| Dirt Bike | 1 | 42 mph | 3 buckets | Keeps full speed on grass, sand and snow |
| Scooter | 1 | 27 mph | 2 buckets | Slow, but about 2,500 blocks to a bucket |

Tank sizes are in buckets of the car's own fuel: BuildCraft's **light fuel** for the gasoline cars, **dense fuel**
for the diesel ones (the semi, delivery van, city bus, heavy-duty pickup and farm tractor). A sedan goes about 600
blocks on a bucket; a semi about 250, or 180 pulling a trailer; the bus about 300.

**Driving:** right-click a car to get in (the first person in drives). The movement keys drive and steer, **jump**
is the handbrake and **sneak** gets out. **H** sounds the horn, **L** locks or unlocks the car and **G** hitches a
trailer or loads a car onto a car hauler. While you're
in a car, a dashboard in the bottom right shows your speed and a fuel gauge.

- **Fuel:** cars burn fuel for every block they cover, and a little while idling with someone at the wheel. The cars
  run on **gasoline**: BuildCraft's Light Fuel (and the Mixed Light and Gaseous Fuels), or other mods' gasoline,
  ethanol, kerosene and LPG. The semi, delivery van, city bus, heavy-duty pickup and farm tractor run on **diesel**:
  BuildCraft's Dense Fuel and Mixed Heavy Fuels, or other mods' diesel and biodiesel. Neither takes the other's fuel, and nothing runs on crude oil. How far a bucket goes
  comes from BuildCraft's own fuel values. The `reikocars:gasoline` and `reikocars:diesel` fluid tags decide which
  fuel is which, so a data pack can move fluids between them. A car's tooltip says what it runs on.
- **Filling up:** buy gasoline at a gas pump and diesel at a diesel pump, or (if the server allows it) pour a bucket
  of the right fuel straight into the tank.
- **Ownership:** whoever puts a car down owns it. New cars start **locked**, so only the owner can get in, though
  anyone can ride along while the owner is inside. Only the owner (or an admin) can pick a car back up. Punch it a
  few times and it drops as an item, keeping its fuel and paint. A pickup's bed spills its items.
- **Paint:** right-click a car with any dye.
- **Off-road:** grass, dirt, sand, snow and the like (the `reikocars:off_road` block tag) slow every car but the
  off-roader, the heavy-duty pickup, the farm tractor and the dirt bike. Ice is slippery.
- **Running things over** hurts mobs and knocks them aside, more the faster you go. Players only get hurt with PvP
  on, and never your own pets.
- Cars climb a full block, so curbs, slabs and stairs don't stop them. Cars can't be blown up or burned.

## Motorcycles

Four motorcycles ride like the cars (same keys, fuel, locks, paint and pumps), on gasoline:

- **Sport Bike**: the fastest thing in the mod and the quickest off the line, with a seat for a passenger.
- **Cruiser**: long, low and loud, with a passenger seat and **saddlebags**: sneak + use opens their 9 slots.
- **Dirt Bike**: one seat, high and light, and it keeps its speed on grass, sand and snow like the off-roader.
- **Scooter**: one seat, slow, with a tiny tank that lasts ages (about 2,500 blocks a bucket).

They turn tighter than cars and **lean into turns**, more the faster you go, and the rider and passenger lean with
them. In first person the view tilts a little too. Parked with nobody on, a bike stands over on its kickstand.

They're built at the Car Workbench like the cars, from their own **frame**, **bodywork** and **seat**, a **Motorcycle
Engine** and **two** tires: the front wheel goes in the front left slot and the back wheel in the rear left one.

## Semis and trailers

The **Semi Truck** pulls one trailer at a time. There are three:

| Trailer | Carries |
| --- | --- |
| **Box Trailer** | 54 slots of cargo. Right-click it to open it, like a double chest |
| **Tanker Trailer** | 64 buckets of any one fluid: fuel, oil, water, lava, other mods' fluids |
| **Car Hauler** | 2 cars, one behind the other |

**Building a trailer:** trailers are put together at the Car Workbench too, from the **Trailer Chassis** (which
every trailer takes), the trailer's own body (the **Box Trailer Body**, **Tanker Trailer Tank** or **Car Hauler
Deck**) and four **Tires**. They take no engine or interior, so those two slots are shaded for them.

**Putting a trailer down:** use the trailer item on the ground. It stands on its landing legs facing the way you
look, so face the way you'll drive off. Whoever puts it down owns it.

**Hitching:** back the semi up under the trailer's front, so the fifth wheel (the plate behind the cab) is under
the trailer's nose and the two are roughly in line (within 45°), then press **G**. The dashboard shows
**[G] Hitch trailer** when you're close enough. Press G again to drop it off where it stands.

**Driving a rig:** the trailer swings round behind you on bends and cuts the corner, as a real one does, and it can't
fold back on itself past 100°. The semi turns about its back wheels, so take corners wide. Pulling a trailer, the
semi is a little slower to get going and burns 40% more fuel. In third person the camera stands far enough back to
see the whole rig. Trailers aren't solid: you can walk through them and they never snag the semi.

**The tanker:** fill or empty it with a bucket (or any fluid container) by right-clicking it. To move fuel in bulk,
park it with a tank beside it (within 2 blocks of its sides or ends) and right-click it with an empty hand: **Load**
takes from every tank, cauldron or machine nearby that will give up its fluid, and **Unload** fills everything
nearby that will take it, nearest first. So a tanker can fill up at a BuildCraft refinery's tanks and unload into a
row of pumps. Pumps only take their own kind of fuel, and never give any back, so you can't load from someone's pump.
A picked-up tanker keeps its load.

**The car hauler:** it carries cars and motorcycles up to a minivan's size, but not trucks, vans, buses or tractors.
Drive a car up to the hauler's back end, lined up with it, and press **G**: the car drives on
and stays on when you get out. With both spots full, nothing more goes on. Get in a car on the hauler and press **G**
to drive it off the back. Hitch the hauler to a semi and its cars go wherever it goes.

**Ownership and paint:** as with cars. **Sneak + right-click** with an empty hand locks or unlocks a trailer (only
its owner can), and a locked trailer can only be opened, hitched or loaded by its owner. Right-click with a dye to
paint it. Punch it to pick it up. A box trailer spills its cargo.

## Building a car

Cars and trailers are put together at a **Car Workbench**. Each car has three parts of its own (a **chassis**, a
**body** and an **interior**), and they all take four **Tires** and an engine: a **Car Engine**, or a **Diesel
Engine** for the semi. A trailer takes the Trailer Chassis, its own body and four tires.

1. Craft the parts (see [Recipes](#recipes)) and place a Car Workbench.
2. Right-click the workbench. The buttons along the top are the cars it can build. Hover over one to see which parts
   you have, and click it to put all the parts you carry into place. You can also drop parts in by hand or shift-click
   them; a stack of tires fills all four wheels.
3. The slots are laid out like the car seen from above: a wheel at each corner, the engine at the front, the chassis
   in the middle with the body and interior either side. Empty slots show the part they need.
4. A 3D model of the car fills in as the parts go in: anything not fitted yet shows as a see-through blue ghost. Drag
   it to turn it.
5. When every part is in, take the car from the slot on the right. A green tick on a car's button means you carry
   everything for it.

Parts left in the workbench go back to you when you close it.

### With JEI

If JEI is installed, it gets a **Car Workbench** recipe page for each car and trailer: the parts laid out as in the
workbench, the vehicle turning in 3D, and a car's seats, top speed and tank size, or what a trailer carries. Look up a car's recipe (R), or what a part, tire or engine
is used for (U). Clicking the arrow in the workbench's screen lists every car. With the workbench open, JEI's **+**
button puts that car's parts in from your inventory, the same as clicking the car's button; if you're missing any,
the button is greyed out and JEI marks the missing parts in red.

## Gas and diesel pumps

There are two pumps, which work the same way:

- The **Gas Pump** (red, "GAS" on its sign) sells gasoline.
- The **Diesel Pump** (green, "DIESEL" on its sign) sells diesel: BuildCraft's Dense Fuel or Mixed Heavy Fuels, or
  other mods' diesel and biodiesel. It fills semis, and sells diesel by the bucket for BuildCraft's combustion
  engines and other mods' generators.

A pump is two blocks tall. Whoever places it owns it, and only they (or an admin) can break it. A broken pump keeps
its fuel and price on the item.

**Getting fuel in:** connect any pipe (BuildCraft's or another mod's) to either half of the pump, from any side.
The pump holds 16 buckets of its own kind of fuel, one fluid at a time. Nothing can be piped back out, so the only
way out is to sell it. The owner can also fill it from a bucket, and anyone can unload a tanker into it.

**Buying fuel:** right-click the pump. The screen shows the fuel in the pump and its price. A pump fills either the
car at the hose (within 6 blocks) or the **empty buckets** you carry. The little button at the start of the car row
switches between them, showing a car or a bucket. The screen starts on the car if one that runs on this fuel is at
the hose, and on buckets otherwise. Choose an amount (or **Fill up**), then pay:

- **Card:** the card you carry (the ⇄ button switches if you have several), its PIN, then **Card**. A debit card
  pays from its bank account; a credit card puts it on the bill.
- **Cash:** **Cash** pays with the coins and bills you carry, or from your wallet, and gives change.

This is the economy mod's own shop payment, so sales tax applies and the money goes straight into the owner's
bank account, even while they're offline. The pump's owner fills up for free, so they can also take fuel back out
of their own pump in buckets.

**Setting the price:** the owner sees a price row at the bottom of the screen. Type a price per bucket and press
**Set**. The lit display on the front of the pump shows the price, or EMPTY.

## Recipes

| Item | Recipe |
| --- | --- |
| Car Workbench | iron ingots on top, iron gear, crafting table, iron gear across the middle, iron legs |
| Tire ×4 | dried kelp in the corners, black dye on the sides, centre empty |
| Car Engine | iron ingots, iron gears, pistons around a BuildCraft **Combustion Engine** |
| Gas Pump | iron ingots, glass pane, BuildCraft **Tank**, iron gear |
| Diesel Pump | as the Gas Pump, with green dye either side of the iron gear |
| Diesel Engine | as the Car Engine, with an iron block in place of the top gear |
| Motorcycle Engine | iron gear on top, piston, BuildCraft **Combustion Engine**, piston across the middle, iron legs |
| Trailer Chassis | iron ingots on top, iron gears in the outer middle slots, iron legs |
| Box Trailer Body | iron ingots top and bottom, chests in the outer middle slots |
| Tanker Trailer Tank | four BuildCraft **Tanks** around an empty middle, iron ingots in the corners |
| Car Hauler Deck | iron bars in the corners, iron ingots across the middle |

The car parts, crafted on an ordinary crafting table:

| Part | Sedan | Sports Car | Pickup Truck | Off-Roader | Semi Truck |
| --- | --- | --- | --- | --- | --- |
| **Chassis**: iron rails (`I I`) around a middle | iron gear | gold gear | iron block | iron gear between two pistons | iron gear between two iron blocks |
| **Body**: glass panes on top, iron below, a dye in the middle | blue dye | red dye, gold ingots beside it | white dye, chest at the bottom | yellow dye, iron bars beside it | green dye, iron block at the bottom (the cab) |
| **Interior**: a dashboard on top, redstone in the middle, seats below | clock, wool seats | clock, leather seats, gold trim | clock, leather bench | compass, wool and leather | clock, leather, a bed (the sleeper) |

Then at the Car Workbench: chassis + body + interior + engine + 4 Tires = the car. The semi takes a Diesel Engine;
the rest take a Car Engine. A trailer is the Trailer Chassis + its body + 4 Tires.

The second lot of cars' parts, the same way:

| Part | Hatchback | Muscle Car | Convertible | Minivan |
| --- | --- | --- | --- | --- |
| **Chassis**: iron rails (`I I`) around a middle | copper ingot | piston | gold ingot | iron bars |
| **Body** | glass panes over lime dye and iron | two glass panes over orange dye and iron | a glass pane over cyan dye and iron | glass all round light grey dye, iron below |
| **Interior** | clock, redstone, wool seats | clock, redstone, leather seats | clock, redstone, leather and wool | clock, two rows of wool and redstone |

| Part | Delivery Van | City Bus | Heavy-Duty Pickup | Farm Tractor |
| --- | --- | --- | --- | --- |
| **Chassis** | iron rails, copper between iron blocks | iron rows, iron gear between iron blocks | iron rails, iron block between pistons | iron rails, iron block between iron gears |
| **Body** | glass and white dye, iron | glass all round yellow dye, iron blocks below | glass, red dye, iron blocks and a chest | iron frame round green dye |
| **Interior** | clock, redstone, leather, iron below | clock and redstone in wool | clock, redstone, leather, iron | compass, redstone, leather, iron |

The gasoline cars take a Car Engine, the diesel ones a Diesel Engine.

The motorcycles' parts:

| Part | Sport Bike | Cruiser | Dirt Bike | Scooter |
| --- | --- | --- | --- | --- |
| **Frame**: iron ingots in a diagonal (`I  `/`I?I`/`  I`) around a middle | gold gear | iron block | piston | iron gear |
| **Bodywork** | glass pane over iron and blue dye | black dye between iron, leather below | yellow dye either side over three iron | iron around cyan dye |
| **Seat**: a clock or compass on top over three of leather or wool | clock, leather, iron stalk | clock, leather, leather legs | compass, wool, iron stalk | clock, wool, iron legs |

At the Car Workbench: frame + bodywork + seat + Motorcycle Engine + 2 Tires = the motorcycle.

The workbench recipes are data files (`data/reikocars/recipe/sedan.json` and so on, type `reikocars:car_assembly`),
so a data pack can change a car's parts or add new vehicles. Each one names a `chassis`, `body`, `interior`, `engine`
and `wheel` (an item or a tag) and a `result`. The `interior` and `engine` can be left out, as the trailers'
recipes do, and then those slots must be empty. `"wheels": 2` makes it a two-wheeler, as the motorcycles are.

## Settings

Server settings are in `serverconfig/reikocars-server.toml` in the world folder:

| Setting | Default | What it does |
| --- | --- | --- |
| `fuel.consumptionMultiplier` | `1.0` | Fuel burned per block. 0.5 doubles every car's range |
| `fuel.idleBurnPerTick` | `0.5` | MJ burned each tick with a driver aboard |
| `fuel.allowBucketFueling` | `true` | Pour fuel buckets straight into cars |
| `fuel.fallbackEnergyPerBucket` | `90000` | MJ per bucket for a tagged fuel BuildCraft doesn't know |
| `driving.speedMultiplier` | `1.0` | Top speed and acceleration |
| `driving.offRoadSpeedFactor` | `0.6` | Top speed off-road (1.0 turns it off) |
| `driving.runOverDamage` | `6.0` | Damage per block-per-tick of speed (0 turns it off) |
| `driving.lockNewCars` | `true` | New cars start locked |
| `pumps.defaultPricePerBucket` | `4.0` | Starting price of a new pump |
| `pumps.capacityBuckets` | `16` | Buckets a pump holds |
| `pumps.hoseLength` | `6.0` | How close a car must be to a pump |

Each player can pick **mph** or **km/h**, and turn the dashboard off, in `config/reikocars-client.toml`.

## Building and testing

You need JDK 21. The two mods it builds on are in `libs/`: replace those jars to build against newer versions
(and update the names in `gradle.properties`). JEI comes from its Maven repository: the build compiles against
the API of `jei_version` in `gradle.properties` (19.21.2.313, so the plugin works with that and every newer 1.21.1
JEI) and puts JEI in the dev runs. `-Pjei_version=...` tries another version.

```sh
./gradlew build             # build the jar into build/libs/
./gradlew runGameTestServer # run the in-game tests (payments, pumps, pipes, fuel, diesel, locks, the workbench, semis, trailers, motorcycles), then exit
./gradlew runClient         # start a dev client
./gradlew runClient -Pshowcase  # build a test scene, buy fuel, drive, save screenshots to run/screenshots/, quit
./gradlew runClient -Pshowcase=workbench  # the same for the Car Workbench and its JEI pages
./gradlew runClient -Pshowcase=diesel     # the same for the gas and diesel pumps and selling buckets
./gradlew runClient -Pshowcase=semi       # the same for semis, hitching, driving a rig, trailers and the tanker
./gradlew runClient -Pshowcase=bikes      # the same for the motorcycles: parked, riding and leaning, saddlebags
./gradlew runClient -Pshowcase=cars2      # the same for the hatchback, muscle car, convertible, minivan, van, bus, 4x4, tractor
```

## Editing the models

The cars are Blockbench projects in `blockbench/` (format: Modded Entity, box UV). Each has the same bones, which
the mod relies on:

- `body`: the paintwork. Paint it light grey: the game tints it the car's colour.
- `trim`: glass (may be see-through), seats, bumpers, grille and so on, drawn as painted.
- `lights`: head- and taillights, which glow while the engine runs.
- `wheel_fl`, `wheel_fr`, `wheel_rl`, `wheel_rr`: each pivoting on its axle. They roll, and the front pair steers.
- `wheel_rl2`, `wheel_rr2` (the semi only): the second rear axle, which rolls with the first.

A motorcycle has `body`, `trim`, `lights`, `wheel_rl` (its back wheel) and a `fork`, which steers and holds the
forks, handlebars and `wheel_fl` (its front wheel) inside it. It has no right-hand wheels.

The trailers (`box_trailer`, `tanker_trailer`, `car_hauler`) work the same way, with `body`, `trim`, `lights` and
two axles of wheels, `wheel_l1`, `wheel_r1`, `wheel_l2` and `wheel_r2`. Their textures are in
`textures/entity/trailer/`. The front faces north, and the kingpin (which sits on the semi's fifth wheel) must stay
56 pixels ahead of the middle and 18 up, as set in `trailer/TrailerType.java`.

The car's front faces north (−Z), and the driver's side is **−X** in Blockbench. To change a car, edit its
`.bbmodel`, save the texture over `src/main/resources/assets/reikocars/textures/entity/car/<car>.png`, use
**File > Export > Export Java Entity**, and paste the exported `createBodyLayer()` into
`client/model/<Car>Model.java`. If you move the seats, update the seat table at the top of `car/CarType.java`.

## Project layout

```text
src/main/java/com/reiko/cars/
├── ReikoCars.java        mod entry point
├── car/                  the car entity and its types, fuel (CarFuel, CarTank), the car item
├── pump/                 the gas and diesel pump block, its block entity, menu, protection, and PumpService (selling fuel)
├── trailer/              the trailers, their item, hitching and car hauling (Hitching), and the tanker (TankerService)
├── workbench/            the Car Workbench block and menu, the car parts, and the car_assembly recipe type
├── compat/jei/           the JEI plugin (only loaded when JEI is installed)
├── network/              packets: pump actions, horn, lock and hitch, tanker load and unload
├── registry/             blocks, items, entities, menus, recipes, sounds, data components, capabilities, creative tab
├── config/               server settings
├── client/               car and trailer renderers and models, dashboard, pump, workbench and tanker screens, pump display, engine sound, keys
├── gametest/             in-game tests
└── dev/                  the showcase script (dev only, not in the jar)
blockbench/               the car, motorcycle, semi and trailer models
```

## License

GPL-3.0, like Reiko's Economy Mod. See [LICENSE](LICENSE).
