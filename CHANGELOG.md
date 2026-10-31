# Changelog

Reiko's Car Mod, for Minecraft 1.21.1 (NeoForge). Needs BuildCraft Reborn 2.1.0+ and Reiko's Economy Mod 1.1.0+;
JEI, Patchouli and Xaero's Minimap are optional.

## 1.39.1

- Parked cars are much lighter on the server: a car with nobody at the wheel, sitting still on the ground, skips its
  physics and collision checks (it looks again once a second, in case the ground's gone from under it). In a load
  test of 500 parked cars and 20 drivers on a dedicated server, the cars' share of the server's time fell from about
  half to about a fifth.
- Asking a car who's at the wheel (done many times a tick) no longer builds a list each time.
- The mod list's description covers everything the mod does now, with links to the source and the issue tracker; JEI
  is listed as an optional dependency.

## 1.39.0

- Advancements: a "Reiko's Cars" tab with 26 of them, from your first drive to First Road Trip (1,000 blocks without
  getting out), Cross Country (10,000, worth 100 experience), Fleet Owner (five cars at once) and Checkered Flag (win
  a race), with branches for fuel, owning cars, the law, looking after a car, trailers and the driving jobs.
- One advancement trigger, `reikocars:car`, with an event and an optional minimum, so datapacks can add their own.

## 1.38.1

- Road paver: bridges get railings (walls along both sides), the sides of cuts and fills slope back instead of standing
  straight up, and lines follow bends smoothly from block to block.
- The last block of road laid gets its lines too.

## 1.38.0

- The Road Paver: a diesel work vehicle that lays a road as you drive it, any width from 3 to 15 blocks, with center,
  lane and edge lines, sidewalks and street lamps. It cuts through hills, fills dips and bridges gaps, using what's in
  its hopper, then your inventory, then (if set to) your bank account.

## 1.37.0

- Visible crash damage: crumpled fronts, caved-in sides and dented backs, windows that crack and shatter, and smashed
  lamps. A car bent on one side pulls toward it. The Repair Bench fixes it, and insurance pays for it.

## 1.36.0

- Garages: Garage Doors and Roller Shutters any size, built from panels and painted with dye, opened by hand, with a
  remote, a wall button, or by driving up to them. Cars shut in a garage can't be stolen, towed or impounded. Parking
  garages charge by the hour or the day.

## 1.35.1

- Fixes from a full test pass, among them getting out of a tall vehicle onto its roof.

## 1.35.0

- The Car Manual: a guide book (with Patchouli) with a chapter for each part of the mod, every vehicle turning in 3D,
  its recipes, and screenshots. Every player gets one the first time they join.

## 1.34.0

- A tow ball on the pickups, the off-roader, the minivan and the farm tractor, and trailers for it: the Camper (sleep in
  it, cook on its stove, stock its cupboards; its awning and chairs fold out when parked) and the Livestock Trailer. The
  semi gets a Fifth-Wheel Camper and a Livestock Trailer of its own.

## 1.33.0

- Bus stops: a Bus Stop Sign and a Bus Shelter with a bench. The city bus calls at its owner's stops in order, opens its
  doors for whoever's waiting, carries villagers between stops for a fare, and its driver is paid for each stop.

## 1.32.0

- GPS (N): the gas stations, chargers, mechanics, car washes and other places nearby, with their prices, and your cars
  wherever they're parked, with directions. With Xaero's Minimap, its destination and your cars show on the minimap.

## 1.31.0

- Weather driving: rain, storms and snow cut grip, wipers sweep the windshield, snow settles on parked cars, and
  Winter Tires grip on snow and ice.

## 1.30.0

- Dirt: cars get dirty off-road, in mud and in the rain, and it shows. Car Washes, coin-op Wash Bays with a pressure
  washer, or a bucket of water clean them. A filthy car rusts, and dealers pay less for it.

## 1.29.0

- The Extra Battery Module: more range for electric cars, fitted at the Tuning Bench.

## 1.28.0

- The Extended Fuel Tank: a bigger tank for gasoline and diesel cars, fitted at the Tuning Bench.

## 1.27.0

- Tuning and racing: the Tuning Bench fits engine stages, a turbo, tires, brakes and suspension, and paints race
  numbers and stripes. A Race Control and numbered race flags make a track with timed laps and races for a pot.

## 1.26.0

- License plates on every car, driver's licenses earned with a driving test at a DMV, penalty points from cameras, and
  police officers' Ticket Books.

## 1.25.0

- Car loans from dealerships, impound lots (for repossessed and towed cars) and car rentals by the day.

## 1.24.0

- The fire truck: a fire hose that sprays from its water tank, an aerial ladder, and fire hydrants to fill it.

## 1.23.0

- Electric cars (the City EV, Electric Sedan, Electric Pickup, Electric Van and Electric Moped) and charging stations
  powered by BuildCraft engines.

## 1.22.0

- Headlights that light the road at night and underground.

## 1.21.1

- Water no longer shows through car windows.

## 1.21.0

- Radio stations play on a world schedule, so every car on a station plays the same moment; trailers are solid; every
  vehicle's icon redrawn; the dashboard keeps clear of the hotbar.

## 1.20.0

- A car radio: three stations of original music, or your own discs.

## 1.19.0

- Roads: asphalt and Road Paint, traffic lights, stop signs, street lamps, and red-light and speed cameras that fine
  drivers for their owners.

## 1.18.0

- Turn signals, hazard lights and car alarms.

## 1.17.0

- Work vehicles: the police car, ambulance, garbage truck, ice cream truck, dump truck and cement mixer.

## 1.16.0

- Insurance offices, and car jobs on the economy mod's job board.

## 1.15.0

- Parking meters and toll gates.

## 1.14.0

- Buying and selling cars between players, and car dealerships.

## 1.13.0

- The taxi and its meter, and bus fares.

## 1.12.0

- Car keys.

## 1.11.0

- Tow trucks, the mail van and mailboxes.

## 1.10.0

- Repairs for locked vehicles, and trailer wear.

## 1.9.0

- Wear and tear, and the Repair Bench.

## 1.8.0

- Every vehicle model, and the pump and workbench, rebuilt in detail.

## 1.7.0

- Eight more vehicles: the hatchback, muscle car, convertible and minivan on gasoline; the delivery van, city bus,
  heavy-duty pickup and farm tractor on diesel.

## 1.6.0

- Motorcycles: the sport bike, cruiser, dirt bike and scooter. They lean into turns.

## 1.5.0

- Trailers are built at the Car Workbench.

## 1.4.0

- The semi truck, on diesel, and its trailers: the box trailer, tanker and car hauler.

## 1.3.0

- The diesel pump, and selling fuel by the bucket.

## 1.2.0

- JEI support.

## 1.1.0

- The Car Workbench: cars are built from their own parts.

## 1.0.0

- Four drivable cars that burn BuildCraft Reborn's fuels, and gas pumps that sell fuel for cash or card through
  Reiko's Economy Mod.
