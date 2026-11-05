# Schematic compatibility

Version **1.0.0** targets NeoForge 1.21.1 / Create 6.0.10, with Forge 1.20.1 / Create 6.0.8 and Fabric 1.20.1 / Create Fabric 6.0.8.1 companions. The addon matrix below applies to NeoForge and the listed versions; it does not certify every feature or loader version of an addon.

## Shared schematics

Files remain ordinary Create `.nbt` structures with optional compatibility metadata and native configuration fields. Stock Create can load compatible blocks and native settings. The companions adapt supported legacy item fields, arm coordinates and fluid units in memory without rewriting the shared file. Extended placement behavior can require the matching companion on the recipient.

Mechanical arms retain targets and selection modes. Threshold switches retain limits, inversion and stack display. List and built-in Create 6 attribute filters retain supported rules and item data. Belt casings use Create's native `Casing` NBT. Factory Gauges retain panel settings, recipe layouts, frequencies and selected connections, with endpoints adjusted for rotation and mirroring.

The Schematicannon preserves the saved belt's shaft axis when rebuilding over existing shafts. Completed jobs clear block skips and skip-missing settings so the next schematic starts without the previous job's exclusions.

Select both endpoints and backing blocks for connected gauges, wires or rods. Connections outside the selection are omitted. Capture a fresh schematic after installing the addon: information already removed from an old schematic cannot be reconstructed. Fabric fluid limits are converted only when the source unit is known; check older files without that information.

A client-only installation can save only the configuration its server sends it. Install Better Schematics on the destination server for extended placement fixes, and on both peers for the NeoForge cannon picker and material export.

## Addon settings

| Addon | Tested version | Verified scope |
| --- | --- | --- |
| Power Grid | 0.6.2 | Existing wire transfer/endpoint transforms/material costs, meter units, display equations/units/prefixes, button timeout and normally-closed setting. Wires also have Forge cross-loader tests. |
| Tom's Storage | 2.4.2 | Emitter filter/threshold/comparison, hopper ghost filter, cable channel, painted trim and proxy camouflage. |
| Crafts & Additions | 1.6.0; 1.7.2 (November 3) | Motor speed and direction. |
| Copycats+ | 3.0.9 | Single-material copycat material, consumed item, connected-texture setting, and a working cannon paying for the material. |
| Railways / Steam 'n' Rails | 0.2.1 | Headstock wood/color, wooden buffer material, smokestack color, conductor whistle flag color. |
| Connected | 1.3.2 | Dashboard sign text/color/glow; pulse-program parameters, fresh progress counters, and idle placement. |
| New Age | 1.2.0 | Energiser capacitor mode and motor speed. A NeoForge mixin reconciles `capacitorMode` with the legacy reader's `capacitorModer` spelling without mutating input data. |
| Diesel Generators | 1.3.15 | Normal-engine direction and upgrade selection, modular-engine upgrade data, silencers and turbochargers. Checklist and working-cannon tests charge for installed upgrades exactly once. |
| Enchantment Industry | 2.5.4b | Enchanting level, printer template, forger mode. |
| Bits 'n' Bobs | 2.3.5 | Nixie text/index/display mode and throttle lever value, including required backing blocks. |
| Quality of Life | 1.6.3-fix1 | Inventory linker's inventory-selection mode. |
| Mobile Packages | 0.7.8 | Bee-port return mode and filter mode. |
| Numismatics | 1.1.0 | Native price settings and bank labels. Added profiles exclude money, cards, inventories, and ownership data. |
| Pipe Organs | 0.9.2.1 | Pipe pitch, native note-link frequency, MIDI ghost frequencies, tracker buttons. |
| Simulated | 1.3.2 | Altitude thresholds, laser color/range, sensor opacity, nameplate text/style, receiver distance limits, and nonempty typewriter key/frequency bindings. |
| Gadgets & Gizmos / Thrusters bundle | 2.0-pre-release-3; 2.0-pre-release-4 bundle (November 3) | Camera FOV/raycast/filter settings, display dimensions, dock options/name, coupler inversion, scissor range, native joystick settings. These blocks use the `createthrusters` mod ID. |
| Propulsion | 1.1.5 | Tilt limits, thruster dye/plume settings, native power controls and dual-row value. |
| Radar | 0.4.9.4; 5.0.3 (November 3) | Scan field of view, height/range and track expiry. |
| Submarine | 3.3.0 | Alarm period and barometer name. |
| High Seas | bundled with Submarine 3.3.0 | Engine throttle cap and enabled setting; added profile omits fuel and burn progress. |
| Create Aeronautics: Transmission & Linkage | 0.2.8; 0.2.9 (November 3) | Damping-bearing resistance; hydraulic and hinge-head stretch resistance, free mode, expected length, redstone minimum/maximum lengths and return force; brass hinge angle limits. Both universal-joint blocks support selected in-world andesite/brass rod connections, including transforms, delayed endpoints, reloads, saved cannon projectiles and one-rod material payment. |
| Warnautics | 1.0.8 | Siren scanning/watch settings, with live linger omitted from the profile. |
| Northstar | 0.6.6 | Native combustion-engine speed. |
| Petrochem | 1.3.3 | Native small-engine speed. |
| Optical | 0.4.2 | Native beam-modulator frequency. |
| Big Cannons | 5.11.7 | Native autocannon firing rate. |
| Azimuth | 1.4.10 | Coordinate-marker placement/rebuilding; its neighbor-derived coordinate is allowed to recompute. |
| [Mekanism](https://modrinth.com/mod/mekanism) | 10.7.19.85 | Machine redstone controls, side IO and auto-ejection, transporter input/output colors, factory sorting, and logistical-sorter ghost filters/modes. Side settings follow rotation/mirroring, including vertically facing energy cubes. |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 19.2.18 | Full-block inscriber options, IO-port redstone/fill/operation modes, interface ghost stock lists/fuzzy mode/priority, and pattern-provider blocking/locking/visibility/priority. |
| [Storage Drawers](https://modrinth.com/mod/storagedrawers) | 13.11.4 | Standard one-, two-, and four-slot drawers retain remembered item types, locks, quantity/concealment settings, priority and suspension. Copied drawers start with zero stored items and no installed upgrades. |
| [Sophisticated Storage](https://modrinth.com/mod/sophisticated-storage) | 1.6.8.2196 | Barrel, chest and limited-barrel memory filters, item-data matching, no-sort slots, sorting mode, item-display settings and lock/display options. Inventories, upgrades, expanded capacities and linked-storage identities are omitted. |

## Limits

- Both instances need compatible versions of the blocks and mods used by the schematic. This is not a general Minecraft downgrade or arbitrary addon-data converter.
- Added configuration profiles omit inventories, energy, processing progress, ownership, bank accounts and physical network identities. Existing native safe-NBT writers remain responsible for the data they supply.
- Power Grid wires and Factory Gauges have dedicated endpoint adapters. Crafts & Additions wire networks, Struts/Coaster connections, Hypertube graphs, flight routes, Radar targets and ship/sublevel attachments need reconnecting or separate support.
- Transmission & Linkage copies settings and same-world universal-joint rods when both endpoints are selected. Hydraulic connections, cross-sublevel rods and creative/giant rod modes need reconnecting. Hydraulic linking can reset expected length and redstone limits; reapply those settings afterward.
- AE2 support covers the listed full-block machines, not multipart cable parts, paid encoded patterns or upgrades. Storage Drawers compacting/framed variants and Sophisticated Storage linked-inventory networks are outside these profiles.
- Arbitrary Copycats multipart layouts, large Diesel Generator assemblies, MIDI assets, addon-defined attribute rules and unsupported item components may need feature-specific adapters.
