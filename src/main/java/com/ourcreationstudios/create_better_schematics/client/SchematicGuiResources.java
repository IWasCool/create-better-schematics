package com.ourcreationstudios.create_better_schematics.client;

import java.util.Optional;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Uses the installed Create mod's native art without bundling it or requiring a theme pack. */
public final class SchematicGuiResources {
    public static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(
            CreateBetterSchematics.MODID, "textures/gui/widgets.png");
    private static final ResourceLocation CREATE_WIDGETS = ResourceLocation.fromNamespaceAndPath(
            "create", "textures/gui/widgets.png");

    private SchematicGuiResources() {}

    public static Optional<Resource> nativeWidgets(ResourceManager manager) {
        // Packs are ordered from lowest to highest priority. Create's mod resources
        // supply the original atlas before external packs can replace its namespace.
        return manager.listPacks().map(pack -> {
            var stream = pack.getResource(PackType.CLIENT_RESOURCES, CREATE_WIDGETS);
            return stream == null ? null : new Resource(pack, stream);
        }).filter(java.util.Objects::nonNull).findFirst();
    }
}
