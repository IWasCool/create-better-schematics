package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import com.ourcreationstudios.create_better_schematics.client.SchematicPorterScreen;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicTableScreen.class, remap = false)
abstract class SchematicTableScreenMixin extends AbstractSimiContainerScreen<SchematicTableMenu> {
    @Shadow private ScrollInput schematicsArea;
    @Shadow private IconButton refreshButton;
    @Shadow private List<Rect2i> extraAreas;

    protected SchematicTableScreenMixin(SchematicTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void betterSchematics$addPorter(CallbackInfo ci) {
        // Use the helper's import slot, or the next button above it when occupied.
        boolean hasImportButton = ModList.get().isLoaded("createschematichelper");
        int y = topPos + 13 - (hasImportButton ? 21 : 0);
        IconButton porter = new IconButton(leftPos + 208, y, AllIcons.I_SCHEMATIC);
        Component title = SchematicPorterScreen.text("title");
        porter.setToolTip(title);
        porter.setMessage(title);
        porter.getToolTip().add(SchematicPorterScreen.text("button_help"));
        porter.withCallback(() -> {
            List<Component> available = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
            String filename = null;
            if (schematicsArea != null && !available.isEmpty()) {
                int selected = Math.clamp(schematicsArea.getState(), 0, available.size() - 1);
                filename = available.get(selected).getString();
            }
            minecraft.setScreen(new SchematicPorterScreen((SchematicTableScreen) (Object) this,
                    filename));
        });
        addRenderableWidget(porter);
        extraAreas = new ArrayList<>(extraAreas);
        if (!hasImportButton) {
            int previousX = refreshButton.getX();
            int previousY = refreshButton.getY();
            extraAreas.removeIf(area -> area.getX() == previousX && area.getY() == previousY
                    && area.getWidth() == refreshButton.getWidth() && area.getHeight() == refreshButton.getHeight());
            // The helper normally moves Refresh below its import slot for us.
            refreshButton.setPosition(porter.getX(), porter.getY() + porter.getHeight() + 3);
            extraAreas.add(new Rect2i(refreshButton.getX(), refreshButton.getY(),
                    refreshButton.getWidth(), refreshButton.getHeight()));
        }
        extraAreas.add(new Rect2i(porter.getX(), porter.getY(), porter.getWidth(), porter.getHeight()));
    }
}
