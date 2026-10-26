package com.ourcreationstudios.create_better_schematics.client.jei;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** This facade has no JEI class references, so the mod stays optional on both sides. */
public final class JeiMaterials {
    private static final MaterialBookmarkList quantities = new MaterialBookmarkList();
    private static Consumer<List<CannonMaterial>> importer;
    private static Path file;
    private static BlockPos sourceCannon;
    private static ItemStack sourceSchematic = ItemStack.EMPTY;
    private static boolean imported;

    private JeiMaterials() {}

    public static boolean supportedVersion() {
        return ModList.get().getModContainerById("jei")
                .map(mod -> mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion("19.53.0")) >= 0)
                .orElse(false);
    }

    public static boolean available() { return importer != null; }

    public static void start(Consumer<List<CannonMaterial>> receiver) {
        stop();
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;
        String scope;
        if (minecraft.getSingleplayerServer() != null)
            scope = "local:" + minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        else if (minecraft.getCurrentServer() != null)
            scope = "server:" + minecraft.getCurrentServer().ip;
        else
            return;
        String id = UUID.nameUUIDFromBytes(scope.getBytes(StandardCharsets.UTF_8)).toString();
        file = minecraft.gameDirectory.toPath().resolve("config/create_better_schematics/jei-materials/" + id + ".nbt");
        if (Files.isRegularFile(file)) {
            try {
                CompoundTag data = NbtIo.readCompressed(file, NbtAccounter.create(16 * 1024 * 1024L));
                quantities.read(data, minecraft.level.registryAccess());
                imported = data.getInt("Version") == 1;
                sourceCannon = NbtUtils.readBlockPos(data, "Cannon").orElse(null);
                if (data.contains("Schematic"))
                    sourceSchematic = ItemStack.parse(minecraft.level.registryAccess(), data.getCompound("Schematic"))
                            .orElse(ItemStack.EMPTY);
            } catch (IOException | RuntimeException exception) {
                CreateBetterSchematics.LOGGER.warn("Cannot restore JEI material quantities", exception);
            }
        }
        importer = receiver;
    }

    public static void stop() {
        importer = null;
        file = null;
        quantities.clear();
        sourceCannon = null;
        sourceSchematic = ItemStack.EMPTY;
        imported = false;
        com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient.clearPending();
    }

    public static void upload(List<CannonMaterial> materials, BlockPos cannon, ItemStack schematic) {
        if (importer == null)
            throw new IllegalStateException("JEI bookmarks are not available");
        importer.accept(materials);
        sourceCannon = cannon.immutable();
        sourceSchematic = schematic.copyWithCount(1);
        imported = true;
        save();
    }

    public static boolean isSource(BlockPos cannon, ItemStack schematic) {
        // Older sidecars did not record their source. Adopt the first applied cannon once.
        return sourceCannon == null ? imported
                : sourceCannon.equals(cannon) && ItemStack.isSameItemSameComponents(sourceSchematic, schematic);
    }

    public static MaterialBookmarkList quantities() { return quantities; }

    public static int count(ItemStack stack) { return available() ? quantities.count(stack) : 0; }

    public static void save() {
        if (file == null || Minecraft.getInstance().level == null)
            return;
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "materials-", ".tmp");
            CompoundTag data = quantities.write(Minecraft.getInstance().level.registryAccess());
            if (sourceCannon != null && !sourceSchematic.isEmpty()) {
                data.put("Cannon", NbtUtils.writeBlockPos(sourceCannon));
                data.put("Schematic", sourceSchematic.save(Minecraft.getInstance().level.registryAccess()));
            }
            NbtIo.writeCompressed(data, temporary);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            CreateBetterSchematics.LOGGER.warn("Cannot save JEI material quantities", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) {}
            }
        }
    }
}
