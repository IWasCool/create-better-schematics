package com.ourcreationstudios.create_better_schematics.mixin.client;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipAccess;
import com.ourcreationstudios.create_better_schematics.client.CannonSkipScreen;
import com.ourcreationstudios.create_better_schematics.client.CannonMaterialsClient;
import com.ourcreationstudios.create_better_schematics.client.jei.JeiMaterials;
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

    @Unique private IconButton betterSchematics$jeiButton;
    @Shadow protected IconButton skipMissingButton;
    @Shadow protected abstract boolean placementSettingsHidden();

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void betterSchematics$addJeiButton(CallbackInfo ci) {
        betterSchematics$jeiButton = null;
        if (!JeiMaterials.supportedVersion())
            return;
        betterSchematics$jeiButton = new IconButton(leftPos + 156, topPos + 111, AllIcons.I_ADD);
        betterSchematics$jeiButton.withCallback(() -> CannonMaterialsClient.request((SchematicannonScreen) (Object) this));
        betterSchematics$jeiButton.setMessage(CannonMaterialsClient.text("button"));
        addRenderableWidget(betterSchematics$jeiButton);
        betterSchematics$updateJeiButton();
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$tickJeiButton(CallbackInfo ci) {
        CannonMaterialsClient.tick((SchematicannonScreen) (Object) this);
        betterSchematics$updateJeiButton();
    }

    @Unique
    private void betterSchematics$updateJeiButton() {
        if (betterSchematics$jeiButton == null)
            return;
        var blueprint = getMenu().contentHolder.inventory.getStackInSlot(0);
        boolean schematic = blueprint.getItem() instanceof SchematicItem
                && blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        boolean waiting = CannonMaterialsClient.waiting((SchematicannonScreen) (Object) this);
        betterSchematics$jeiButton.active = JeiMaterials.available() && CannonMaterialsClient.serverAvailable()
                && schematic && !waiting;
        String hint = !JeiMaterials.available() ? "unavailable" : !CannonMaterialsClient.serverAvailable()
                ? "needs_server" : !schematic ? "needs_schematic" : waiting ? "loading" : "help";
        betterSchematics$jeiButton.setToolTip(CannonMaterialsClient.text("button"));
        betterSchematics$jeiButton.getToolTip().add(CannonMaterialsClient.text(hint));
    }

    @Inject(method = "initPlacementSettings", at = @At("RETURN"), remap = false)
    private void betterSchematics$openSkipPicker(CallbackInfo ci) {
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
