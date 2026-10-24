package com.ourcreationstudios.create_better_schematics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.porter.SchematicPorter;
import com.ourcreationstudios.create_better_schematics.porter.SchematicPorter.Target;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class SchematicPorterScreen extends Screen {
    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_HEIGHT = 220;
    private final SchematicTableScreen parent;
    private final String filename;
    private Target target = Target.FORGE_1_20_1;
    private int left;
    private int top;
    private int scroll;
    private boolean working;
    private boolean failed;
    private SchematicPorter.Export exported;
    private Component status = text("scope");
    private SelectionScrollInput targetInput;
    private IconButton exportButton;
    private final List<FormattedCharSequence> reportLines = new ArrayList<>();

    public SchematicPorterScreen(SchematicTableScreen parent, String filename) {
        super(text("title"));
        this.parent = parent;
        this.filename = filename;
        if (filename == null)
            status = text("select_schematic");
    }

    public static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + ".porter." + key, args);
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        Label label = new Label(left + 16, top + 57, CommonComponents.EMPTY).withShadow();
        targetInput = new SelectionScrollInput(left + 12, top + 52, PANEL_WIDTH - 24, 18);
        targetInput.forOptions(List.of(text("target.forge_1_20_1"), text("target.fabric_1_20_1")))
                .titled(text("target").copy()).writingTo(label);
        targetInput.setState(target.ordinal());
        targetInput.onChanged();
        targetInput.calling(index -> {
            target = Target.values()[index];
            exported = null;
            failed = false;
            status = text("scope");
            refreshReport();
        });
        addRenderableWidget(targetInput);
        addRenderableWidget(label);
        IconButton folder = button(12, AllIcons.I_OPEN_FOLDER, text("folder"),
                () -> Util.getPlatform().openFile(CreatePaths.SCHEMATICS_DIR.toFile()));
        folder.getToolTip().add(text("copy_help"));
        button(PANEL_WIDTH - 56, AllIcons.I_MTD_CLOSE, CommonComponents.GUI_BACK, this::onClose);
        exportButton = button(PANEL_WIDTH - 34, AllIcons.I_CONFIG_SAVE, text("export"), this::export);
        exportButton.green = true;
        updateControls();
        refreshReport();
    }

    private IconButton button(int x, AllIcons icon, Component tooltip, Runnable action) {
        IconButton button = new IconButton(left + x, top + PANEL_HEIGHT - 27, icon);
        button.setToolTip(tooltip);
        button.setMessage(tooltip);
        button.withCallback(action);
        return addRenderableWidget(button);
    }

    private void export() {
        if (working || filename == null)
            return;
        working = true;
        failed = false;
        exported = null;
        status = text("working");
        updateControls();
        refreshReport();
        Target requestedTarget = target;
        CompletableFuture.supplyAsync(() -> {
            try {
                return SchematicPorter.export(CreatePaths.SCHEMATICS_DIR, filename, requestedTarget);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.ioPool()).whenComplete((result, error) -> minecraft.execute(() -> {
            working = false;
            failed = error != null;
            exported = result;
            if (error != null) {
                Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                status = text("error", cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
                CreateBetterSchematics.LOGGER.warn("Could not port schematic {}", filename, cause);
            } else {
                status = text("saved", result.file().getFileName().toString());
                CreateClient.SCHEMATIC_SENDER.refresh();
            }
            updateControls();
            refreshReport();
        }));
    }

    private void updateControls() {
        if (exportButton != null)
            exportButton.active = !working && filename != null;
        if (targetInput != null)
            targetInput.active = !working;
    }

    private void refreshReport() {
        reportLines.clear();
        addReport(status);
        if (exported != null) {
            var result = exported.conversion();
            addReport(text("counts", result.arms(), result.switches(), result.filters()));
            addReport(text("copy_help"));
            for (var notice : result.notices())
                addReport(text("notice." + notice.key(), notice.detail(), notice.count()));
        }
        scroll = 0;
    }

    private void addReport(Component component) {
        reportLines.addAll(font.split(component, PANEL_WIDTH - 36));
        reportLines.add(FormattedCharSequence.EMPTY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderFrame(graphics);
        graphics.fill(left + 12, top + 52, left + PANEL_WIDTH - 12, top + 70, 0xff454858);
        graphics.fill(left + 13, top + 53, left + PANEL_WIDTH - 13, top + 69, 0xff747b9e);
        graphics.fill(left + 8, top + 77, left + PANEL_WIDTH - 8, top + PANEL_HEIGHT - 35, 0xcc20242a);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 4, AllGuiTextures.FONT_COLOR);
        Component selected = filename == null ? text("no_schematic")
                : Component.literal(font.plainSubstrByWidth(filename, PANEL_WIDTH - 28));
        graphics.drawString(font, selected, left + 14, top + 29, 0xdddddd);
        graphics.drawString(font, text("target_label"), left + 14, top + 42, 0xdddddd);
        int visibleLines = (PANEL_HEIGHT - 119) / 11;
        int maxScroll = Math.max(0, reportLines.size() - visibleLines);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        for (int i = 0; i < visibleLines && scroll + i < reportLines.size(); i++)
            graphics.drawString(font, reportLines.get(scroll + i), left + 16, top + 84 + i * 11,
                    failed ? 0xff9999 : 0xeeeeee);
        if (maxScroll > 0)
            graphics.drawString(font, "\u2195", left + PANEL_WIDTH - 20, top + PANEL_HEIGHT - 49, 0xbbbbbb);
    }

    private void renderFrame(GuiGraphics graphics) {
        AllGuiTextures table = AllGuiTextures.SCHEMATIC_TABLE;
        // Tile Create's pattern at its original pixel scale instead of stretching it.
        frameBand(graphics, table, 0, table.getStartY(), 15);
        for (int y = 15; y < PANEL_HEIGHT - 6; y += 4)
            frameBand(graphics, table, y, table.getStartY() + 15, Math.min(4, PANEL_HEIGHT - 6 - y));
        AllGuiTextures footer = AllGuiTextures.SCHEMATIC_TABLE;
        frameBand(graphics, footer, PANEL_HEIGHT - 6, footer.getStartY() + footer.getHeight() - 6, 6);
    }

    private void frameBand(GuiGraphics graphics, AllGuiTextures texture, int y, int sourceY, int h) {
        graphics.blit(texture.location, left, top + y, texture.getStartX(), sourceY, 6, h);
        // These sheets include an eight-pixel protruding button beyond the main frame.
        int frameWidth = texture.getWidth() - 8;
        int tileWidth = (frameWidth - 12) / 4 * 4;
        for (int x = 6; x < PANEL_WIDTH - 6; x += tileWidth)
            graphics.blit(texture.location, left + x, top + y, texture.getStartX() + 6, sourceY,
                    Math.min(tileWidth, PANEL_WIDTH - 6 - x), h);
        graphics.blit(texture.location, left + PANEL_WIDTH - 6, top + y,
                texture.getStartX() + frameWidth - 6, sourceY, 6, h);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                && mouseY >= top + 77 && mouseY < top + PANEL_HEIGHT - 35) {
            scroll = Math.max(0, scroll - (int) Math.signum(vertical) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
