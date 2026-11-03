package com.ourcreationstudios.create_better_schematics.schematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Inspects the original file without resolving registries, which would erase missing blocks and items. */
public final class SchematicMods {
    private static final long MAX_NBT_BYTES = 128L * 1024 * 1024;
    private static final Set<String> IDENTIFIER_FIELDS = Set.of(
            "id", "Id", "Item", "item", "FluidName", "fluid", "fluidName",
            "Block", "block", "BlockId", "blockId", "EntityId", "entityId");

    private SchematicMods() {}

    public static List<String> read(Path directory, String filename) throws IOException {
        Path root = directory.toRealPath();
        Path file = root.resolve(filename).normalize();
        if (!filename.endsWith(".nbt") || !root.equals(file.getParent())
                || !file.getFileName().toString().equals(filename)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Select an existing .nbt file in the schematics folder.");
        return find(NbtIo.readCompressed(file, NbtAccounter.create(MAX_NBT_BYTES)));
    }

    public static List<String> find(CompoundTag schematic) throws IOException {
        ListTag size = schematic.getList("size", Tag.TAG_INT);
        if (size.size() != 3 || size.getInt(0) <= 0 || size.getInt(1) <= 0 || size.getInt(2) <= 0
                || !schematic.contains("blocks", Tag.TAG_LIST)
                || !(schematic.contains("palette", Tag.TAG_LIST) || schematic.contains("palettes", Tag.TAG_LIST)))
            throw new IOException("This file is not a Create/Minecraft structure schematic.");

        Set<String> mods = new TreeSet<>();
        BitSet usedStates = new BitSet();
        ListTag palette = schematic.getList("palette", Tag.TAG_COMPOUND);
        ListTag palettes = schematic.getList("palettes", Tag.TAG_LIST);
        boolean singlePalette = schematic.contains("palette", Tag.TAG_LIST);
        int paletteSize = singlePalette ? palette.size() : palettes.getList(0).size();
        ListTag blocks = schematic.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag value : blocks) {
            CompoundTag block = (CompoundTag) value;
            if (!block.contains("state", Tag.TAG_ANY_NUMERIC) || block.getInt("state") < 0)
                throw new IOException("Invalid schematic block state.");
            // Avoid allocating a bit set from an untrusted, potentially enormous state index.
            int state = block.getInt("state");
            if (state >= paletteSize)
                throw new IOException("Schematic block state is outside its palette.");
            usedStates.set(state);
            collect(block.getCompound("nbt"), mods, "nbt", 0);
        }
        if (singlePalette) {
            collectPalette(palette, usedStates, mods);
        } else {
            for (Tag alternate : palettes)
                collectPalette((ListTag) alternate, usedStates, mods);
        }
        collect(schematic.getList("entities", Tag.TAG_COMPOUND), mods, "entities", 0);
        return List.copyOf(mods);
    }

    private static void collectPalette(ListTag palette, BitSet usedStates, Set<String> mods) throws IOException {
        if (usedStates.length() > palette.size())
            throw new IOException("Schematic block state is outside its palette.");
        for (int index = usedStates.nextSetBit(0); index >= 0; index = usedStates.nextSetBit(index + 1))
            add(palette.getCompound(index).getString("Name"), mods);
    }

    private static void collect(Tag tag, Set<String> mods, String context, int depth) throws IOException {
        if (depth > 128)
            throw new IOException("Schematic data is nested too deeply to inspect.");
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                if (value instanceof StringTag string && (IDENTIFIER_FIELDS.contains(key)
                        || key.equals("Name") && (context.equals("BlockState") || context.equals("block_state"))))
                    add(string.getAsString(), mods);
                // Custom components themselves require their registering mod, even with primitive values.
                if (context.equals("components"))
                    add(key.startsWith("!") ? key.substring(1) : key, mods);
                collect(value, mods, key, depth + 1);
            }
        } else if (tag instanceof ListTag list) {
            for (Tag value : list)
                collect(value, mods, context, depth + 1);
        }
    }

    private static void add(String value, Set<String> mods) {
        if (value.indexOf(':') < 1)
            return;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id != null)
            mods.add(id.getNamespace());
    }
}
