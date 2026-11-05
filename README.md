# Create: Better Schematics

Preserves machine configuration in Create schematics and adds selective Schematicannon building.
Saved files remain ordinary Create `.nbt` schematics.

**Public source:** [IWasCool/create-better-schematics](https://github.com/IWasCool/create-better-schematics).
Mod version: **1.0.0**.

## Features

- Copy supported machine settings, mechanical arm targets, filters, belt casings and Factory Gauge connections.
- Preserve selected addon configuration, Power Grid wires and same-world Transmission & Linkage universal-joint rods.
- Inspect a schematic's installed and missing mods from the Schematic Table's **i** tooltip. Scroll over the icon for longer lists; click to refresh.
- Choose block types to skip in a searchable Schematicannon picker. Encased variants group under their obtainable items. Apply saves choices to the cannon and updates required materials.
- Send material quantities to JEI, or delegate EMI exports to EMI: Create Schematics. Applying skips refreshes an imported list while preserving personal bookmarks.

The NeoForge build includes all features. The Forge companion provides arm/threshold fixes, Factory Gauges, Power Grid wires and shared configuration profiles. The Fabric companion provides arm/threshold fixes and native belt export. See [compatibility details](COMPATIBILITY.md) for supported addon settings and transfer limits.

## Installation

| Build | Minecraft | Requirements |
| --- | --- | --- |
| NeoForge | 1.21.1 | NeoForge 21.1.255+, Create 6.0.10 (before 6.1), Java 21 |
| Forge companion | 1.20.1 | Forge 47.4.10+, Create 6.0.8 (before 6.1), Java 17+ |
| Fabric companion | 1.20.1 | Fabric Loader 0.17.2+, Create Fabric 6.0.8.1 and its normal dependencies, Java 17+ |

Place the matching JAR in your instance's `mods` folder alongside Create. Client installation supports local schematic saving and mod inspection on compatible Create servers without Better Schematics. Extended placement fixes need the mod on the destination server. The NeoForge skip picker and material export need it on both client and server. Singleplayer supports the full feature set.

Capture a fresh schematic after installation. Include connected endpoints and their backing blocks when copying gauges, wires or rods. Copy the `.nbt` file between instances' `schematics` folders as usual. Both instances still need compatible versions of the blocks and addons used by the build. This does not convert arbitrary Minecraft versions or every addon format.

## Optional integrations

**JEI 19.53+**, with EMI absent: click **+** beside the cannon's Confirm button to bookmark required materials and quantities. Applied skips are excluded. Click + again after changing schematics.

**EMI:** the materials button requires [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) **1.1.0+**, by **LiukRast and swzo**. Its exporter manages favourites and quantities. EMI takes priority over JEI; without the addon, the button explains the missing requirement. Click + again after reconnecting or changing schematics. The server needs Better Schematics but no recipe viewer.

Blueprinted and Schematic Helper controls share the Schematic Table's compact tabs. Menus use the installed Create/Minecraft artwork. New World Core may optionally theme them for a dark resource pack.

**Language:** English (`en_us`), with Minecraft's English fallback for other languages.

## Building

Use a Java 21 JDK. Gradle resolves Java 17 for the companions.

```powershell
.\gradlew.bat build
.\gradlew.bat -p forge build
.\fabric\gradlew.bat -p fabric build
```

Upload `build/libs/create_better_schematics-1.0.0.jar` for NeoForge. Companion JARs are in `forge/build/libs/` and `fabric/build/libs/`. Language keys and formatting arguments are checked during the NeoForge build; `./gradlew validateTranslations` runs that check separately.

Report problems through [GitHub Issues](https://github.com/IWasCool/create-better-schematics/issues), including Minecraft/Create versions and the affected schematic or relevant log.

**License:** All Rights Reserved. [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt) preserves the separate NeoForge MDK template notice.
