# Changelog

## 0.0.2

- Replace the NeoForge Schematic Table's separate Mods screen with an **i** tooltip listing installed and missing mods. Scroll over the icon for longer lists; click to reread the selected file.
- Arrange the table controls as compact tabs, including optional Blueprinted and Schematic Helper controls. Keep tab frames behind the menu and reserve space in recipe viewers.
- Use the installed Create artwork without bundling textures. Use Minecraft's creative-inventory scrollbar, separately centered beside the picker frame.
- Honor manual Schematicannon pauses with a Creative Crate or normal fuel, while preserving Play and automatic missing-material recovery.
- Move core GameTests and fixtures out of production sources. Remove obsolete build targets and development-only documentation, and audit translations, mixins and release contents in CI.
- Use version 0.0.2 for all three loader builds.

## 0.0.1

- Preserve supported machine settings, filters, belt casings, Factory Gauge connections and addon configuration in ordinary Create `.nbt` schematics.
- Include compatible item encodings for supported Create 6 transfers between NeoForge 1.21.1 and the Forge/Fabric 1.20.1 companions.
- Add a searchable NeoForge Schematic Table Mods screen and a Schematicannon block picker with saved skip choices, grouping and keyboard navigation.
- Export material quantities to JEI while preserving personal bookmarks; refresh imported lists after applying skips.
- Delegate EMI exports to [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) 1.1.0+, by LiukRast and swzo.
- Copy supported Power Grid wires and same-world Transmission & Linkage universal-joint rods with transformed endpoints and material payment. Suppress Transmission & Linkage 0.2.8's repeated Toolgun recommendation.
- Support client-side schematic saving on compatible servers without Better Schematics. Extended cannon controls and placement fixes require the addon on the relevant server.

Requirements, verified addon versions and remaining limits are documented in [COMPATIBILITY.md](COMPATIBILITY.md).
