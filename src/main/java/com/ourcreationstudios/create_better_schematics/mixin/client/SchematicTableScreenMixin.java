package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import com.ourcreationstudios.create_better_schematics.client.SchematicModsScreen;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicTableScreen.class, remap = false)
abstract class SchematicTableScreenMixin extends AbstractSimiContainerScreen<SchematicTableMenu> {
    @Shadow private ScrollInput schematicsArea;
    @Unique private Button betterSchematics$modsTab;
    @Unique private String betterSchematics$selectedFile;

    protected SchematicTableScreenMixin(SchematicTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void betterSchematics$addModsTab(CallbackInfo ci) {
        // Returning from the Mods tab (or resizing) must retain the selected file.
        var files = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
        if (schematicsArea != null && betterSchematics$selectedFile != null)
            for (int index = 0; index < files.size(); index++)
                if (files.get(index).getString().equals(betterSchematics$selectedFile)) {
                    schematicsArea.setState(index);
                    break;
                }
        // Blueprinted adds a column of export/share controls beside the table.
        int tabX = leftPos + (ModList.get().isLoaded("create_blueprinted") ? 226 : 206);
        betterSchematics$modsTab = addRenderableWidget(Button.builder(SchematicModsScreen.text("tab"), button -> {
            String filename = betterSchematics$selection();
            if (filename != null) {
                betterSchematics$selectedFile = filename;
                minecraft.setScreen(new SchematicModsScreen((SchematicTableScreen) (Object) this, filename));
            }
        }).bounds(tabX, topPos, 46, 20).build());
        betterSchematics$updateModsTab();
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$tickModsTab(CallbackInfo ci) {
        betterSchematics$updateModsTab();
    }

    @Unique
    private String betterSchematics$selection() {
        var files = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
        int index = schematicsArea == null ? -1 : schematicsArea.getState();
        return schematicsArea != null && schematicsArea.visible && index >= 0 && index < files.size()
                ? files.get(index).getString() : null;
    }

    @Unique
    private void betterSchematics$updateModsTab() {
        if (betterSchematics$modsTab == null)
            return;
        String filename = betterSchematics$selection();
        if (filename != null)
            betterSchematics$selectedFile = filename;
        betterSchematics$modsTab.active = filename != null;
        betterSchematics$modsTab.setTooltip(Tooltip.create(SchematicModsScreen.text(filename == null ? "select" : "title")));
    }

    @Inject(method = "getExtraAreas", at = @At("RETURN"), cancellable = true, remap = false)
    private void betterSchematics$includeModsTab(CallbackInfoReturnable<List<Rect2i>> cir) {
        if (betterSchematics$modsTab == null)
            return;
        List<Rect2i> areas = new ArrayList<>(cir.getReturnValue());
        areas.add(new Rect2i(betterSchematics$modsTab.getX(), betterSchematics$modsTab.getY(), 46, 20));
        cir.setReturnValue(areas);
    }
}
