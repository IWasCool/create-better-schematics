package com.ourcreationstudios.create_better_schematics.mixin.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ourcreationstudios.create_better_schematics.client.SchematicModsInfo;
import com.ourcreationstudios.create_better_schematics.client.SchematicTableTab;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.gui.GuiGraphics;
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
    @Shadow private IconButton refreshButton;
    @Shadow protected AllGuiTextures background;
    @Unique private SchematicModsInfo betterSchematics$modsInfo;
    @Unique private String betterSchematics$selectedFile;
    @Unique private boolean betterSchematics$toolbarLaidOut;
    @Unique private List<IconButton> betterSchematics$tabs = List.of();

    protected SchematicTableScreenMixin(SchematicTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void betterSchematics$addModsInfo(CallbackInfo ci) {
        // Resizing must retain the selected file and update the visible label.
        var files = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
        if (schematicsArea != null && betterSchematics$selectedFile != null)
            for (int index = 0; index < files.size(); index++)
                if (files.get(index).getString().equals(betterSchematics$selectedFile)) {
                    schematicsArea.setState(index).onChanged();
                    break;
                }
        betterSchematics$toolbarLaidOut = false;
        betterSchematics$tabs = List.of();
        betterSchematics$modsInfo = addRenderableWidget(new SchematicModsInfo(leftPos + 206, topPos + 43));
        betterSchematics$updateModsInfo();
    }

    @Unique
    private void betterSchematics$layoutToolbar() {
        if (betterSchematics$toolbarLaidOut || betterSchematics$modsInfo == null)
            return;
        List<IconButton> controls = new ArrayList<>();
        if (ModList.get().isLoaded("create_blueprinted")) {
            // Other mods may add share/download buttons after our init injection.
            // Wait until the first tick/render, when the complete toolbar is present.
            controls.addAll(children().stream().filter(IconButton.class::isInstance).map(IconButton.class::cast)
                    .filter(button -> button == refreshButton
                            || button.getClass().getName().startsWith("net.swzo.create_blueprinted.gui."))
                    .sorted(Comparator.comparingInt(IconButton::getY)).toList());
        } else {
            controls.add(refreshButton);
        }
        controls.add(betterSchematics$modsInfo);
        int rowWidth = controls.size() * 14 - 2;
        // The texture includes an eight-pixel output arrow beyond the title's right edge.
        int headerRight = leftPos + background.getWidth() - 8;
        int x = headerRight - 2 - rowWidth;
        for (IconButton control : controls) {
            control.setX(x);
            control.setWidth(12);
            control.setHeight(14);
            // The bottom two pixels sit behind the table's header frame.
            control.setY(topPos - 12);
            ((SchematicTableTab) control).betterSchematics$attachTab();
            x += 14;
        }
        betterSchematics$tabs = List.copyOf(controls);
        betterSchematics$toolbarLaidOut = true;
    }

    @Inject(method = "renderBg", at = @At("HEAD"), remap = false)
    private void betterSchematics$layoutBeforeRender(GuiGraphics graphics, float partialTicks, int mouseX,
                                                     int mouseY, CallbackInfo ci) {
        betterSchematics$layoutToolbar();
        for (IconButton tab : betterSchematics$tabs)
            ((SchematicTableTab) tab).betterSchematics$renderBehind(graphics, mouseX, mouseY, partialTicks);
    }

    @Inject(method = "containerTick", at = @At("RETURN"), remap = false)
    private void betterSchematics$tickModsInfo(CallbackInfo ci) {
        betterSchematics$layoutToolbar();
        betterSchematics$updateModsInfo();
    }

    @Unique
    private String betterSchematics$selection() {
        var files = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
        int index = schematicsArea == null ? -1 : schematicsArea.getState();
        return schematicsArea != null && schematicsArea.visible && index >= 0 && index < files.size()
                ? files.get(index).getString() : null;
    }

    @Unique
    private void betterSchematics$updateModsInfo() {
        if (betterSchematics$modsInfo == null)
            return;
        String filename = betterSchematics$selection();
        if (filename != null)
            betterSchematics$selectedFile = filename;
        betterSchematics$modsInfo.active = filename != null;
        betterSchematics$modsInfo.select(filename);
    }

    @Inject(method = "getExtraAreas", at = @At("RETURN"), cancellable = true, remap = false)
    private void betterSchematics$includeToolbar(CallbackInfoReturnable<List<Rect2i>> cir) {
        if (betterSchematics$modsInfo == null)
            return;
        List<Rect2i> areas = new ArrayList<>(cir.getReturnValue());
        // The original refresh button no longer occupies the right side of the menu.
        areas.removeIf(area -> area.getX() == leftPos + 206 && area.getY() == topPos + 23
                && area.getWidth() == 18 && area.getHeight() == 18);
        for (IconButton button : betterSchematics$tabs)
            if (button.visible)
                areas.add(new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
        cir.setReturnValue(areas);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        // Tabs are part of this menu even though they extend above its original rectangle.
        for (IconButton tab : betterSchematics$tabs)
            if (tab.visible && mouseX >= tab.getX() && mouseX < tab.getRight()
                    && mouseY >= tab.getY() && mouseY < tab.getBottom())
                return false;
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }
}
