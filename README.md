# Reiko's Car Mod

Drivable cars for Minecraft 1.21.1 (NeoForge) that run on **BuildCraft Reborn**'s fuel, and gas pumps that sell
that fuel for **cash or card** through **Reiko's Economy Mod**.

Refine oil into fuel with BuildCraft, pipe it into a gas pump, set a price, and other players pay you to fill up.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.264 or newer (Reiko's Economy Mod 1.0.0 itself needs 21.1.265)
- [BuildCraft Reborn](https://github.com/Reiko45/BuildCraft-Reborn) 2.1.0 or newer
- [Reiko's Economy Mod](https://github.com/Reiko45/Reiko-s-Economy-Mod) 1.0.0 or newer

## The cars

| Car | Seats | Top speed | Tank | Notes |
| --- | --- | --- | --- | --- |
| Sedan | 4 | 40 mph | 8 buckets | All-rounder |
| Sports Car | 2 | 58 mph | 6 buckets | Fast, quick to accelerate, thirsty |
| Pickup Truck | 2 | 36 mph | 12 buckets | 27-slot truck bed (sneak + use) |
| Off-Roader | 4 | 38 mph | 10 buckets | Keeps full speed on grass, sand and snow |

Tank sizes are in buckets of BuildCraft's **light fuel**. A sedan goes about 600 blocks on a bucket of it.

**Driving:** right-click a car to get in (the first person in drives). The movement keys drive and steer, **jump**
is the handbrake and **sneak** gets out. **H** sounds the horn and **L** locks or unlocks the car. While you're
in a car, a dashboard in the bottom right shows your speed and a fuel gauge.

- **Fuel:** cars burn fuel for every block they cover, and a little while idling with someone at the wheel. Any
  fluid in the `reikocars:car_fuels` tag works. That's BuildCraft's fuels, plus other mods' gasoline, diesel,
  biodiesel and ethanol. How far a bucket goes comes from BuildCraft's own fuel values, so **dense fuel goes four
  times as far as light fuel**. Crude oil doesn't run a car.
- **Filling up:** buy fuel at a gas pump, or (if the server allows it) pour a fuel bucket straight into the car.
- **Ownership:** whoever puts a car down owns it. New cars start **locked**, so only the owner can get in, though
  anyone can ride along while the owner is inside. Only the owner (or an admin) can pick a car back up. Punch it a
  few times and it drops as an item, keeping its fuel and paint. A pickup's bed spills its items.
- **Paint:** right-click a car with any dye.
- **Off-road:** grass, dirt, sand, snow and the like (the `reikocars:off_road` block tag) slow every car but the
  off-roader. Ice is slippery.
- **Running things over** hurts mobs and knocks them aside, more the faster you go. Players only get hurt with PvP
  on, and never your own pets.
- Cars climb a full block, so curbs, slabs and stairs don't stop them. Cars can't be blown up or burned.

## Gas pumps

A pump is two blocks tall. Whoever places it owns it, and only they (or an admin) can break it. A broken pump keeps
its fuel and price on the item.

**Getting fuel in:** connect any pipe (BuildCraft's or another mod's) to either half of the pump, from any side.
The pump holds 16 buckets of one kind of car fuel. Nothing can be piped back out, so the only way out is to sell
it. The owner can also fill it from a bucket.

**Buying fuel:** park within 6 blocks of the pump and right-click it. The screen shows the fuel in the pump, its
price, and the car at the hose. Choose an amount (or **Fill up**), then pay:

- **Card:** the card you carry (the ⇄ button switches if you have several), its PIN, then **Card**. A debit card
  pays from its bank account; a credit card puts it on the bill.
- **Cash:** **Cash** pays with the coins and bills you carry, or from your wallet, and gives change.

This is the economy mod's own shop payment, so sales tax applies and the money goes straight into the owner's
bank account, even while they're offline. The pump's owner fills up for free.

**Setting the price:** the owner sees a price row at the bottom of the screen. Type a price per bucket and press
**Set**. The lit display on the front of the pump shows the price, or EMPTY.

## Recipes

| Item | Recipe |
| --- | --- |
| Tire ×4 | dried kelp in the corners, black dye on the sides, centre empty |
| Car Engine | iron ingots, iron gears, pistons around a BuildCraft **Combustion Engine** |
| Sedan | 4 tires, glass pane, iron ingots, car engine |
| Sports Car | 4 tires, glass pane, gold ingots, gold gear, car engine |
| Pickup Truck | 4 tires, glass pane, iron ingots, chest, car engine |
| Off-Roader | 4 tires, glass pane, iron ingots, iron block, car engine |
| Gas Pump | iron ingots, glass pane, BuildCraft **Tank**, iron gear |

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
(and update the names in `gradle.properties`).

```sh
./gradlew build             # build the jar into build/libs/
./gradlew runGameTestServer # run the in-game tests (payments, pumps, pipes, fuel, locks), then exit
./gradlew runClient         # start a dev client
./gradlew runClient -Pshowcase  # build a test scene, buy fuel, drive, save screenshots to run/screenshots/, quit
```

## Editing the models

The cars are Blockbench projects in `blockbench/` (format: Modded Entity, box UV). Each has the same bones, which
the mod relies on:

- `body`: the paintwork. Paint it light grey: the game tints it the car's colour.
- `trim`: glass (may be see-through), seats, bumpers, grille and so on, drawn as painted.
- `lights`: head- and taillights, which glow while the engine runs.
- `wheel_fl`, `wheel_fr`, `wheel_rl`, `wheel_rr`: each pivoting on its axle. They roll, and the front pair steers.

The car's front faces north (−Z), and the driver's side is **−X** in Blockbench. To change a car, edit its
`.bbmodel`, save the texture over `src/main/resources/assets/reikocars/textures/entity/car/<car>.png`, use
**File > Export > Export Java Entity**, and paste the exported `createBodyLayer()` into
`client/model/<Car>Model.java`. If you move the seats, update the seat table at the top of `car/CarType.java`.

## Project layout

```text
src/main/java/com/reiko/cars/
├── ReikoCars.java        mod entry point
├── car/                  the car entity and its types, fuel (CarFuel, CarTank), the car item
├── pump/                 the gas pump block, its block entity, menu, protection, and PumpService (selling fuel)
├── network/              packets: pump actions, horn and lock
├── registry/             blocks, items, entities, menus, sounds, data components, capabilities, creative tab
├── config/               server settings
├── client/               car renderer and models, dashboard, pump screen and display, engine sound, keys
├── gametest/             in-game tests
└── dev/                  the showcase script (dev only, not in the jar)
blockbench/               the car models
```

## License

GPL-3.0, like Reiko's Economy Mod. See [LICENSE](LICENSE).
