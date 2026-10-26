package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.util.List;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.client.CannonSkipScreen;
import com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicannonScreen.class, remap = false)
abstract class SchematicannonScreenMixin extends AbstractSimiContainerScreen<SchematicannonMenu> {
    protected SchematicannonScreenMixin(SchematicannonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique private IconButton betterSchematics$materialsButton;
    @Shadow protected IconButton skipMissingButton;
    @Shadow protected IconButton skipBlockEntitiesButton;
    @Shadow protected List<IconButton> replaceLevelButtons;
    @Shadow protected abstract boolean placementSettingsHidden();

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void betterSchematics$addMaterialsButton(CallbackInfo ci) {
        betterSchematics$materialsButton = null;
        if (!CannonMaterialsClient.supportedViewer())
            return;
        betterSchematics$materialsButton = new IconButton(leftPos + 152, topPos + 111, AllIcons.I_ADD);
        betterSchematics$materialsButton.withCallback(() -> CannonMaterialsClient.request((SchematicannonScreen) (Object) this));
        betterSchematics$materialsButton.setMessage(CannonMaterialsClient.text("button"));
        addRenderableWidget(betterSchematics$materialsButton);
        betterSchematics$updateMaterialsButton();
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$tickMaterialsButton(CallbackInfo ci) {
        CannonMaterialsClient.tick((SchematicannonScreen) (Object) this);
        betterSchematics$updateMaterialsButton();
    }

    @Unique
    private void betterSchematics$updateMaterialsButton() {
        if (betterSchematics$materialsButton == null)
            return;
        var blueprint = getMenu().contentHolder.inventory.getStackInSlot(0);
        boolean schematic = blueprint.getItem() instanceof SchematicItem
                && blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        boolean waiting = CannonMaterialsClient.waiting((SchematicannonScreen) (Object) this);
        betterSchematics$materialsButton.active = CannonMaterialsClient.exportAvailable() && CannonMaterialsClient.serverAvailable()
                && schematic && !waiting;
        String hint = CannonMaterialsClient.needsEmiAddon() ? "needs_addon"
                : !CannonMaterialsClient.exportAvailable() ? "unavailable" : !CannonMaterialsClient.serverAvailable()
                ? "needs_server" : !schematic ? "needs_schematic" : waiting ? "loading" : "help";
        betterSchematics$materialsButton.setToolTip(CannonMaterialsClient.text("button"));
        betterSchematics$materialsButton.getToolTip().add(CannonMaterialsClient.text(hint));
        if (CannonMaterialsClient.usesEmi())
            betterSchematics$materialsButton.getToolTip().add(CannonMaterialsClient.text("required"));
    }

    @Inject(method = "initPlacementSettings", at = @At("RETURN"), remap = false)
    private void betterSchematics$openSkipPicker(CallbackInfo ci) {
        if (!placementSettingsHidden() && betterSchematics$materialsButton != null) {
            for (int index = 0; index < replaceLevelButtons.size(); index++)
                replaceLevelButtons.get(index).setX(leftPos + 30 + index * 18);
            skipMissingButton.setX(leftPos + 106);
            skipBlockEntitiesButton.setX(leftPos + 128);
        }
        if (CannonSkipScreen.supportsServer() && !placementSettingsHidden() && skipMissingButton != null)
            skipMissingButton.withCallback(() -> CannonSkipScreen.open((SchematicannonScreen) (Object) this));
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$describeSkipPicker(CallbackInfo ci) {
        if (!CannonSkipScreen.supportsServer() || placementSettingsHidden() || skipMissingButton == null)
            return;
        var cannon = ((SchematicannonScreen) (Object) this).getMenu().contentHolder;
        int selected = ((CannonSkipAccess) cannon).betterSchematics$getSkipSettings().skipped().size();
        skipMissingButton.green = selected > 0 || cannon.skipMissing;
        skipMissingButton.setToolTip(Component.translatable(CreateBetterSchematics.MODID + ".cannon.skip.title"));
        skipMissingButton.getToolTip().add(Component.translatable(CreateBetterSchematics.MODID + ".cannon.skip.button_help", selected));
    }
}
