package com.ourcreationstudios.create_better_schematics.client;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.schematic.SchematicMods;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Client-only inspection; it also works when the server does not have Better Schematics installed. */
public final class SchematicModsScreen extends Screen {
    private static final int GREEN = 0xff55cc66;
    private static final int RED = 0xffff6666;
    private final SchematicTableScreen parent;
    private final String filename;
    private List<ModEntry> mods = List.of();
    private boolean loading = true;
    private boolean failed;
    private boolean requested;
    private int loadId;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private EditBox search;
    private ModsList list;

    public SchematicModsScreen(SchematicTableScreen parent, String filename) {
        super(text("title"));
        this.parent = parent;
        this.filename = filename;
    }

    public static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + ".schematic.mods." + key, args);
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        double scroll = list == null ? 0 : list.getScrollAmount();
        panelWidth = Math.min(340, width - 20);
        panelHeight = Math.min(330, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        addRenderableWidget(Button.builder(text("schematic_tab"), button -> onClose())
                .bounds(panelLeft + 10, panelTop + 21, 96, 20).build());
        Button tab = addRenderableWidget(Button.builder(text("tab"), button -> {})
                .bounds(panelLeft + 108, panelTop + 21, 72, 20).build());
        tab.active = false;
        search = addRenderableWidget(new EditBox(font, panelLeft + 10, panelTop + 65,
                panelWidth - 20, 18, text("search")));
        search.setHint(text("search").copy().withStyle(ChatFormatting.GRAY));
        search.setMaxLength(128);
        search.setValue(query);
        search.setResponder(value -> refreshList());
        list = addRenderableWidget(new ModsList());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(panelLeft + panelWidth - 90, panelTop + panelHeight - 27, 80, 20).build());
        addRenderableWidget(Button.builder(text("reload"), button -> load())
                .bounds(panelLeft + 10, panelTop + panelHeight - 27, 80, 20).build());
        refreshList();
        list.setScrollAmount(scroll);
        setInitialFocus(search);
        if (!requested)
            load();
    }

    private void load() {
        int requestId = ++loadId;
        requested = true;
        loading = true;
        failed = false;
        mods = List.of();
        refreshList();
        // Reading/decompressing a large schematic must not freeze the render thread.
        CompletableFuture.supplyAsync(() -> {
            try {
                return SchematicMods.read(CreatePaths.SCHEMATICS_DIR, filename);
            } catch (IOException | RuntimeException e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()).whenComplete((ids, failure) -> minecraft.execute(() -> {
            if (minecraft.screen != this || requestId != loadId)
                return;
            loading = false;
            failed = failure != null;
            if (failed) {
                CreateBetterSchematics.LOGGER.warn("Could not read mods used by schematic {}", filename, failure);
            } else {
                mods = ids.stream().map(ModEntry::resolve)
                        .sorted(Comparator.comparing(ModEntry::installed)
                                .thenComparing(ModEntry::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ModEntry::id)).toList();
            }
            refreshList();
        }));
    }

    private void refreshList() {
        if (list == null)
            return;
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        list.populate(mods.stream().filter(mod -> mod.id().contains(query)
                || mod.name().toLowerCase(Locale.ROOT).contains(query)).toList());
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.player.containerMenu != parent.getMenu())
            minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        if (minecraft.player != null && minecraft.player.containerMenu == parent.getMenu())
            minecraft.setScreen(parent);
        else
            minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 5, 0xeeeeee);
        graphics.drawString(font, font.plainSubstrByWidth(filename, panelWidth - 20),
                panelLeft + 10, panelTop + 49, 0xeeeeee);
        if (!loading && !failed) {
            long installed = mods.stream().filter(ModEntry::installed).count();
            graphics.drawString(font, text("installed_count", installed), panelLeft + 10,
                    panelTop + panelHeight - 43, GREEN);
            Component missing = text("missing_count", mods.size() - installed);
            graphics.drawString(font, missing, panelLeft + panelWidth - 10 - font.width(missing),
                    panelTop + panelHeight - 43, mods.size() == installed ? GREEN : RED);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (loading || failed || list.children().isEmpty()) {
            Component message = text(loading ? "loading" : failed ? "error" : mods.isEmpty() ? "empty" : "no_results");
            var lines = font.split(message, panelWidth - 44);
            int y = list.getY() + (list.getHeight() - lines.size() * font.lineHeight) / 2;
            for (var line : lines) {
                graphics.drawString(font, line, panelLeft + (panelWidth - font.width(line)) / 2, y, 0xeeeeee);
                y += font.lineHeight;
            }
        }
        ModsList.Row hovered = list.getHovered();
        if (hovered != null)
            graphics.renderComponentTooltip(font, List.of(Component.literal(hovered.mod.name()),
                    Component.literal(hovered.mod.id()).withStyle(ChatFormatting.GRAY),
                    text(hovered.mod.installed() ? "installed" : "missing").copy()
                            .withStyle(hovered.mod.installed() ? ChatFormatting.GREEN : ChatFormatting.RED)), mouseX, mouseY);
    }

    private void renderPanel(GuiGraphics graphics) {
        AllGuiTextures texture = AllGuiTextures.SCHEMATIC_TABLE;
        // Tile the plain strips so Create's frame retains its original pixel borders at any size.
        for (int y = 0; y < panelHeight; ) {
            int sourceY = y < 15 ? y : y >= panelHeight - 7 ? texture.getHeight() - (panelHeight - y) : 15;
            int stripHeight = y < 15 ? 15 : y >= panelHeight - 7 ? 7 : Math.min(4, panelHeight - 7 - y);
            for (int x = 0; x < panelWidth; ) {
                int sourceX = x < 7 ? x : x >= panelWidth - 7 ? texture.getWidth() - (panelWidth - x) : 7;
                int length = x < 7 || x >= panelWidth - 7 ? 7
                        : Math.min(texture.getWidth() - 14, panelWidth - 7 - x);
                graphics.blit(texture.location, panelLeft + x, panelTop + y,
                        texture.getStartX() + sourceX, texture.getStartY() + sourceY, length, stripHeight);
                x += length;
            }
            y += stripHeight;
        }
    }

    private record ModEntry(String id, String name, boolean installed) {
        static ModEntry resolve(String namespace) {
            if (namespace.equals("minecraft"))
                return new ModEntry(namespace, "Minecraft", true);
            return ModList.get().getMods().stream()
                    .filter(mod -> mod.getModId().equals(namespace) || mod.getNamespace().equals(namespace))
                    .findFirst().map(mod -> new ModEntry(namespace, mod.getDisplayName(), true))
                    .orElseGet(() -> new ModEntry(namespace, namespace, false));
        }
    }

    private final class ModsList extends ObjectSelectionList<ModsList.Row> {
        private ModsList() {
            super(SchematicModsScreen.this.minecraft, panelWidth - 20, panelHeight - 144, panelTop + 90, 30);
            setX(panelLeft + 10);
        }

        private void populate(List<ModEntry> entries) {
            clearEntries();
            entries.forEach(mod -> addEntry(new Row(mod)));
            setScrollAmount(0);
        }

        @Override public int getRowWidth() { return getWidth() - 18; }

        @Override protected int getScrollbarPosition() { return getRight() - 6; }

        @Override public Row getHovered() { return super.getHovered(); }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0xff242424);
        }

        @Override protected void renderListSeparators(GuiGraphics graphics) {}

        private final class Row extends ObjectSelectionList.Entry<Row> {
            private final ModEntry mod;

            private Row(ModEntry mod) { this.mod = mod; }

            @Override
            public Component getNarration() {
                return text("row_narration", mod.name(), mod.id(), text(mod.installed() ? "installed" : "missing"));
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered)
                    graphics.fill(left - 2, top - 1, left + width, top + height, 0xff383838);
                int color = mod.installed() ? GREEN : RED;
                // Pixel marks remain visible with every font/resource pack.
                int markX = left + 2;
                int markY = top + 7;
                if (mod.installed()) {
                    for (int step = 0; step < 4; step++)
                        graphics.fill(markX + step, markY + step, markX + step + 2, markY + step + 2, color);
                    for (int step = 0; step < 8; step++)
                        graphics.fill(markX + 3 + step, markY + 3 - step, markX + 5 + step, markY + 5 - step, color);
                } else {
                    for (int step = 0; step < 9; step++) {
                        graphics.fill(markX + step, markY - 3 + step, markX + step + 2, markY - 1 + step, color);
                        graphics.fill(markX + step, markY + 5 - step, markX + step + 2, markY + 7 - step, color);
                    }
                }
                Component status = text(mod.installed() ? "installed" : "missing");
                int nameWidth = width - 30 - font.width(status);
                graphics.drawString(font, font.plainSubstrByWidth(mod.name(), nameWidth), left + 20, top + 1, 0xffffff);
                graphics.drawString(font, status, left + width - font.width(status) - 2, top + 1, color);
                graphics.drawString(font, font.plainSubstrByWidth(mod.id(), width - 24), left + 20, top + 13, 0xaaaaaa);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0)
                    return false;
                setSelected(this);
                return true;
            }
        }
    }
}
