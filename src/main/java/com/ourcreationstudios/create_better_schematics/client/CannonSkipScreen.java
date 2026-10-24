package com.ourcreationstudios.create_better_schematics.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.BlockEntry;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.Result;
import com.ourcreationstudios.create_better_schematics.cannon.CannonSkipPackets.View;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class CannonSkipScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = 214;
    private static final int GRID_LEFT = 15;
    private static final int GRID_TOP = 73;
    private static final int TEXT_COLOR = 0xeeeeee;

    private final SchematicannonScreen parent;
    private final Set<Block> skipped = new HashSet<>();
    private List<BlockEntry> entries = List.of();
    private int fingerprint;
    private boolean loaded;
    private boolean saving;
    private boolean waiting;
    private boolean skipMissing;
    private int waitTicks;
    private Component status = text("loading");
    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int rows;
    private EditBox search;
    private BlockGrid blockGrid;
    private IconButton apply;
    private IconButton selectAll;
    private IconButton clear;
    private IconButton cancel;
    private IconButton missing;

    public CannonSkipScreen(SchematicannonScreen parent) {
        super(text("title"));
        this.parent = parent;
    }

    public static void open(SchematicannonScreen parent) {
        CannonSkipScreen screen = new CannonSkipScreen(parent);
        Minecraft.getInstance().setScreen(screen);
        screen.request();
    }

    public static void receive(View packet) {
        if (Minecraft.getInstance().screen instanceof CannonSkipScreen screen
                && screen.parent.getMenu().containerId == packet.menuId())
            screen.accept(packet);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + ".cannon.skip." + key, args);
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        int previousScroll = blockGrid == null ? 0 : blockGrid.scrollRow;
        rows = Mth.clamp((height - 167) / SLOT_SIZE, 2, 6);
        panelHeight = 159 + rows * SLOT_SIZE;
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - panelHeight) / 2;
        search = addRenderableWidget(new EditBox(font, panelLeft + GRID_LEFT, panelTop + 35,
                COLUMNS * SLOT_SIZE + 20, 16, text("search")));
        search.setHint(text("search").copy().withStyle(ChatFormatting.GRAY));
        search.setMaxLength(128);
        blockGrid = addRenderableWidget(new BlockGrid(panelLeft + GRID_LEFT, panelTop + GRID_TOP));
        search.setValue(query);
        search.setResponder(value -> refreshList());
        missing = iconButton(8, panelHeight - 52, AllIcons.I_SKIP_MISSING, text("skip_missing"), () -> {
            skipMissing = !skipMissing;
            updateControls();
        });
        selectAll = iconButton(8, panelHeight - 24, AllIcons.I_BLACKLIST, text("all"), () -> {
            entries.forEach(entry -> skipped.add(entry.block()));
        });
        clear = iconButton(30, panelHeight - 24, AllIcons.I_WHITELIST, text("none"), skipped::clear);
        cancel = iconButton(158, panelHeight - 24, AllIcons.I_MTD_CLOSE, CommonComponents.GUI_CANCEL, this::onClose);
        apply = iconButton(180, panelHeight - 24, AllIcons.I_CONFIRM, text("apply"), () -> {
            if (!loaded) {
                request();
                return;
            }
            saving = true;
            waiting = true;
            waitTicks = 0;
            PacketDistributor.sendToServer(new CannonSkipPackets.Save(parent.getMenu().containerId,
                    fingerprint, List.copyOf(skipped), skipMissing));
            updateControls();
        });
        apply.green = true;
        refreshList();
        blockGrid.setScrollRow(previousScroll);
        updateControls();
        setInitialFocus(search);
    }

    private IconButton iconButton(int x, int y, AllIcons icon, Component tooltip, Runnable callback) {
        IconButton button = new IconButton(panelLeft + x, panelTop + y, icon);
        button.setToolTip(tooltip);
        button.setMessage(tooltip);
        button.withCallback(callback);
        return addRenderableWidget(button);
    }

    private void request() {
        loaded = false;
        waiting = true;
        saving = false;
        waitTicks = 0;
        status = text("loading");
        if (apply != null)
            updateControls();
        PacketDistributor.sendToServer(new CannonSkipPackets.Request(parent.getMenu().containerId));
    }

    private void accept(View view) {
        waiting = false;
        saving = false;
        if (view.result() == Result.CLOSED) {
            minecraft.player.closeContainer();
            minecraft.setScreen(null);
            return;
        }
        if (view.result() == Result.APPLIED) {
            minecraft.setScreen(parent);
            return;
        }
        loaded = view.result() == Result.READY;
        if (loaded) {
            fingerprint = view.fingerprint();
            entries = new ArrayList<>(view.entries());
            entries.sort(Comparator.comparing((BlockEntry entry) -> entry.block().getName().getString(),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(entry -> BuiltInRegistries.BLOCK.getKey(entry.block())));
            skipped.clear();
            entries.stream().filter(BlockEntry::skipped).forEach(entry -> skipped.add(entry.block()));
            skipMissing = view.skipMissing();
            rebuildWidgets();
        } else {
            status = text("error." + view.result().name().toLowerCase(Locale.ROOT));
            updateControls();
        }
        refreshList();
    }

    private void updateControls() {
        Component action = text(loaded ? "apply" : "reload");
        apply.setIcon(loaded ? AllIcons.I_CONFIRM : AllIcons.I_REFRESH);
        apply.setToolTip(action);
        apply.setMessage(action);
        apply.active = !waiting;
        selectAll.active = clear.active = missing.active = loaded && !saving;
        missing.green = skipMissing;
        missing.setMessage(text("missing_narration", text(skipMissing ? "enabled" : "disabled")));
        missing.getToolTip().clear();
        missing.getToolTip().add(text("skip_missing"));
        missing.getToolTip().add(text(skipMissing ? "enabled" : "disabled").copy()
                .withStyle(skipMissing ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        search.active = loaded && !saving;
        blockGrid.active = loaded && !saving;
        cancel.active = !saving;
    }

    private void refreshList() {
        if (blockGrid == null)
            return;
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        blockGrid.populate(entries.stream().filter(entry -> query.isEmpty()
                || entry.block().getName().getString().toLowerCase(Locale.ROOT).contains(query)
                || BuiltInRegistries.BLOCK.getKey(entry.block()).toString().contains(query)).toList());
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.player.containerMenu != parent.getMenu()) {
            minecraft.setScreen(null);
            return;
        }
        if (waiting && ++waitTicks > 200) {
            waiting = saving = loaded = false;
            status = text("error.failed");
            updateControls();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!saving)
            minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics);
        graphics.drawString(font, title, panelLeft + (PANEL_WIDTH - font.width(title)) / 2,
                panelTop + 4, AllGuiTextures.FONT_COLOR, false);
        graphics.drawString(font, text("description"), panelLeft + 8, panelTop + 20, TEXT_COLOR);
        graphics.drawString(font, text("inventory"), panelLeft + GRID_LEFT + 1, panelTop + 62, 0x404040, false);
        graphics.drawString(font, text("summary", skipped.size(), entries.size()),
                panelLeft + GRID_LEFT, panelTop + panelHeight - 67, TEXT_COLOR);
        graphics.drawString(font, text("skip_missing"), panelLeft + 31, panelTop + panelHeight - 47,
                missing.active ? TEXT_COLOR : 0xa0a0a0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!loaded || saving || blockGrid.shown.isEmpty()) {
            Component message = saving ? text("saving") : !loaded ? status
                    : entries.isEmpty() ? text("empty") : text("no_results");
            graphics.fill(blockGrid.getX(), blockGrid.getY(), blockGrid.getX() + COLUMNS * SLOT_SIZE,
                    blockGrid.getBottom(), 0xe0c6c6c6);
            var lines = font.split(message, COLUMNS * SLOT_SIZE - 12);
            int y = blockGrid.getY() + (blockGrid.getHeight() - lines.size() * font.lineHeight) / 2;
            for (var line : lines) {
                graphics.drawString(font, line, blockGrid.getX() + (COLUMNS * SLOT_SIZE - font.width(line)) / 2,
                        y, 0x404040, false);
                y += font.lineHeight;
            }
        }
        BlockEntry hovered = blockGrid.entryAt(mouseX, mouseY);
        if (hovered != null && loaded && !saving) {
            boolean excluded = skipped.contains(hovered.block());
            graphics.renderComponentTooltip(font, List.of(hovered.block().getName(),
                    Component.literal(BuiltInRegistries.BLOCK.getKey(hovered.block()).toString())
                            .withStyle(ChatFormatting.DARK_GRAY),
                    text("count", hovered.count()).copy().withStyle(ChatFormatting.GRAY),
                    text(excluded ? "state.skipped" : "state.build").copy()
                            .withStyle(excluded ? ChatFormatting.RED : ChatFormatting.GREEN),
                    text("row_help").copy().withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (loaded && mouseX >= panelLeft + GRID_LEFT && mouseX < panelLeft + 198
                && mouseY >= panelTop + panelHeight - 69 && mouseY < panelTop + panelHeight - 55) {
            long count = entries.stream().filter(entry -> skipped.contains(entry.block())).mapToLong(BlockEntry::count).sum();
            graphics.renderComponentTooltip(font, List.of(text("skipped_count", count)), mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphics graphics) {
        // Compose the frame from Create's existing sheets, keeping their pixel borders intact.
        AllGuiTextures table = AllGuiTextures.SCHEMATIC_TABLE;
        graphics.blit(table.location, panelLeft, panelTop, table.getStartX(), table.getStartY(), PANEL_WIDTH, 15);
        for (int y = 15; y < panelHeight - 30; y += 4)
            graphics.blit(table.location, panelLeft, panelTop + y, table.getStartX(), table.getStartY() + 15,
                    PANEL_WIDTH, Math.min(4, panelHeight - 30 - y));
        AllGuiTextures footer = AllGuiTextures.FILTER;
        graphics.blit(footer.location, panelLeft, panelTop + panelHeight - 30, footer.getStartX(),
                footer.getStartY() + footer.getHeight() - 30, PANEL_WIDTH, 30);
        AllGuiTextures inventory = AllGuiTextures.PLAYER_INVENTORY;
        int x = panelLeft + GRID_LEFT - 7;
        int y = panelTop + 56;
        graphics.blit(inventory.location, x, y, 0, 0, inventory.getWidth(), 17);
        for (int row = 0; row < rows; row++)
            graphics.blit(inventory.location, x, y + 17 + row * SLOT_SIZE, 0, 17, inventory.getWidth(), SLOT_SIZE);
        graphics.blit(inventory.location, x, y + 17 + rows * SLOT_SIZE, 0, 94, inventory.getWidth(), 14);
    }

    private final class BlockGrid extends AbstractWidget {
        private static final int SCROLL_X = 170;
        private static final int SCROLLER_HEIGHT = 15;
        private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
        private static final ResourceLocation SCROLL_BACKGROUND = ResourceLocation.withDefaultNamespace("widget/scroller_background");
        private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
        private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");

        private List<BlockEntry> shown = List.of();
        private List<ItemStack> icons = List.of();
        private int scrollRow;
        private int focusedIndex;
        private boolean draggingScroll;

        private BlockGrid(int x, int y) {
            super(x, y, SCROLL_X + 12, rows * SLOT_SIZE, text("inventory"));
        }

        private void populate(List<BlockEntry> shown) {
            this.shown = shown;
            icons = shown.stream().map(entry -> {
                ItemStack icon = new ItemStack(entry.block().asItem());
                return icon.isEmpty() ? new ItemStack(Items.BARRIER) : icon;
            }).toList();
            scrollRow = focusedIndex = 0;
        }

        private int maxScrollRow() {
            return Math.max(0, (shown.size() + COLUMNS - 1) / COLUMNS - rows);
        }

        private void setScrollRow(int row) {
            scrollRow = Mth.clamp(row, 0, maxScrollRow());
            if (!shown.isEmpty())
                focusedIndex = Mth.clamp(focusedIndex, scrollRow * COLUMNS,
                        Math.min(shown.size() - 1, (scrollRow + rows) * COLUMNS - 1));
        }

        private int indexAt(double mouseX, double mouseY) {
            int x = (int) Math.floor(mouseX - getX());
            int y = (int) Math.floor(mouseY - getY());
            if (x < 0 || x >= COLUMNS * SLOT_SIZE || y < 0 || y >= height)
                return -1;
            int index = (scrollRow + y / SLOT_SIZE) * COLUMNS + x / SLOT_SIZE;
            return index < shown.size() ? index : -1;
        }

        private BlockEntry entryAt(double mouseX, double mouseY) {
            int index = indexAt(mouseX, mouseY);
            return index < 0 ? null : shown.get(index);
        }

        private void toggle(int index) {
            if (!active || index < 0 || index >= shown.size())
                return;
            Block block = shown.get(index).block();
            if (!skipped.remove(block))
                skipped.add(block);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int hovered = indexAt(mouseX, mouseY);
            for (int slot = 0; slot < rows * COLUMNS; slot++) {
                int x = getX() + slot % COLUMNS * SLOT_SIZE;
                int y = getY() + slot / COLUMNS * SLOT_SIZE;
                graphics.blitSprite(SLOT, x, y, SLOT_SIZE, SLOT_SIZE);
                int index = scrollRow * COLUMNS + slot;
                if (index >= shown.size() || !loaded || saving)
                    continue;
                BlockEntry entry = shown.get(index);
                boolean excluded = skipped.contains(entry.block());
                if (excluded)
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x66c45555);
                graphics.renderItem(icons.get(index), x + 1, y + 1);
                String count = compactCount(entry.count());
                float countScale = Math.min(0.75f, 16.0f / font.width(count));
                graphics.pose().pushPose();
                graphics.pose().translate(x + 17, y + 17, 200);
                graphics.pose().scale(countScale, countScale, 1);
                graphics.drawString(font, count, -font.width(count), -8, 0xffffff);
                graphics.pose().popPose();
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 200);
                graphics.pose().scale(0.5f, 0.5f, 1);
                if (excluded)
                    AllIcons.I_MTD_CLOSE.render(graphics, 0, 0);
                graphics.pose().popPose();
                if (active && (index == hovered || isFocused() && index == focusedIndex)) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 300);
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80ffffff);
                    graphics.pose().popPose();
                }
            }
            graphics.blitSprite(SCROLL_BACKGROUND, getX() + SCROLL_X, getY(), 12, height);
            int thumbY = maxScrollRow() == 0 ? 0 : (height - SCROLLER_HEIGHT) * scrollRow / maxScrollRow();
            graphics.blitSprite(active && maxScrollRow() > 0 ? SCROLLER : SCROLLER_DISABLED,
                    getX() + SCROLL_X, getY() + thumbY, 12, SCROLLER_HEIGHT);
        }

        private String compactCount(int count) {
            if (count >= 1_000_000)
                return count / 1_000_000 + "m";
            if (count >= 1_000)
                return count / 1_000 + "k";
            return Integer.toString(count);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            draggingScroll = mouseX >= getX() + SCROLL_X && maxScrollRow() > 0;
            if (draggingScroll) {
                scrollTo(mouseY);
            } else {
                int index = indexAt(mouseX, mouseY);
                if (index >= 0) {
                    focusedIndex = index;
                    toggle(index);
                }
            }
        }

        private void scrollTo(double mouseY) {
            setScrollRow((int) Math.round((mouseY - getY() - SCROLLER_HEIGHT / 2.0)
                    / (height - SCROLLER_HEIGHT) * maxScrollRow()));
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            if (active && draggingScroll)
                scrollTo(mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            draggingScroll = false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!active || !isMouseOver(mouseX, mouseY) || maxScrollRow() == 0 || scrollY == 0)
                return false;
            setScrollRow(scrollRow - (int) Math.signum(scrollY));
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!active || shown.isEmpty())
                return false;
            if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                toggle(focusedIndex);
                playDownSound(minecraft.getSoundManager());
                return true;
            }
            Integer next = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> focusedIndex - 1;
                case GLFW.GLFW_KEY_RIGHT -> focusedIndex + 1;
                case GLFW.GLFW_KEY_UP -> focusedIndex - COLUMNS;
                case GLFW.GLFW_KEY_DOWN -> focusedIndex + COLUMNS;
                case GLFW.GLFW_KEY_HOME -> 0;
                case GLFW.GLFW_KEY_END -> shown.size() - 1;
                case GLFW.GLFW_KEY_PAGE_UP -> focusedIndex - rows * COLUMNS;
                case GLFW.GLFW_KEY_PAGE_DOWN -> focusedIndex + rows * COLUMNS;
                default -> null;
            };
            if (next == null)
                return super.keyPressed(keyCode, scanCode, modifiers);
            focusedIndex = Mth.clamp(next, 0, shown.size() - 1);
            int focusedRow = focusedIndex / COLUMNS;
            scrollRow = Mth.clamp(scrollRow, focusedRow - rows + 1, focusedRow);
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            if (!shown.isEmpty()) {
                BlockEntry entry = shown.get(focusedIndex);
                output.add(NarratedElementType.TITLE, text("row_narration", entry.block().getName(), entry.count(),
                        text(skipped.contains(entry.block()) ? "skip" : "build")));
                output.add(NarratedElementType.USAGE, text("grid_narration"));
            } else {
                output.add(NarratedElementType.TITLE, text(loaded ? "no_results" : "loading"));
            }
        }
    }
}
