package com.ourcreationstudios.create_better_schematics.client.emi;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonMaterial;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Delegates to LiukRast and swzo's addon; it alone manages EMI favourites and quantities. */
public final class EmiCreateSchematicsMaterials {
    public static final String ADDON_ID = "emi_create_schematics";
    private static boolean resolved;
    private static Method load;
    private static WeakReference<ClientLevel> sourceLevel = new WeakReference<>(null);
    private static BlockPos sourceCannon;
    private static ItemStack sourceSchematic = ItemStack.EMPTY;

    private EmiCreateSchematicsMaterials() {}

    public static boolean installed() { return ModList.get().isLoaded("emi"); }

    public static boolean addonInstalled() { return ModList.get().isLoaded(ADDON_ID); }

    public static boolean available() {
        if (!installed() || !addonInstalled())
            return false;
        if (!resolved) {
            resolved = true;
            if (ModList.get().getModContainerById(ADDON_ID).orElseThrow().getModInfo().getVersion()
                    .compareTo(new DefaultArtifactVersion("1.1.0")) < 0)
                return false;
            try {
                Method method = Class.forName("net.liukrast.schematicdisplay.clipboard.ClipboardScreenUtils")
                        .getMethod("load", List.class);
                if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == void.class)
                    load = method;
            } catch (ReflectiveOperationException | LinkageError exception) {
                CreateBetterSchematics.LOGGER.warn("EMI: Create Schematics export entry point is unavailable", exception);
            }
        }
        return load != null;
    }

    public static void upload(List<CannonMaterial> materials, BlockPos cannon, ItemStack schematic) {
        if (!available())
            throw new IllegalStateException("EMI: Create Schematics is required to export to EMI");
        // Use the same Create clipboard entries that the addon's own clipboard button accepts.
        var entries = materials.stream().map(material -> new ClipboardEntry(false, Component.empty())
                .displayItem(material.stack().copy(), material.count())).toList();
        try {
            load.invoke(null, List.of(entries));
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("EMI: Create Schematics could not export the material list", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot call EMI: Create Schematics", exception);
        }
        sourceLevel = new WeakReference<>(Minecraft.getInstance().level);
        sourceCannon = cannon.immutable();
        sourceSchematic = schematic.copyWithCount(1);
    }

    public static boolean isSource(BlockPos cannon, ItemStack schematic) {
        return sourceLevel.get() != null && sourceLevel.get() == Minecraft.getInstance().level
                && cannon.equals(sourceCannon) && ItemStack.isSameItemSameComponents(sourceSchematic, schematic);
    }
}
