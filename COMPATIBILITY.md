# Addon schematic compatibility

Version 0.0.4 button-layout validation, 0.0.3 porter validation and the earlier 0.0.2 addon matrix, October 23, 2026. Coverage is specific to the features and versions below. The expanded addon matrix has not been rerun for 0.0.3 or the UI-only 0.0.4 update. The latest Transmission & Linkage run loaded 67 selected addon/dependency JARs exposing 92 mod IDs; the earlier expanded run loaded 71 JARs exposing 99 mod IDs. The earlier audit inspected 63 top-level addon/integration JARs and five bundled Aeronautics/Gadgets modules from the available local modpacks; the factory/storage integrations were inspected from their pinned official artifacts.

## Schematic Porter (0.0.3)

The NeoForge schematic table has a native Create porter button above Create Schematic Helper's import button. Without the helper, Porter uses the import slot and Refresh moves below it. The 0.0.4 button remains usable with no schematic selected and shows instructions while disabling export. It saves a separate `.nbt` copy for Minecraft 1.20.1 / Create 6, converts item-component data to legacy item NBT and arm positions to X/Y/Z compounds, and scales Fabric fluid thresholds to droplets. List filters retain sparse slots, allow/deny and respect-data modes, nested item data, names, styles and lore. Create 6 attribute filters retain built-in rules, rule inversion, and any/all/deny mode. The destination companions retain arm targets that are placed later and threshold limits, inversion and stack display through cannon placement and world reloads.

| Environment | Tested versions | Result | Evidence |
| --- | --- | --- | --- |
| NeoForge source and round trips | Minecraft 1.21.1, Create 6.0.10, NeoForge 21.1.255 | 189 required tests passed, including five porter/regression tests | `build/porter-neoforge-gametest.log` |
| Forge destination | Minecraft 1.20.1, Create 6.0.8, Forge 47.4.10 | Five required tests passed, including two converted-file tests | `build/porter-forge-gametest.log` |
| Fabric destination | Minecraft 1.20.1, Create Fabric 6.0.8.1+build.1744, Fabric Loader 0.17.3, Fabric API 0.92.6 | Three required porter tests passed | `build/porter-fabric-gametest.log`, `build/porter-fabric-results.xml` |
| Porter screen | NeoForge development client, 320x240 GUI and resized view | Both targets export successfully; source bytes and completed reports survive | `build/porter-ui/PASS.txt`, screen PNGs in that directory |
| Porter button layout (0.0.4) | NeoForge client at 320x240 GUI and after resizing, with and without Create Schematic Helper 2.2.0 | Native button styling, import-slot fallback, clear spacing, clickable extra areas, empty-selection prompt, selected-file handoff, download-mode access and single-button recreation pass | `build/porter-layout/helper-PASS.txt`, `build/porter-layout/standalone-PASS.txt`, screen PNGs in that directory |

These cases use actual files emitted by the porter, then exercise the destination's native schematic printer, rotation/mirroring, serialized cannon projectiles, delayed arm targets and block entity reloads. The fluid case loads a real Fabric fluid-tank observer with converted limits of 162,000/40,500 droplets for the original 2,000/500 mB settings. A regression guard checks that serializing a threshold switch never queries its observed inventory or loads a neighboring chunk. File checks verify that originals and earlier exports stay byte-for-byte unchanged; invalid files and unsupported modern attribute rules produce no partial export.

The full bundled Create Fabric test suite was also checked: `TestFluids.thresholdSwitch` and `TestItems.attributeFilters` fail both with and without this companion. The companion-disabled baseline ran 61 tests and reproduced both failures (`build/porter-fabric-baseline-results.xml`, `build/porter-fabric-baseline.log`). The normal companion test task isolates its three transfer cases; it does not claim those upstream failures are fixed. Test-only filtering and fixtures are excluded from the production Fabric JAR.

Both target instances need the blocks and mods referenced by the schematic. This porter does not convert addon wiring or unavailable 1.21 blocks. Fresh exports record switch units; older files with unknown units report that fluid thresholds need checking. Item components without a legacy mapping are listed in the report. Addon-defined attribute rules and pre-Create-6 attribute encodings are outside the supported conversion scope. Create 0.5.1 is not an export target.

## Verified NeoForge features

These tests use Minecraft 1.21.1, NeoForge 21.1.255, and Create 6.0.10. Tests exercise real block entities, export actual `.nbt` files, load them with Create's schematic printer, apply a rotation or rotation plus mirror, restore serialized cannon projectiles, and reload the resulting block entities. Native addon support is retained and tested alongside the new profiles.

Create's native chain-drive coverage adds 150 tests: 144 geometric combinations of connection axis, shaft axis, rotation, mirror and placement path, plus six interleaved-processor placement cases. Both encased chain drives and adjustable chain gearshifts retain their shaft axis, connection plane and end/middle roles, and remain connected after ticking and reloads. The processor cases wait for the whole cannon build and all projectiles to finish before checking the final twists. These layouts did not reproduce a chain-orientation defect, so no additional orientation patch is claimed.

| Addon | Tested version | Verified scope |
| --- | --- | --- |
| NeoForge with chain-drive coverage and installed content | 374 required tests passed, including 150 chain-drive checks | `build/chain-drive-addons.log` |
| NeoForge base with chain-drive coverage | 184 required tests passed | `build/chain-drive-base.log` |
| Power Grid | 0.6.2 | Existing wire transfer/endpoint transforms/material costs, meter units, display equations/units/prefixes, button timeout and normally-closed setting. Wires also have Forge cross-loader tests. |
| Tom's Storage | 2.4.2 | Emitter filter/threshold/comparison, hopper ghost filter, cable channel, painted trim and proxy camouflage. |
| Crafts & Additions | 1.6.0 | Motor speed and direction. |
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
| Gadgets & Gizmos / Thrusters bundle | 2.0-pre-release-3 | Camera FOV/raycast/filter settings, display dimensions, dock options/name, coupler inversion, scissor range, native joystick settings. These blocks use the `createthrusters` mod ID. |
| Propulsion | 1.1.5 | Tilt limits, thruster dye/plume settings, native power controls and dual-row value. |
| Radar | 0.4.9.4 | Scan field of view, height/range and track expiry. |
| Submarine | 3.3.0 | Alarm period and barometer name. |
| High Seas | bundled with Submarine 3.3.0 | Engine throttle cap and enabled setting; added profile omits fuel and burn progress. |
| Create Aeronautics: Transmission & Linkage | 0.2.8 | Damping-bearing resistance; hydraulic and hinge-head stretch resistance, free mode, expected length, redstone minimum/maximum lengths and return force; brass hinge angle limits. Both universal-joint variants and the regulator also have block-state placement/reload checks. |
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

There are 81 transfer fixtures across these 31 mod IDs. Ten additional working-cannon fixtures cover Copycats material payment, the two Diesel Generators upgrades, a block from each supported factory/storage mod, and three Transmission & Linkage blocks. The eight transmission fixtures run through both creative placement and saved cannon projectiles, with rotation, mirroring, reloads, and checks that source settings and links stay intact. A separate Mekanism test checks all 72 facing/rotation/mirror combinations against its native `RelativeSide` API. The storage cases seed source inventories and verify that ghost filters remain non-extractable after cannon placement and reload. The shared registry also has tests for nested/list field selection, inherited profiles, defensive copies, placement defaults, identity protection, and rejecting unpriced installed parts.

The four factory/storage mods were selected from established projects with millions of downloads on their linked official Modrinth pages. Their exact artifacts and the required GuideME 21.1.19 / Sophisticated Core 1.5.17.2426 libraries are pinned with SHA-512 hashes in `tools/popular-mod-test-lock.json`. They remain optional at runtime.

## Other audited content

Aeronautics/Offroad, Aeroworks, Integrated Farming, Central Kitchen, Create Deco, Create Nuclear, Create Dragons Plus, Create BB, Contraption Terminals, Vibrant Vaults, Power Loader, Teleporters, Hypertubes, Tracks, Linear Bearing, Coasters, Struts, Rolling Tones, Trading Floor, Garnished, Interiors and the installed schematic-helper/integration mods were inspected or included in the content loading run. Decorative blocks and settings already handled by Create's block-state/safe-NBT APIs continue to use those paths. Loading successfully is not a feature-complete certification of each addon.

Metalwork 3.0.0 includes an unused `GenericDisplayBlockEntity` library class with no registered block or other class reference in that JAR. It is not advertised as a tested display feature. Its registered decorative content uses the ordinary block-state path.

## Remaining limits

- Physical graphs need their own relocation and material adapters. Power Grid wires and Create gauges have those adapters. This release does not add adapters for Crafts & Additions wire networks, Struts/Coaster connections, Hypertube graphs, ship/sublevel attachments, flight/navigation routes, or Radar target references.
- Profiles preserve selected settings and supplement the addon's native safe data. They do not add undeclared inventories, energy, processing progress, ownership, bank accounts, or physical network identities. Existing native safe-NBT writers remain authoritative for the data they already supply.
- Multi-block assembly, directional motion, every Copycats multipart variant, huge Diesel Generator assemblies, MIDI assets/roll contents, and optional integrations such as Apotheosis need their own feature-specific validation. The table certifies the listed configuration transfers.
- World-specific destinations and active targets are excluded from new profiles. Reconnect or configure those after placement unless a dedicated adapter is listed.
- Transmission & Linkage copies block settings, excluding rod connections, creative/giant rod modes and settings, runtime forces/strain, generated stress/speed, and ship/hinge sublevel identities. Rods must be supplied and reconnected separately. Version 0.2.8's linking action reinitializes expected length and redstone length limits; reapply those after reconnecting. Hinge angle limits are preserved numerically; mirrored physical motion across ship/sublevel assemblies is outside these configuration tests.
- The new AE2 coverage is for the listed full-block machines; multipart cable parts and paid encoded-pattern/upgrade items require separate adapters. Storage Drawers compacting/framed variants and Sophisticated Storage linked-inventory networks are outside this configuration certification. Limited barrels retain the addon's required exact item-data matching mode.
- The NeoForge addon matrix does not certify the same addon on Forge 1.20.1. A destination still needs compatible versions of the blocks and mods used by the schematic. The companion's established gauge and Power Grid transfers remain tested in both directions.

## Validation and reproduction

| Run | Latest result | Evidence |
| --- | --- | --- |
| NeoForge with Transmission & Linkage 0.2.8 and installed content | 224 required tests passed, including 35 transmission checks | `build/transmission-linkage-after.log` |
| NeoForge with expanded content set and the four new mods | 207 required tests passed | `build/popular-compat-expanded.log` |
| NeoForge with the four new mods and core integrations | 94 required tests passed | `build/popular-compat-second.log` |
| NeoForge without optional addons | 34 required tests passed | `build/popular-compat-base.log` |
| NeoForge with Power Grid, Tom's Storage, Crafts & Additions | 61 required tests passed | `build/anti-dupe-gametest.log` |
| Forge without optional addons | 3 required tests passed | `build/popular-compat-forge-base.log` |
| Forge with Power Grid | 6 required tests passed | `build/popular-compat-forge-addons.log` |

```powershell
py -3 tools/prepare_addon_test_mods.py --mods-dir 'C:/path/to/instance/mods'
# Optional: add pinned addons/dependencies from a separate directory.
py -3 tools/prepare_addon_test_mods.py --mods-dir 'C:/path/to/instance/mods' --additional-mods-dir 'C:/path/to/extra-jars'
.\gradlew.bat -PcompatTests -PcompatModsDir=build/addon-test-mods runGameTestServer
```

To reproduce the four popular-mod checks without an installed modpack:

```powershell
py -3 tools/prepare_popular_mod_test_mods.py
.\gradlew.bat -PcompatTests -PcompatModsDir=build/popular-mod-test-mods runGameTestServer
```

To combine those pinned mods with an installed content pack, use `--additional-mods-dir build/popular-mod-test-mods --output build/popular-expanded-test-mods` with `prepare_addon_test_mods.py`, then set `-PcompatModsDir=build/popular-expanded-test-mods`. The preparation tool recognizes both uppercase and lowercase NeoForge dependency declarations, including AE2's GuideME dependency.

The preparation tool never changes the input modpack. It copies content addons and required dependencies into an isolated build directory and writes a manifest. A stale output directory is rejected instead of silently retaining extra JARs. The test-only mixin limits automatic GameTest discovery so unrelated addons' development tests do not load their missing optional test dependencies. The build also requires an actual passing GameTest summary, because NeoForge can otherwise exit with status zero after a loading error.

The extended run uses a file URI for logging to accommodate Numismatics' development startup on Windows. The optional test source set and dependencies are excluded from production JARs.

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
