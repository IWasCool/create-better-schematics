# Changelog

## 0.0.8 — NeoForge 1.21.1

- Copy connected Create Aeronautics: Transmission & Linkage universal joints, including andesite and brass rods, rotation, mirroring, and saved cannon projectiles. Include both endpoints in the schematic. Each connection requires one rod.
- Suppress Transmission & Linkage's repeated recommendation to install Create Aeronautics: Toolgun.
- Send required Schematicannon materials to JEI with the **+** button beside Confirm. Imported bookmarks display quantities, exclude skipped blocks, and preserve personal bookmarks when refreshed. Verified with JEI 19.70.0.460; requires JEI 19.53 or newer.
- Allow local schematic saving when joining a compatible Create server without Better Schematics. Extended cannon controls activate when the server supports them.
- Preserve belt casings with Create's native NBT and improve shared item/filter data for the Forge and Fabric 1.20.1 companions.
- Exclude test classes and fixtures from the release jar.

Requires Minecraft 1.21.1, NeoForge 21.1.255 or newer, and Create 6.0.10 (before 6.1). JEI and Transmission & Linkage are optional. Rod support was verified with Transmission & Linkage 0.2.8. Hydraulic links between ships still require reconnecting after placement.

Replace the previous Better Schematics jar and restart Minecraft. When upgrading a server from 0.0.5 or earlier, update the participating clients too. JEI material export requires Better Schematics 0.0.7 or newer on the server; singleplayer includes this automatically. Forge/Fabric 1.20.1 companions remain at 0.0.7 and do not include the NeoForge JEI button or rod adapter.

Validation: 405 required GameTests with the expanded addon set, 196 without optional content addons, a live JEI 19.70.0.460 import/refresh test, and four cancelled Toolgun recommendation handler calls. See [COMPATIBILITY.md](COMPATIBILITY.md) for the tested scope.
