# Changelog

## 0.0.11 — NeoForge 1.21.1

- Group normal, andesite-encased and brass-encased shafts/cogwheels by their actual obtainable item in the picker, with combined block counts. Encased/glass pipes use Fluid Pipe and belts use Mechanical Belt.
- Retain original block IDs when applying a grouped skip, so all represented variants are excluded from construction and the exported JEI/EMI material list. Existing partial variant skips remain visible and are preserved until toggled.
- Keep using Create's native item requirements for actual exported quantities, including the belt connector/pulley costs and composite blocks.
- Remove routine extra help lines from the added buttons. Missing EMI addon/server requirements still explain a disabled export button.
- Restore the original inventory frame and search-field textures. Keep the centered layout and grey only behind the scrollbar.

This is a NeoForge client update; the existing server material and skip channels remain compatible. EMI export still exclusively requires and delegates to EMI: Create Schematics. Forge/Fabric companions remain at 0.0.7.

Validation: 197 base GameTests; a live JEI 19.70.0.460 check with encased shafts/cogs/pipes, grouped skips, restored materials, button tooltips and a scrolling 71-group picker; and live EMI 1.1.24 / EMI: Create Schematics 1.1.0 export and missing-addon checks with JEI also installed. The restored menu textures were inspected in the rendered screen.

## 0.0.10 — NeoForge 1.21.1

- Show **Send materials to EMI** when EMI is installed, with a tooltip requiring and crediting **EMI: Create Schematics** by LiukRast and swzo.
- Delegate EMI exports exclusively to the installed addon's clipboard exporter, including refreshes after applying block skips. Requires EMI: Create Schematics 1.1.0 or newer. The addon remains a separate download: [Modrinth](https://modrinth.com/mod/emi-create-schematics).
- Disable the button when the required addon is missing or incompatible. EMI takes priority even with JEI installed; the JEI exporter and quantity mixins stay disabled in that setup.
- Retain the existing JEI material export when EMI is absent. No server protocol change is required; Better Schematics 0.0.7 or newer supplies the materials.

The EMI addon owns favourites, quantity rendering, replacement and persistence. Better Schematics only supplies the current material list and invokes its export method; it does not include or reproduce the addon's implementation. After reconnecting, click + to select the source cannon for automatic skip refreshes again. Forge/Fabric companions remain at 0.0.7.

Validation: 196 base GameTests; live EMI 1.1.24 / EMI: Create Schematics 1.1.0 export, skip, empty-list and restore checks with and without JEI; a missing-addon check with EMI and JEI together; and the existing live JEI 19.70.0.460 regression check.

## 0.0.9 — NeoForge 1.21.1

- Automatically update an imported JEI build list when skip choices are applied. Skipped materials disappear and unskipped materials return, including after Skip all.
- Remember the source cannon and schematic with the saved material list. Applying skips to another cannon leaves the imported build list alone.
- Remove the extra required-material tooltip line; bookmark icons retain their quantities.
- Move the + and Cancel buttons away from the footer divider and adjust the cannon settings to fit.
- Centre the slot grid and scrollbar together inside the Schematic blocks panel. Use grey backgrounds for the scrollbar and search field.

Requires the same Minecraft, Create, NeoForge, and optional JEI versions as 0.0.8. The skip-sync change is on the NeoForge client and works with the existing server material channels. Forge/Fabric companions remain at 0.0.7.

Validation: 196 base GameTests and a live JEI 19.70.0.460 test covering Apply, Skip all, Build all, saved/legacy material lists, personal bookmarks, button spacing, and a scrolling 67-type block table.

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
