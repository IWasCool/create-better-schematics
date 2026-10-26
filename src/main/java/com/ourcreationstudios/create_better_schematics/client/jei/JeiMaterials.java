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
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** This facade has no JEI class references, so the mod stays optional on both sides. */
public final class JeiMaterials {
    private static final MaterialBookmarkList quantities = new MaterialBookmarkList();
    private static Consumer<List<CannonMaterial>> importer;
    private static Path file;

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
                quantities.read(NbtIo.readCompressed(file, NbtAccounter.create(16 * 1024 * 1024L)), minecraft.level.registryAccess());
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
        com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient.clearPending();
    }

    public static void upload(List<CannonMaterial> materials) {
        if (importer == null)
            throw new IllegalStateException("JEI bookmarks are not available");
        importer.accept(materials);
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
            NbtIo.writeCompressed(quantities.write(Minecraft.getInstance().level.registryAccess()), temporary);
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
