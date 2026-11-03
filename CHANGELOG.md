# Changelog

## 0.0.1 — Initial release

- Preserve supported Create machine settings, filters, belt casings, Factory Gauge connections and addon configuration in ordinary `.nbt` schematics. Include compatible item encodings for supported Create 6 transfers between NeoForge 1.21.1 and the Forge/Fabric 1.20.1 companions.
- Add a searchable Schematicannon block picker with saved skip choices, Skip all/Build all controls and keyboard navigation. Group encased shafts, cogwheels and pipes under their obtainable items while keeping the original block types for construction and material calculations.
- Export required materials to JEI 19.53 or newer when EMI is absent, preserving personal bookmarks and displaying quantities. Applying block skips refreshes the imported build list.
- When EMI is installed, delegate material exports exclusively to the separate [EMI: Create Schematics](https://modrinth.com/mod/emi-create-schematics) addon, version 1.1.0 or newer, by **LiukRast and swzo**. Disable export with an explanation when the addon or server support is unavailable.
- Copy supported Power Grid wires and in-world Create Aeronautics: Transmission & Linkage universal-joint rods with transformed endpoints and material payment. Suppress Transmission & Linkage's repeated Toolgun recommendation. Hydraulic links between ships still require reconnecting after placement.
- Allow local schematic saving on compatible Create servers without Better Schematics. Extended cannon controls and placement fixes require the addon on the relevant server. Keep native block-state IDs and the shared structure format unchanged.
- Use Create's existing menu textures and Minecraft slots/search field. Exclude test classes, fixtures and the historical explicit schematic conversion exporter from release JARs.

NeoForge requires Minecraft 1.21.1, NeoForge 21.1.255 or newer, and Create 6.0.10 (before 6.1). The Forge companion targets Minecraft 1.20.1 / Create 6.0.8; the Fabric companion targets Minecraft 1.20.1 / Create Fabric 6.0.8.1. All three mod builds use version 0.0.1. Optional addons and their tested limits are listed in [COMPATIBILITY.md](COMPATIBILITY.md).

Validation: the cleanup regression run passed 197 required NeoForge GameTests. Earlier local client, addon and companion validation is recorded in the compatibility notes; these checks were performed during development of this initial release.
