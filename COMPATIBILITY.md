# Addon schematic compatibility

This describes **0.0.2**. The main build targets NeoForge 1.21.1 / Create 6.0.10. The companions target Forge 1.20.1 / Create 6.0.8 and Fabric 1.20.1 / Create Fabric 6.0.8.1. Support covers the fields and placement paths listed below; a successful startup does not certify every feature of an addon.

## Installation and shared files

Local saving and Schematic Table mod inspection work with Better Schematics installed only on the client. Configuration that a server does not send to its client cannot be recovered by a client-only addon. Install Better Schematics on the destination server for extended placement fixes, and on both peers for the NeoForge skip picker and material export. All five network channels are optional; stock Create peers can still connect. The mod adds no block-state property or required structure-processor registry entry.

Files remain ordinary Create `.nbt` structures. Native settings and compatible blocks can be loaded by stock Create. Supported Create 6 cross-version transfers include both legacy and modern item representations; the companions adapt supported fields in memory without rewriting the file. This is not a general Minecraft downgrade. Both instances need compatible versions of the blocks and addons used by the schematic.

Capture a fresh schematic after installation. Select both endpoints and backing blocks for connected gauges, wires or rods. Out-of-selection connections are omitted. Older schematics without gauge-position metadata cannot reliably relocate gauge connections. Fabric fluid thresholds are converted when the source unit is known; older files without that information retain their recorded amounts and need checking.

## NeoForge controls

- The Schematic Table's **i** tooltip reads referenced mod IDs directly from the selected file, including missing blocks/items. Missing namespaces appear first in red; installed mods appear green. Scroll over the icon for longer lists and click to refresh. Optional Blueprinted and Schematic Helper controls share the compact tabs.
- The cannon picker groups encased shafts, cogs and pipes under obtainable items. Applied choices still refer to original block types. Composite costs remain separate and material exports use Create's real requirements. Partially skipped groups remain visible in amber.
- The picker uses Create's native menu art and Minecraft's creative-inventory scrollbar. Its track is separate and centered beside the full frame. No artwork is bundled. Manual Pause prevents new launches and fuel use with normal supplies or a Creative Crate; Play resumes.
- JEI **19.53+**, with EMI absent, supports material bookmarks and quantities. Apply refreshes imported materials while preserving personal bookmarks. The server needs Better Schematics but no recipe viewer.
- With EMI installed, export requires [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) **1.1.0+**, by **LiukRast and swzo**. Its public exporter manages favourites and quantities. Better Schematics bundles neither that addon nor its exporter. EMI takes priority over JEI; missing addon support disables the button with an explanation. Click + again after reconnecting or changing schematics.

The current language file is English (`en_us`). Minecraft falls back to it for other languages. Release checks verify all 74 keys, including packet-result messages, and formatting placeholders.

## Core configuration and placement

- Mechanical arms preserve target coordinates, selection modes and delayed destinations. Destinations follow rotations/mirrors and retry when their blocks become available.
- Threshold switches preserve limits, inversion and stack display. Legacy Create 0.5.1 percentage settings migrate when destination capacity becomes available; Create 6 amount fields take precedence.
- List and built-in Create 6 attribute filters preserve rules, inversion, mode and supported nested item data. Custom names, lore, damage, enchantments and custom data receive supported legacy equivalents. Addon-defined attributes and unsupported component formats need their own adapters.
- Factory Gauges preserve four panel slots, filters, targets, units, frequencies, recipe addresses/counts, crafting layouts, promise expiry settings and gauge/redstone-link connections. Endpoints and panel slots transform with the schematic. Pending source deliveries are excluded.
- Belt casings use Create's native `Casing` NBT. Older schematics with the addon's former material property migrate when read/exported. Tools that discard block-entity NBT also discard casing material.
- Chain-drive regression coverage exercises 150 layouts and placement cases across connection axes, shaft axes, rotations, mirrors, projectiles and reloads. These pass with Create 6.0.10's native orientation behavior; no extra chain-orientation patch is included.
- Power Grid wires preserve transformed endpoints/routes, colors, paid item counts and supported hanging-wire sag. Copies receive fresh junction identities. NeoForge and Forge tests check complete payment for a 130-item wire and exact returns on removal.
- Transmission & Linkage universal-joint rods copy within one world when both endpoints are selected. Rotated/mirrored copies consume one rod per connection. Hydraulic and cross-sublevel links still need reconnecting. Version 0.2.8's repeated Toolgun recommendation is suppressed; 0.2.9 removed that handler.

## Verified NeoForge features

The matrix records tested addon versions. The latest selected-addon run uses Minecraft 1.21.1, NeoForge 21.1.256, Create 6.0.10, Flywheel 1.0.6 and Ponder 1.0.82. Tests export actual files, use Create's printer, transform placements, restore saved cannon projectiles and reload block entities. Native safe-NBT writers remain active alongside added profiles.

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

## Remaining limits

- Physical graphs need their own relocation and material adapters. Power Grid wires and Create gauges have those adapters. This release does not add adapters for Crafts & Additions wire networks, Struts/Coaster connections, Hypertube graphs, ship/sublevel attachments, flight/navigation routes, or Radar target references.
- Profiles preserve selected settings and supplement the addon's native safe data. They do not add undeclared inventories, energy, processing progress, ownership, bank accounts, or physical network identities. Existing native safe-NBT writers remain authoritative for the data they already supply.
- Multi-block assembly, directional motion, every Copycats multipart variant, huge Diesel Generator assemblies, MIDI assets/roll contents, and optional integrations such as Apotheosis need their own feature-specific validation. The table certifies the listed configuration transfers.
- World-specific destinations and active targets are excluded from configuration profiles. Reconnect or configure those after placement unless a dedicated adapter is listed.
- Transmission & Linkage copies block settings and in-world universal-joint rods with both endpoints selected. Hydraulic connections, cross-sublevel rods, creative/giant rod modes and settings, runtime forces/strain, generated stress/speed, and ship/hinge sublevel identities remain outside this adapter. Supply and reconnect those rods separately. Version 0.2.8's hydraulic linking action reinitializes expected length and redstone length limits; reapply those after reconnecting. Hinge angle limits are preserved numerically; mirrored physical motion across ship/sublevel assemblies is outside these configuration tests.
- The AE2 coverage is for the listed full-block machines; multipart cable parts and paid encoded-pattern/upgrade items require separate adapters. Storage Drawers compacting/framed variants and Sophisticated Storage linked-inventory networks are outside this configuration certification. Limited barrels retain the addon's required exact item-data matching mode.
- The NeoForge addon matrix does not certify the same addon on Forge 1.20.1. A destination still needs compatible versions of the blocks and mods used by the schematic. The companion's established gauge and Power Grid transfers remain tested in both directions.

## Validation and reproduction

Final local checks on November 5, 2026:

| Check | Result |
| --- | --- |
| NeoForge base suite, including Creative Crate/normal-fuel pause checks | 199 required tests passed |
| NeoForge selected installed addons, with pinned Tom's Storage fallback | 365 required tests passed |
| Stock Create recipient without Better Schematics | 6 required tests passed |
| Forge with Power Grid | 10 required tests passed |
| Stock Forge recipient without Better Schematics | 2 required tests passed |
| Fabric companion | 5 required tests passed |
| Audited production JAR in a real client, with Blueprinted/Schematic Helper | 109 checks passed |
| Same production JAR with the optional Core dark-theme integration | 110 checks passed |

The selected-addon run uses 58 copied addon/dependency JARs plus the pinned Tom's Storage 2.4.2 fallback, covering the listed configuration, transformation, payment and placement cases. This is not certification of every feature in the full pack. Local log evidence is kept in ignored `build/release-final-checks/`; generated results and downloaded mods are not release content.

The final client checks load the audited production JAR, inspect actual resources, exercise the tooltip tabs and optional controls, and capture normal/compact layouts. Normal and dark screenshots were visually reviewed. Earlier JEI/EMI client checks covered quantity exports, automatic skip refresh, restoring an empty list and retaining personal bookmarks; those are prior local checks, separate from the final runs above.

Core tests and resources are in `src/testFixtures`, outside production sources. Compatibility and companion suites use their own test directories. These tests have executable assertions and are excluded from release JARs. The captured Forge export used by the stock NeoForge recipient is included in test resources, so that suite also works from a clean checkout. GitHub Actions builds and audits all three loader artifacts and runs the core/Create-only NeoForge regressions.

Run core tests with a Java 21 JDK:

```powershell
.\gradlew.bat -PgameTests runGameTestServer
.\gradlew.bat -PgameTests -PnativeCompatibilityTests runNativeGameTestServer
```

Prepare an isolated addon test set from an installed pack, then run:

```powershell
py -3 tools/prepare_addon_test_mods.py --mods-dir 'C:/path/to/instance/mods'
.\gradlew.bat -PcompatTests -PcompatModsDir=build/addon-test-mods runGameTestServer
```

For the tested NeoForge/Flywheel combination, add `-Pneo_version=21.1.256 -PcompatFlywheelVersion=1.0.6`. To add the pinned factory/storage mods, run `py -3 tools/prepare_popular_mod_test_mods.py` and supply `--additional-mods-dir build/popular-mod-test-mods` to the preparation tool. Their versions and SHA-512 hashes are in `tools/popular-mod-test-lock.json`.

Run the companions with their matching wrappers:

```powershell
.\gradlew.bat -p forge -PcompatTests runGameTestServer
.\fabric\gradlew.bat -p fabric runGametest
```

Forge's development GameTest task serializes early mod initialization to avoid the Create/Registrate startup failure seen with parallel initialization. The task requires a fresh passing log, so a mod-loading error cannot count as success. Power Grid's optional native solver is unavailable in this Windows test environment; the tests use its Java fallback.

To check stock Forge sharing after a NeoForge producer run, use `./gradlew -p forge -PnativeCompatibilityTests runNativeGameTestServer`. The producer writes fresh fixtures into `build/shareable-fixtures`. The Fabric task isolates its five companion tests; it does not run the entire bundled Create suite, whose threshold-switch/attribute-filter failures were previously reproduced with the companion absent.

Build and audit release artifacts with the [README commands](README.md#building-and-checking). Source/JAR checks reject duplicate or missing language keys, invalid placeholders, version mismatches, missing mixin classes, incorrect Java bytecode targets, bundled artwork and test/development content.

## Extending a profile

Call these APIs during common setup, using the block entity type ID or fully qualified class name from the target addon:

```java
SchematicConfigurationRegistry.register(typeId, "Filter", "Limit");
SchematicConfigurationRegistry.registerClass("example.addon.MachineBlockEntity", "Settings.Mode");
SchematicConfigurationRegistry.registerBehaviour("example.addon.ProgramBehaviour",
        "Instructions.*.Type", "Instructions.*.Value");
```

Class and behaviour matches include subclasses without loading optional addon classes. Nested compound paths and compound-list wildcards copy only named fields. Use `registerPlacementDefaults` for runtime fields whose absent value would start a copied machine. Installed physical parts need material requirements in addition to copied configuration; the Diesel Generators adapter demonstrates that boundary. Spatial references need a placement processor rather than a field profile.

The native contract is documented by Create's [safe-NBT registry](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/api/schematic/nbt/SafeNbtWriterRegistry.java); the integration details here were checked against the pinned local addon JARs.
