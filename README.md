# Reiko's Car Mod

Drivable cars, motorcycles, semi trucks, tow trucks and a mail van for Minecraft 1.21.1 (NeoForge) that run on
**BuildCraft Reborn**'s fuel, trailers for the semis to pull, and gas and diesel pumps that sell the fuel for **cash or
card** through **Reiko's Economy Mod**.

Refine oil into fuel with BuildCraft, pipe it into a pump, set a price, and other players pay you to fill up.
Cars wear out as they're driven, and a **Repair Bench** fixes them: fit your own parts, or run a mechanic's shop and
charge for repairs. Tow trucks haul broken-down cars to it, and mailboxes and the mail van carry parcels between
players for postage.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.264 or newer (Reiko's Economy Mod 1.0.0 itself needs 21.1.265)
- [BuildCraft Reborn](https://github.com/Reiko45/BuildCraft-Reborn) 2.1.0 or newer
- [Reiko's Economy Mod](https://github.com/Reiko45/Reiko-s-Economy-Mod) 1.0.0 or newer
- Optional: [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 19.21 or newer, for the recipe pages below

## The cars

| Car | Seats | Top speed | Tank | Notes |
| --- | --- | --- | --- | --- |
| Sedan | 4 | 40 mph | 8 buckets | All-rounder |
| Taxi | 4 | 40 mph | 8 buckets | Runs a fare meter (see [Fares](#fares)); lit TAXI sign |
| Sports Car | 2 | 58 mph | 6 buckets | Fast, quick to accelerate, thirsty |
| Pickup Truck | 2 | 36 mph | 12 buckets | 27-slot truck bed (sneak + use) |
| Off-Roader | 4 | 38 mph | 10 buckets | Keeps full speed on grass, sand and snow |
| Hatchback | 4 | 36 mph | 6 buckets | Small and cheap, about 800 blocks to a bucket |
| Muscle Car | 2 | 54 mph | 8 buckets | Big V8: quick off the line, slow to turn, thirsty |
| Convertible | 2 | 51 mph | 6 buckets | Open-top roadster |
| Minivan | 6 | 34 mph | 10 buckets | Three rows of seats, 9-slot trunk (sneak + use) |
| Semi Truck | 2 | 38 mph | 12 buckets of diesel | Runs on diesel and pulls a trailer (see [Semis and trailers](#semis-and-trailers)) |
| Delivery Van | 2 | 34 mph | 10 buckets of diesel | 54-slot cargo area (sneak + use) |
| City Bus | 10 | 29 mph | 16 buckets of diesel | A driver and nine passengers, who pay a fare (see [Fares](#fares)); turns about its back axle, so swing wide |
| Heavy-Duty Pickup | 4 | 38 mph | 14 buckets of diesel | Lifted crew-cab 4x4: 27-slot truck bed, full speed off-road |
| Farm Tractor | 1 | 20 mph | 6 buckets of diesel | Slow, but at home in a field: full speed on grass, dirt and farmland |
| Flatbed Tow Truck | 2 | 36 mph | 14 buckets of diesel | Carries a car on its deck (see [Tow trucks](#tow-trucks)); 9-slot toolboxes |
| Wrecker Tow Truck | 2 | 38 mph | 12 buckets of diesel | Tows a car on its wheel-lift by the front wheels; 18-slot toolboxes |
| Mail Van | 2 | 35 mph | 9 buckets | Right-hand drive; collects and delivers parcels (see [Mail](#mail)); 36-slot mail compartment |
| Sport Bike | 2 | 65 mph | 4 buckets | The fastest thing on the road (see [Motorcycles](#motorcycles)) |
| Cruiser | 2 | 45 mph | 5 buckets | 9-slot saddlebags (sneak + use) |
| Dirt Bike | 1 | 42 mph | 3 buckets | Keeps full speed on grass, sand and snow |
| Scooter | 1 | 27 mph | 2 buckets | Slow, but about 2,500 blocks to a bucket |

Tank sizes are in buckets of the car's own fuel: BuildCraft's **light fuel** for the gasoline cars, **dense fuel**
for the diesel ones (the semi, delivery van, city bus, heavy-duty pickup, farm tractor and tow trucks). A sedan goes about 600
blocks on a bucket; a semi about 250, or 180 pulling a trailer; the bus about 300.

**Driving:** right-click a car to get in (the first person in drives), with one of its **keys** in your inventory
(see [Car keys](#car-keys)). The movement keys drive and steer, **jump**
is the handbrake and **sneak** gets out. **H** sounds the horn, **L** locks or unlocks the car and **G** hitches a
trailer, loads a car onto a car hauler, picks up or drops a car in a tow truck, or stops the mail van for the mail. While you're
in a car, a dashboard in the bottom right shows your speed and a fuel gauge.

- **Fuel:** cars burn fuel for every block they cover, and a little while idling with someone at the wheel. The cars
  run on **gasoline**: BuildCraft's Light Fuel (and the Mixed Light and Gaseous Fuels), or other mods' gasoline,
  ethanol, kerosene and LPG. The semi, delivery van, city bus, heavy-duty pickup, farm tractor and tow trucks run on **diesel**:
  BuildCraft's Dense Fuel and Mixed Heavy Fuels, or other mods' diesel and biodiesel. Neither takes the other's fuel, and nothing runs on crude oil. How far a bucket goes
  comes from BuildCraft's own fuel values. The `reikocars:gasoline` and `reikocars:diesel` fluid tags decide which
  fuel is which, so a data pack can move fluids between them. A car's tooltip says what it runs on.
- **Filling up:** buy gasoline at a gas pump and diesel at a diesel pump, or (if the server allows it) pour a bucket
  of the right fuel straight into the tank.
- **Ownership:** whoever puts a car down owns it. New cars start **locked**, so only the owner and anyone with its
  key can get in, though anyone can ride along with them. Only the owner (or an admin) can pick a car back up. Punch
  it a few times and it drops as an item, keeping its fuel, paint and locks. A pickup's bed spills its items.
- **Paint:** right-click a car with any dye.
- **Off-road:** grass, dirt, sand, snow and the like (the `reikocars:off_road` block tag) slow every car but the
  off-roader, the heavy-duty pickup, the farm tractor and the dirt bike. Ice is slippery.
- **Running things over** hurts mobs and knocks them aside, more the faster you go. Players only get hurt with PvP
  on, and never your own pets.
- Cars climb a full block, so curbs, slabs and stairs don't stop them. Cars can't be blown up or burned.

## Car keys

Every car, motorcycle and truck needs one of its keys to start, **even for its owner**. With no key in your inventory
the engine won't turn over and the dashboard says **NO KEY**.

- **Cutting keys:** craft **Blank Keys** (an iron ingot and a gold nugget make two), then **sneak + right-click**
  your car with one to cut a key for it. Cut as many as you like and hand them out. Only the owner (or an admin) can
  cut keys. A key is named for its car ("Sedan Key"), its fob is in the car's paint, and its tooltip names the owner.
- **What a key does:** anyone carrying it can start and drive the car, get into it while it's locked, open its trunk
  or bed, and lock or unlock it with **L**. They can't pick it up or cut more keys.
- **Key fob:** right-click with a key to lock or unlock its car from up to 32 blocks away. The lights flash.
- **Lost a key?** Changing the locks makes every other key stop fitting: the owner **sneak + right-clicks** the car
  twice with one of its keys, and only that key fits from then on. (Lost every key? Cut a new one from a blank first.)
- Picking a car up keeps its locks, so its keys still fit when you put it down again.
- **Trailers** take keys too, cut the same way: a key lets someone open, hitch and lock a locked trailer.

A server can set `driving.requireKeys` to `false`, so cars start without keys and keys only share locked cars.
Cars from before 1.12.0 have no keys yet: cut one before driving.

## Fares

The **city bus** and the **taxi** take fares from their passengers, paid to whoever's driving. The vehicle's owner
rides free.

- **The bus:** sit down while someone's driving and a pay screen opens: the fare (plus sales tax), by **Card** with
  PIN or **Cash**. Close it or press **Get off** and you're off the bus. Each time you get on is a new ride.
- **The taxi:** get in while someone's driving and you've hired it. The **meter** starts at a starting fare and adds
  so much for every 100 blocks driven; the dashboard shows it (**FARE $3.40**). When you get out, the fare comes out
  of your **bank account** and goes to the driver. Get out within 5 blocks and it's free. If you can't cover it all,
  you pay what you have. Other passengers ride along on the same hire.
- **Setting fares:** the owner **sneak + right-clicks** the bus or taxi with an empty hand: a fare per ride for the
  bus, or a starting fare and a rate per 100 blocks for the taxi, and what it's earned so far. 0 makes rides free.
  Anyone else doing that is told the fares.

New buses charge $2.00 a ride, and new taxis $3.00 plus $1.00 per 100 blocks (see [Settings](#settings)).

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

**Ownership and paint:** as with cars. **Sneak + right-click** with an empty hand locks or unlocks a trailer (its
owner, or anyone with its key), and a locked trailer can only be opened, hitched or loaded by its owner or someone
with its key. Right-click with a dye to
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

## Wear and repairs

Cars wear out like real ones. Each car has five parts, each from 100% (new) down to 0%:

| Part | Wears from | When it's worn |
| --- | --- | --- |
| **Oil** | distance: an oil change every 50,000 blocks or so | Below 25% the engine wears four times as fast, and twenty times as fast with none |
| **Engine** | distance, slowly (about 400,000 blocks), and hard crashes | Below 50%, less power: slower and slower to pick up speed. Any wear burns more fuel, up to half as much again |
| **Tires** | distance (80,000 blocks on a road), faster off-road and towing, and hard braking and handbrake skids | Below 30%, less grip: worse acceleration, braking and cornering |
| **Brakes** | every stop, by how fast you were going (about 550 hard stops) | Below 40%, the car takes longer to stop. Worn pads squeal |
| **Body** | crashes, by how hard you hit (a wall, another car or a mob) | Below 50%, slower. Bumps under about 7 mph leave no mark |

When any part reaches **0%, the car breaks down**: the engine won't start or run until it's repaired (the brakes
still work). Cars are never destroyed.

**Trailers** wear too, though they have no oil or engine: their **tires** wear as they're towed (faster loaded or
off-road), their **brakes** whenever the semi brakes, and their **body** when the rig crashes (the load shoves the
trailer into the cab) or jackknifes (backing up until it folds against the cab). Worn trailer brakes and tires make
the whole rig slower to stop, and a battered trailer slows it down. A trailer with a part at 0% can't be towed: the
rig breaks down until you unhitch it (G), and it won't hitch again until it's fixed, so put a repair bench down
beside it (or pick it up and set it down by one).

**Seeing it:** the dashboard has five warning lights (oil can, engine, tire, brake, car). They come on amber when a
part is worn (25%), orange when badly worn (10%), and flash red when it has given out, with **BROKEN DOWN** on
the dashboard. Towing, a second row shows the trailer's tires, brakes and body. A tired engine smokes from under the bonnet, and the action bar tells the driver as each part gets
low. A car's or trailer's item tooltip shows its condition, and one picked up keeps its wear.

**The Repair Bench:** park within 6 blocks of it and right-click it. It works on any car or trailer there, locked
or not (a repair only helps its owner, and whoever orders it pays). With several beside the bench, the **< >**
arrows pick which; it starts on the one you're in, or else the nearest you can use. It shows the vehicle part by
part, with two ways to fix each:

- **Fit:** fits the parts you carry, for nothing. An oil change takes a Motor Oil, tires a full set (one per
  wheel; four for a trailer), brakes a set of Brake Pads (diesel trucks and trailers take two of each). Engine Parts
  each put back a quarter of the engine and Body Panels a fifth of the body (half that on diesel trucks and
  trailers), so you can fix them a bit at a time.
- **The price button:** has the bench fit parts from its own stock, at the owner's prices. Click the jobs you want,
  then pay the total by **Card** (with PIN) or **Cash**, as at a pump. Sales tax applies, and the money goes to the
  bench's owner.

Whoever places the bench owns it, and only they (or an admin) can break it. The owner gets two more tabs: **Stock**,
to load the bench with parts (hoppers and pipes can fill it too, but nothing comes back out), and **Prices**, to set
the price of each part. The owner repairs from their own stock for free, so a bench works as a garage at home or a
mechanic's shop for other players.

## Tow trucks

Two diesel tow trucks fetch cars that have broken down (or anything else that needs moving):

- **The flatbed** carries the car on its deck. Back up until the car's front or back is at the tail of the deck and
  press **G**: the car is winched up and rides on the deck facing forward. **G** again rolls it off behind.
- **The wrecker** lifts the car by its front wheels on its wheel-lift, the back wheels still on the road. Back the
  wheel-lift up to the car's front (the car facing the same way as the truck) and press **G**. The car swings out
  behind on bends, as a trailer does. **G** again lets it down where it is.

Either truck takes any car, van or pickup up to about 4.8 blocks long, broken down or not, as long as nobody is in
it and it's yours or unlocked. The flatbed carries motorcycles too; neither takes semis, buses, tractors or another
tow truck. The dashboard shows **[G] Tow car** when there's one to pick up. A towed car doesn't run, burn fuel or
wear, and goes wherever the truck goes, saved with it. A loaded tow truck is a little slower and its tires and
brakes wear faster.

## Mail

**Mailboxes** send parcels between players. Whoever puts one up owns it (only they or an admin can break it), and
anyone can right-click it:

- **Send:** type who it's for, put up to nine stacks in the slots, and send. If the mailbox is the addressee's own,
  the parcel goes straight in, free. Otherwise you pay **postage** ($2.00 plus sales tax, by **Card** with PIN or
  **Cash**), the parcel waits in the mailbox for the mail van, and the mailbox's **flag goes up**. The addressee
  needs a mailbox of their own somewhere.
- **Inbox** (the owner's tab): the parcels that have come for you. Right-click a parcel to open it. Only its
  addressee can, or its sender if it comes back. A parcel's tooltip says who it's for, who it's from and where it's
  going. You get a chat message when mail arrives while you're on.

**The mail van** carries the mail. Drive up beside a mailbox (within 3 blocks) and press **G**: the van collects any
parcels waiting there into its mail compartment, and delivers the ones it's carrying for that mailbox's owner. The
driver **earns each parcel's postage** on delivery, so running a mail route pays. A parcel whose addressee no longer
has a mailbox goes back to the sender's mailbox instead. The van is right-hand drive, so the mailboxes are on the
driver's side.

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
| Repair Bench | iron ingot, anvil, iron ingot on top, iron gear, chest, iron gear across the middle, iron legs |
| Mailbox | iron ingot, blue dye, red dye on top, iron ingot, chest, iron ingot across the middle, a wooden fence below |
| Blank Key ×2 | an iron ingot and a gold nugget (shapeless) |
| Motor Oil ×3 | a BuildCraft **Oil** bucket and three glass bottles (shapeless) |
| Engine Parts ×2 | iron ingot, piston, iron ingot over iron gear, iron ingot, iron gear |
| Brake Pads ×2 | three iron ingots over three flint |
| Body Panel ×2 | four iron ingots in a square |

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

The taxi's parts: **Chassis**, iron rails (`I I`) around a compass; **Body**, glass panes either side of glowstone
dust (the roof sign) over yellow dye between iron, iron below; **Interior**, a clock over redstone between wool, a
glass pane (the partition) between wool below. It takes a Car Engine.

| Part | Flatbed Tow Truck | Wrecker Tow Truck | Mail Van |
| --- | --- | --- | --- |
| **Chassis**: iron rails (`I I`) around a middle | chain between iron blocks | tripwire hook between iron blocks | piston between iron blocks |
| **Body** | glass panes and iron over red dye, iron trapdoors below (the deck) | glass panes and a chain over yellow dye, iron and a hook below | glass all along over blue dye between white dye, iron below |
| **Interior** | clock, redstone between leather, a lever between iron | clock, redstone between leather, a chain between iron | clock, redstone between wool, paper between iron |

The tow trucks take a Diesel Engine, the mail van a Car Engine.

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
| `driving.requireKeys` | `true` | A car needs one of its keys to start, even for its owner. If false, keys only share locked cars |
| `pumps.defaultPricePerBucket` | `4.0` | Starting price of a new pump |
| `pumps.capacityBuckets` | `16` | Buckets a pump holds |
| `pumps.hoseLength` | `6.0` | How close a car must be to a pump |
| `wear.wearMultiplier` | `1.0` | How fast oil, engines, tires and brakes wear (0 turns wear off) |
| `wear.crashDamageMultiplier` | `1.0` | Body damage from crashes (0 turns it off) |
| `wear.breakdowns` | `true` | A part at 0% breaks the car down. If false, worn parts only make it drive worse |
| `wear.repairBenchReach` | `6.0` | How close a car must be to a repair bench |
| `wear.defaultPricePerPart` | `20.0` | Starting price of each part at a new repair bench |
| `mail.postage` | `2.0` | Postage for a parcel sent by mail van, earned by whoever delivers it (hand delivery is always free) |
| `mail.mailVanReach` | `3.0` | How close the mail van must be to a mailbox |
| `fares.busFare` | `2.0` | The fare a new city bus charges each passenger (its owner can change it) |
| `fares.taxiStartingFare` | `3.0` | What a new taxi's meter starts at |
| `fares.taxiFarePer100Blocks` | `1.0` | What a new taxi's meter adds per 100 blocks |

Each player can pick **mph** or **km/h**, and turn the dashboard off, in `config/reikocars-client.toml`.

## Building and testing

You need JDK 21. The two mods it builds on are in `libs/`: replace those jars to build against newer versions
(and update the names in `gradle.properties`). JEI comes from its Maven repository: the build compiles against
the API of `jei_version` in `gradle.properties` (19.21.2.313, so the plugin works with that and every newer 1.21.1
JEI) and puts JEI in the dev runs. `-Pjei_version=...` tries another version.

```sh
./gradlew build             # build the jar into build/libs/
./gradlew runGameTestServer # run the in-game tests (payments, pumps, pipes, fuel, diesel, locks, the workbench, semis, trailers, motorcycles, wear and repairs, tow trucks, mail, keys, fares), then exit
./gradlew runClient         # start a dev client
./gradlew runClient -Pshowcase  # build a test scene, buy fuel, drive, save screenshots to run/screenshots/, quit
./gradlew runClient -Pshowcase=workbench  # the same for the Car Workbench and its JEI pages
./gradlew runClient -Pshowcase=diesel     # the same for the gas and diesel pumps and selling buckets
./gradlew runClient -Pshowcase=semi       # the same for semis, hitching, driving a rig, trailers and the tanker
./gradlew runClient -Pshowcase=bikes      # the same for the motorcycles: parked, riding and leaning, saddlebags
./gradlew runClient -Pshowcase=cars2      # the same for the hatchback, muscle car, convertible, minivan, van, bus, 4x4, tractor
./gradlew runClient -Pshowcase=repair     # the Repair Bench, paying for repairs, its owner's tabs, a worn car's dashboard and smoke, a worn trailer
./gradlew runClient -Pshowcase=tow        # the tow trucks loaded and towing, posting a parcel, the mail van's round, the inbox
./gradlew runClient -Pshowcase=keys       # no key, cutting one, its tooltip, driving with it, locking it from a distance
./gradlew runClient -Pshowcase=fares      # paying a bus fare, a taxi's meter running and paid, the owner's fare screen
./gradlew runClient -Pshowcase=gallery    # every model from four sides (or -Pshowcase=gallery:sedan,box_trailer,pumps)
```

## Editing the models

The cars are Blockbench projects in `blockbench/` (format: Modded Entity, box UV), built at Minecraft's own scale:
one texture pixel to one model pixel. They have real wheel arches with dark wells, tyres with recessed rims and a
hub, raked windscreens and rear windows, furnished cabins (seats, dashboards with lit gauges, steering wheels, door
panels), and detailed lights, grilles, bumpers, mirrors and trim. Each has the same top-level bones, which the mod
relies on:

- `body`: the paintwork. Paint it light grey: the game tints it the car's colour.
- `trim`: glass (may be see-through), seats, bumpers, grille and so on, drawn as painted.
- `lights`: head- and taillights, which glow while the engine runs.
- `wheel_fl`, `wheel_fr`, `wheel_rl`, `wheel_rr`: each pivoting on its axle. They roll, and the front pair steers.
- `wheel_rl2`, `wheel_rr2` (the semi only): the second rear axle, which rolls with the first.

The tow trucks' decks and wheel-lift are part of `trim`, so a towed car must sit where the code puts it: the
flatbed's deck top 20 pixels up, running from 4 pixels ahead of the middle to 59 behind, and the wrecker's forks 61
pixels behind the middle (see `tow/Towing.java`). The wrecker's boom is a child bone tilted up from its pivot.

A motorcycle has `body`, `trim`, `lights`, `wheel_rl` (its back wheel) and a `fork`, which steers and holds the
forks, handlebars and `wheel_fl` (its front wheel) inside it. It has no right-hand wheels.

The trailers (`box_trailer`, `tanker_trailer`, `car_hauler`) work the same way, with `body`, `trim`, `lights` and
two axles of wheels, `wheel_l1`, `wheel_r1`, `wheel_l2` and `wheel_r2`. Their textures are in
`textures/entity/trailer/`. The front faces north, and the kingpin (which sits on the semi's fifth wheel) must stay
56 pixels ahead of the middle and 18 up, as set in `trailer/TrailerType.java`.

These bones can hold child bones of their own, turned at an angle: the windscreens, rear windows and pillars, the
steering wheels, a motorcycle's raked fork legs (`fork_legs`, inside `fork`, so they steer with it), the semi's roof
fairing, the tanker's tank. A child is drawn the way its parent is (tinted under `body`, lit under `lights`). Some
textures aren't square (512×256, say); Blockbench and the game both take that.

The car's front faces north (−Z), and the driver's side is **−X** in Blockbench. To change a car, edit its
`.bbmodel`, save the texture over `src/main/resources/assets/reikocars/textures/entity/car/<car>.png`, use
**File > Export > Export Java Entity**, and paste the exported `createBodyLayer()` into
`client/model/<Car>Model.java`. If you move the seats, update the seat table at the top of `car/CarType.java`.

The pumps, the Car Workbench, the Repair Bench and the mailbox are block models: `models/block/gas_pump.json` (the
diesel pump uses the same model with `textures/block/diesel_pump.png`), `models/block/car_workbench.json`,
`models/block/repair_bench.json` and `models/block/mailbox.json` (with `mailbox_flag_up.json` for when there's mail
waiting to go out), each with one 128×128 texture. The
pump's model is two blocks tall and faces north. Its screen's face must stay at z = 3, 19 to 26 pixels up, and its
sign's face at z = 3, 28 to 32 up, because that's where the lit price and the fuel's name are drawn. The screen and
sign glow (`neoforge_data` light on those elements).

## Project layout

```text
src/main/java/com/reiko/cars/
├── ReikoCars.java        mod entry point
├── car/                  the car entity and its types, fuel (CarFuel, CarTank), wear and tear (CarCondition), the car item
├── pump/                 the gas and diesel pump block, its block entity, menu, protection, and PumpService (selling fuel)
├── trailer/              the trailers, their item, hitching and car hauling (Hitching), and the tanker (TankerService)
├── workbench/            the Car Workbench block and menu, the car parts, and the car_assembly recipe type
├── repair/               the Repair Bench block, block entity, menu and protection, the jobs (RepairJobs), and RepairService (repairs and paying)
├── tow/                  the tow trucks' picking up, carrying and dropping off (Towing)
├── fare/                 bus fares and the taxi meter (CarFares), the pay and fare screens' menus, and FareService
├── key/                  car keys: the key items, what a key fits (CarKey), and cutting, changing the locks and the key fob (CarKeys)
├── mail/                 the mailbox block, block entity, menu and protection, parcels, the address list (MailRegistry), and MailService (sending and the mail van)
├── compat/jei/           the JEI plugin (only loaded when JEI is installed)
├── network/              packets: pump, repair bench and mailbox actions, horn, lock and hitch, the driver's wear reports, tanker load and unload
├── registry/             blocks, items, entities, menus, recipes, sounds, data components, capabilities, creative tab
├── config/               server settings
├── client/               car and trailer renderers and models, dashboard, pump, workbench, repair bench, mailbox and tanker screens, pump display, engine sound, keys
├── gametest/             in-game tests
└── dev/                  the showcase script (dev only, not in the jar)
blockbench/               the car, motorcycle, truck and trailer models (Blockbench projects)
```

## License

GPL-3.0, like Reiko's Economy Mod. See [LICENSE](LICENSE).
