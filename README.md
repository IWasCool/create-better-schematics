# Create: Better Schematics

An addon for Create that preserves machine configuration in schematics and adds selective Schematicannon building. Saved files remain ordinary Create `.nbt` schematics.

[Source](https://github.com/IWasCool/create-better-schematics) | [Report an issue](https://github.com/IWasCool/create-better-schematics/issues) | [Compatibility details](COMPATIBILITY.md)

## Installation

Version **0.0.2** provides these builds:

| Build | Minecraft | Requirements | Release JAR |
| --- | --- | --- | --- |
| NeoForge | 1.21.1 | NeoForge 21.1.255+, Create 6.0.10 (before 6.1), Java 21 | `create_better_schematics-0.0.2.jar` |
| Forge companion | 1.20.1 | Forge 47.4.10+, Create 6.0.8 (before 6.1), Java 17+ | `create_better_schematics-forge-1.20.1-0.0.2.jar` |
| Fabric companion | 1.20.1 | Fabric Loader 0.17.2+ (tested 0.17.3), Create Fabric 6.0.8.1 and its normal dependencies, Java 17+ | `create_better_schematics-fabric-1.20.1-0.0.2.jar` |

Put the matching JAR in your instance's `mods` folder. NeoForge has the full feature set. Forge supplies arm/threshold fixes, Factory Gauges, Power Grid wires and shared configuration profiles. Fabric supplies arm/threshold fixes and native belt export. The [addon matrix](COMPATIBILITY.md) is verified on NeoForge; it does not certify every addon on another loader.

Client installation supports local schematic saving and mod inspection on compatible Create servers without Better Schematics. Install it on the destination server for extended placement fixes, and on both client and server for the NeoForge cannon picker and material export. Singleplayer includes a local server and supports the full feature set. Recipe viewers and other integrations are optional.

## Schematic configuration

Save and share schematics through Create as usual. Supported configuration includes:

- Mechanical arm targets and selection modes; threshold-switch values, inversion and stack display.
- List and built-in attribute filters, named/styled items, lore and supported item components.
- Native belt casing data, Factory Gauge panels, recipes, frequencies and connections.
- Power Grid wires and same-world Transmission & Linkage universal-joint rods, with rotated/mirrored endpoints and material costs.
- Selected addon settings, including Create addons, Mekanism, Applied Energistics 2, Storage Drawers and Sophisticated Storage. See the [tested versions and limits](COMPATIBILITY.md#verified-neoforge-features).

Capture a fresh schematic after installing Better Schematics. Include both endpoints and backing blocks for connected gauges or wires. Connections outside the selection are omitted. The mod cannot reconstruct information already missing from an older schematic or configuration that a server never sends to its client.

Copy the `.nbt` file between instances' `schematics` folders. Supported Create 6 transfers between NeoForge 1.21.1 and Forge/Fabric 1.20.1 include compatible item encodings. The companions adapt supported fields in memory without rewriting the shared file. Recipients can load compatible blocks and native settings with stock Create; extended placement behavior may require the matching companion. Both instances still need compatible versions of the blocks and addons used by the build.

This does not convert arbitrary Minecraft versions or every addon format. Inventories, ownership and physical network identities are excluded from added configuration profiles. Hydraulic links between ships and other unsupported networks need reconnecting after placement.

## Schematic Table mod inspection

On NeoForge, select a schematic and hover over the **i** tab at the top-right edge of the Schematic Table. The tooltip lists mods referenced by saved blocks, entities, items, fluids and item components. Installed mods appear green; missing namespaces appear red and sort first. Scroll over the icon for longer lists, or click it to reread the file.

Inspection reads the file directly, so missing blocks and items remain detectable. It works without Better Schematics on the server. Optional Blueprinted and Schematic Helper controls share the compact tab layout. The menu uses the installed Create artwork; no copied textures or resource pack are bundled. New World Core may optionally theme the controls for a dark pack.

## Selective Schematicannon building

Open the cannon's placement settings and click the skip button. Search the inventory-style grid by name or mod ID, then click slots to toggle their block types. Encased shafts, cogwheels and pipes group under their obtainable items; selecting a group affects all represented variants. Red slots are skipped, and amber slots indicate partially skipped groups. Hover for details and exact block counts.

Use **Skip all**, **Build all**, or the native **Skip missing materials** toggle as needed. Arrow keys navigate; Space or Enter toggles a slot. **Apply** saves the choices to that cannon and updates its material checklist. Cancel discards edits. Saved choices survive reloads and affect every block-state variant of the selected types. Already placed or launched blocks are unaffected. Manual Pause remains effective with normal fuel or a Creative Crate; Play resumes construction.

## Material exports

With **JEI 19.53+** installed and EMI absent, click **+** beside Confirm to bookmark required materials with quantities. Applied skips are excluded, the bookmark sidebar opens, and Apply refreshes an imported list. Personal bookmarks remain intact; imported amounts are remembered per world/server. Click + again after changing schematics.

With **EMI** installed, the button instead uses [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) **1.1.0+**, by **LiukRast and swzo**. Better Schematics sends Create clipboard entries to that addon's exporter, which manages favourites and quantities. Without the addon, the button is disabled and explains the requirement. EMI takes priority even when JEI is also installed. Apply refreshes a list exported from that cannon during the current session; click + again after reconnecting or changing schematics.

The server needs Better Schematics to supply cannon materials, but does not need a recipe viewer.

## Building and checking

Use a Java 21 JDK to build all three loaders; Gradle resolves Java 17 for the companions.

```powershell
.\gradlew.bat clean build
.\gradlew.bat -p forge clean build
.\fabric\gradlew.bat -p fabric clean build
py -3 tools/check_release.py --jars build/libs/*.jar forge/build/libs/*.jar fabric/build/libs/*.jar
```

Outputs are in each project's `build/libs` folder. GitHub Actions builds and audits all three loaders. The release checker validates language keys, formatting placeholders, mod versions, mixin references and JAR contents. The current translation is English (`en_us`); other languages use Minecraft's English fallback.

Core GameTests and their fixtures live under `src/testFixtures`, separately from release sources. Run them with `./gradlew -PgameTests runGameTestServer`; compatibility reproduction commands are in [COMPATIBILITY.md](COMPATIBILITY.md#validation-and-reproduction). Test classes, fixtures, downloaded mods and generated evidence are excluded from release JARs.

The mod is **All Rights Reserved**. [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt) preserves the separate NeoForge MDK template notice.
