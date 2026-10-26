# Addon schematic compatibility

## Obtainable items in the picker (NeoForge 0.0.11)

The picker now uses Create's single-item requirements to group encased shafts, small/large cogwheels, encased pipes and glass pipes under their obtainable item. Counts combine the represented schematic blocks. Belts use the Mechanical Belt item name/icon. Actual exported quantities continue to use the full native requirements, including belt connectors, pulley shafts and composite requirements such as a shaft plus metal girder; a decorative casing does not become a separate hidden item requirement.

Only the client presentation is grouped. Apply still sends each represented original block type through the existing validated skip protocol. Choosing a group skips/unskips all its variants, and the active JEI or delegated EMI export refreshes as before. Existing selections affecting only some variants stay intact and show an amber partial state. Search accepts both the obtainable item and the original block names/IDs. Composite or unavailable addon requirements keep a separate block row rather than guessing their item costs.

Working added buttons show their action name without routine extra help lines. Disabled exports retain their missing dependency/server explanation, including the EMI: Create Schematics requirement. The original Create inventory and Minecraft search textures are restored; only the scrollbar track has a grey fill. The grid and scrollbar remain centered together, with the previously fixed footer spacing.

October 26, 2026 validation: 197 base GameTests passed. A debugger probe confirmed that the real Create requirements produce obtainable shafts, cogs, pipes and belt connectors rather than internal encased items, retaining the additional girder cost for a composite shaft. A live JEI 19.70.0.460 test verified three variants merging into each shaft/cog/pipe group, their exact exported quantities, excluding all shaft variants after Apply, restoring them after Build all, single-line working-button tooltips and a scrolling 71-group picker. A live EMI 1.1.24 / EMI: Create Schematics 1.1.0 test passed the corresponding export and grouped-skip checks with JEI also installed. The missing-addon setup still disabled export and named the requirement. The rendered picker was inspected to verify the original frame/search textures and grey scrollbar track. New tests and generated evidence remain local.

## EMI material export (NeoForge 0.0.10)

When EMI is installed, the cannon's + button is labelled **Send materials to EMI**. It requires the separate [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) addon by **LiukRast and swzo**, version 1.1.0 or newer. A small optional bridge turns the server's material list into Create clipboard entries and invokes the addon's public `ClipboardScreenUtils.load` method, the same method its clipboard button uses. All EMI favourites, quantity display, replacement and persistence belong to that addon. Better Schematics includes no EMI exporter implementation, EMI API dependency or bundled addon.

EMI takes priority over JEI. If the addon is absent or its export method is unavailable, the button stays disabled with an explanatory tooltip; JEI does not provide a fallback. JEI bookmark imports and quantity mixins are disabled whenever EMI is loaded. With EMI absent, the existing JEI integration below remains available.

The server still needs Better Schematics 0.0.7 or newer to supply cannon materials; neither EMI nor its addon is required on the server. Applied skips are excluded. After exporting, Apply refreshes that cannon's list using the same addon, including an empty list after Skip all. Source selection is held only for the current client level/session: after reconnecting or changing schematics, click + again. The addon's existing clipboard button remains available.

October 26, 2026 validation used the released EMI 1.1.24+1.21.1+neoforge and EMI: Create Schematics 1.1.0 jars. Live client checks passed with EMI alone plus the addon and with EMI, JEI 19.70.0.460 and the addon together: the real button imported 130 stone through the addon, excluded applied skipped blocks, refreshed after Apply, cleared after Skip all, restored after Build all, and retained a personal diamond favourite. With EMI and JEI installed but the addon absent, the button remained disabled, its tooltip named and credited the required addon, and a direct request did not start an export. The separate JEI-only live regression and all 196 base GameTests passed. Test code and downloaded test mods remain excluded from GitHub and the production jar.

NeoForge 0.0.9 refreshes the active imported JEI build list after the server accepts Apply in the skip picker. Skipping removes imported entries and unskipping restores them, including after Skip all emptied the list. The source cannon and schematic are remembered per world/server, so editing another cannon does not replace an unrelated build list. Older sidecars adopt the first applied cannon. Required amounts remain on bookmark icons without an added tooltip line. The footer controls are spaced away from the divider, and the centred slots/scrollbar share a grey panel and grey search field.

The 0.0.9 live client check passed on JEI 19.70.0.460 using the real skip picker and Apply packets. It covered removing a skipped material, restoring an unskipped material, Skip all, Build all after an empty list, restoring the saved source, migrating an empty older sidecar, retaining personal bookmarks, and scrolling a 67-type schematic. Button and grid positions were checked in the live menu and screenshots. All 196 base GameTests passed, and the production jar built from a separate checkout containing only the selected source changes.

Version 0.0.8 Transmission & Linkage validation, October 26, 2026: 405 required tests passed with the expanded addon set, including 19 new universal-joint rod tests. The real client also cancelled the Toolgun recommendation handler across four simulated logins. The earlier JEI materials, optional-installation and native-sharing validation is retained below; coverage remains specific to the listed versions and features.

## Optional installation

NeoForge 0.0.8 additionally supports Transmission & Linkage 0.2.8 universal-joint rods whose two reciprocal endpoints are included in the schematic and have no ship/sublevel references. Both andesite and brass rods retain their material and rest length through rotation and mirroring. A pending connection waits for both copied endpoints and survives save/reload and cannon projectiles; it never targets an original endpoint. Cannon checklists charge for one rod, at a designated endpoint, before the pair can connect. Better Schematics is needed on the side performing placement for this extension. Transmission & Linkage remains optional.

On clients with Better Schematics, Transmission & Linkage's Toolgun login recommendation is suppressed at its own handler. Other chat messages are unaffected. This does not add Toolgun's ship-blueprint or assembly features: hydraulic links, cross-sublevel rods and hinge assemblies still require the relevant Aeronautics workflow.

The NeoForge client can join a compatible Create server without Better Schematics. All five cannon payloads are optional, and the client checks the negotiated channels before replacing Create's skip-missing button or opening the extended picker. When the addon is absent on the server, saving remains local and Create's normal controls remain available. Installing on both sides enables the existing cannon picker and server placement fixes; singleplayer retains those features through its integrated server.

The addon no longer modifies belt block-state definitions or registers a new structure processor in a synchronized registry. Placement processors are attached directly to Create's in-memory placement settings and are never serialized into shared files. Native belt `Casing` NBT replaces the old addon property; old schematic palettes are migrated into that NBT. Editors that discard NBT cannot preserve the material using stock Create's boolean casing property alone. Existing worlds retain the native casing data already written by Create.

The game tests negotiate the actual registered payloads with each peer missing the addon and with both peers present. A separate stock-Create process compares every network block-state ID against the addon-loaded process. These are automated protocol/registry checks, not a live remote-client connection test. Older Better Schematics builds that changed belt states or required the old protocol must be updated together with their clients.

Client-only capture is limited to loaded chunks and data already synchronized by the server. Server-only addon settings cannot be recovered by a local exporter. Missing blocks/mods and unimplemented Minecraft/Create versions remain unsupported.

## JEI material bookmarks (NeoForge)

The cannon menu has a + button next to Confirm when JEI 19.53 or newer is installed and EMI is absent. The client uses JEI's ingredient bookmark API; JEI's API-only runtime for other recipe viewers does not activate this integration. Older JEI builds and installations without either supported recipe viewer retain the normal cannon screen.

Better Schematics 0.0.7 is required on the server to supply the current cannon's materials. JEI itself is only needed on the client. The two new request/reply channels remain optional, so joining a server without this feature continues to work. The button is disabled with an explanatory tooltip when the server lacks these channels.

A separate read-only schematic printer calculates Create's item requirements using the cannon's current placement predicate, including applied skipped block types, replacement options, existing target blocks, double slabs, belt parts and addon item requirements. Required component-specific items stay separate. Quantities are independent of Minecraft's stack limit. Supplies already in attached inventories are not subtracted. If target chunks are unavailable or the list exceeds its bounded payload, the current JEI list is left intact instead of importing an incomplete list.

The import uses ordinary JEI item bookmarks, so recipe and usage lookups still work. A nonempty import reveals both JEI and its bookmark sidebar if either was hidden. Its count overlay applies only to JEI bookmarks. A local sidecar under `config/create_better_schematics/jei-materials` remembers quantities, which bookmarks the importer created, and the source cannon/schematic, separately for each world/server. Applying skips to that imported build refreshes it automatically; changing schematics still requires clicking +. Refreshing replaces previous imported amounts and removes obsolete imported bookmarks, while preserving unrelated or preexisting personal bookmarks. Shared schematic files are unchanged.

Four added game tests check the real cannon material/skip path, stale and closed menu rejection, packet round-trips, amounts over 64, component-specific ingredients, persistence, and refreshing while retaining personal bookmarks. An isolated JEI client test drives the real menu button and packet handlers, checks a 130-item bookmark, changes applied skips, and captures the actual sidebar.

October 26, 2026: Better Schematics 0.0.8 passed the live client test with JEI **19.70.0.460**, using a jar whose SHA-256 matches the installed modpack copy. Importing revealed hidden bookmarks, displayed the 130-item amount, refreshed changed skips, and retained personal bookmarks. All 196 base GameTests also passed with this JEI release loaded. The test result records the actual loaded JEI version. Compilation still targets the 19.53 API. Local client test files and generated results are excluded from this source update and release JAR.

## Ordinary shared schematics and automatic compatibility

Files saved with Better Schematics remain standard Create/Minecraft `.nbt` structures. The native `DataVersion`, palette, modern item components, blocks and entities are retained. The optional `CreateBetterSchematics` compound records `FormatVersion = 1`, `SourceDataVersion` and `SourceLoader`. Compatibility item fields supplement the native fields rather than replacing them. Legacy exports also include equivalent native modern count/components for supported item data, list filters and built-in attribute filters. No Better Schematics block, entity, block-state property or custom structure format is required to read the file.

Same-version recipients can use Create's normal schematic placement without Better Schematics. A separate test mod runs with Better Schematics and its mixins entirely absent, loads fresh enhanced exports, and checks mechanical-arm targets, native threshold values, inversion, stack display, named deny-list filters and belt casing materials. A stock Schematicannon flight preserves the same threshold/filter settings. Unknown Power Grid wire entities are skipped when that addon is absent, and the supported machines still build. Separate stock-Create recipients also read real exports in both directions between Forge 1.20.1 and NeoForge 1.21.1, including list filters and built-in attribute predicates without any importer installed.

With a matching companion installed, the same native file is adapted automatically in memory on Forge or Fabric 1.20.1 / Create 6. This includes legacy arm positions and filter aliases, conventional tag namespaces, and explicit millibucket/droplet conversions. The source file is never rewritten. Unknown fluid units are preserved rather than guessed; older fluid thresholds may need checking when moving between loaders. New format/version metadata provides a migration point for future ports; it does not certify unimplemented Minecraft or Create versions.

| Environment | Tested versions | Result | Evidence |
| --- | --- | --- | --- |
| NeoForge source and regressions | Minecraft 1.21.1, Create 6.0.10, NeoForge 21.1.255 | 196 required tests passed | `run/logs/latest.log` |
| Create-only recipient | Same Minecraft/Create, Better Schematics absent | Six native sharing, cross-version and registry checks | `build/native-compatibility/run/logs/latest.log` |
| Forge recipient | Minecraft 1.20.1, Create 6.0.8, Forge 47.4.10 | Seven required tests passed, including native modern-component export | `forge/run-gametest/logs/latest.log` |
| Stock Forge recipient | Minecraft 1.20.1, Create 6.0.8, Better Schematics absent | Two required tests passed using fresh NeoForge machine and belt exports | `forge/build/native-compatibility/run/logs/latest.log` |
| Fabric recipient | Minecraft 1.20.1, Create Fabric 6.0.8.1+build.1744, Fabric Loader 0.17.3, Fabric API 0.92.6 | Five required tests passed, including direct native-file and fluid-unit imports | `fabric/run-porter-gametest/build/gametest-results.xml` |

Tests exercise native and historical converted files, real schematic printers, rotation/mirroring, serialized cannon projectiles, delayed arm targets, named/styled filter data, source-file conservation and world reloads. A regression guard checks that serializing a threshold switch never queries its observed inventory or loads a neighboring chunk. Test-only code and fixtures are excluded from the production companions; the Create-only test mod is excluded from the NeoForge JAR.

Extended placement behavior still requires the relevant implementation. Transformed wire endpoints, recreated junction identities, late arm-target retries and relocated gauge networks cannot be supplied by data alone. Recipients need compatible blocks and mods; unsupported or missing blocks cannot be supplied by the schematic. Core data readability does not certify every addon configuration without Better Schematics.

The full bundled Create Fabric suite was previously checked: `TestFluids.thresholdSwitch` and `TestItems.attributeFilters` fail both with and without the companion. The companion-disabled baseline ran 61 tests and reproduced both failures (`build/porter-fabric-baseline-results.xml`, `build/porter-fabric-baseline.log`). The normal companion task isolates its five compatibility tests; it does not claim those upstream failures are fixed.

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
| Create Aeronautics: Transmission & Linkage | 0.2.8 | Damping-bearing resistance; hydraulic and hinge-head stretch resistance, free mode, expected length, redstone minimum/maximum lengths and return force; brass hinge angle limits. Both universal-joint blocks support selected in-world andesite/brass rod connections, including transforms, delayed endpoints, reloads, saved cannon projectiles and one-rod material payment. |
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

Nineteen additional universal-joint tests cover both rod materials across all three mirror choices with rotation, creative placement, delayed saved projectiles and paid working cannons. Pending connections survive a save/reload before the second endpoint arrives; clipped selections remain disconnected and source rods stay intact. The material adapter wraps the completed native requirement calculation so Azimuth's return hook cannot bypass the rod charge or erase other addons' requirements.

The four factory/storage mods were selected from established projects with millions of downloads on their linked official Modrinth pages. Their exact artifacts and the required GuideME 21.1.19 / Sophisticated Core 1.5.17.2426 libraries are pinned with SHA-512 hashes in `tools/popular-mod-test-lock.json`. They remain optional at runtime.

## Other audited content

Aeronautics/Offroad, Aeroworks, Integrated Farming, Central Kitchen, Create Deco, Create Nuclear, Create Dragons Plus, Create BB, Contraption Terminals, Vibrant Vaults, Power Loader, Teleporters, Hypertubes, Tracks, Linear Bearing, Coasters, Struts, Rolling Tones, Trading Floor, Garnished, Interiors and the installed schematic-helper/integration mods were inspected or included in the content loading run. Decorative blocks and settings already handled by Create's block-state/safe-NBT APIs continue to use those paths. Loading successfully is not a feature-complete certification of each addon.

Metalwork 3.0.0 includes an unused `GenericDisplayBlockEntity` library class with no registered block or other class reference in that JAR. It is not advertised as a tested display feature. Its registered decorative content uses the ordinary block-state path.

## Remaining limits

- Physical graphs need their own relocation and material adapters. Power Grid wires and Create gauges have those adapters. This release does not add adapters for Crafts & Additions wire networks, Struts/Coaster connections, Hypertube graphs, ship/sublevel attachments, flight/navigation routes, or Radar target references.
- Profiles preserve selected settings and supplement the addon's native safe data. They do not add undeclared inventories, energy, processing progress, ownership, bank accounts, or physical network identities. Existing native safe-NBT writers remain authoritative for the data they already supply.
- Multi-block assembly, directional motion, every Copycats multipart variant, huge Diesel Generator assemblies, MIDI assets/roll contents, and optional integrations such as Apotheosis need their own feature-specific validation. The table certifies the listed configuration transfers.
- World-specific destinations and active targets are excluded from new profiles. Reconnect or configure those after placement unless a dedicated adapter is listed.
- Transmission & Linkage copies block settings and in-world universal-joint rods with both endpoints selected. Hydraulic connections, cross-sublevel rods, creative/giant rod modes and settings, runtime forces/strain, generated stress/speed, and ship/hinge sublevel identities remain outside this adapter. Supply and reconnect those rods separately. Version 0.2.8's hydraulic linking action reinitializes expected length and redstone length limits; reapply those after reconnecting. Hinge angle limits are preserved numerically; mirrored physical motion across ship/sublevel assemblies is outside these configuration tests.
- The new AE2 coverage is for the listed full-block machines; multipart cable parts and paid encoded-pattern/upgrade items require separate adapters. Storage Drawers compacting/framed variants and Sophisticated Storage linked-inventory networks are outside this configuration certification. Limited barrels retain the addon's required exact item-data matching mode.
- The NeoForge addon matrix does not certify the same addon on Forge 1.20.1. A destination still needs compatible versions of the blocks and mods used by the schematic. The companion's established gauge and Power Grid transfers remain tested in both directions.

## Validation and reproduction

| Run | Latest result | Evidence |
| --- | --- | --- |
| NeoForge 0.0.8 with Transmission & Linkage 0.2.8 and the expanded content set | 405 required tests passed | `build/transmission-rods-0.0.8-expanded.log` |
| NeoForge 0.0.8 without optional addons | 196 required tests passed; production JAR built | `build/transmission-rods-0.0.8-base.log` |
| NeoForge 0.0.8 with Transmission & Linkage and its required bundled content | 289 required tests passed | `build/transmission-rods-0.0.8-minimal.log` |
| NeoForge client with Transmission & Linkage 0.2.8 | Four recommendation-handler calls cancelled | `build/transmission-notice-client/result.json` |
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

To exercise the real optional Toolgun-notice target, use `-PcompatTests -PtransmissionClientTest -PcompatModsDir=build/transmission-linkage-test-mods runTransmissionNoticeClientTest`. The supplied directory needs Transmission & Linkage 0.2.8, Aeronautics 1.3.2, Sable 2.0.5 and their bundled dependencies; the client additionally needs Flywheel 1.0.6 (bundled with the installed Create distribution). This isolated client task uses serial mod initialization and exits after writing its result JSON; it does not join or modify a player's world.

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
