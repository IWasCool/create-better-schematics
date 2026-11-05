package com.ourcreationstudios.create_better_schematics.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.schematic.SchematicMods;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Inspects the selected file locally and keeps the results in the table's info tooltip. */
public final class SchematicModsInfo extends IconButton {
    private String filename;
    private List<ModEntry> mods = List.of();
    private boolean loading;
    private boolean failed;
    private int requestId;
    private int firstRow;

    public SchematicModsInfo(int x, int y) {
        super(x, y, (graphics, iconX, iconY) -> {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, "i", iconX + (16 - font.width("i")) / 2, iconY + 4, 0xffffff, false);
        });
        setMessage(text("title"));
        withCallback(() -> load());
    }

    public static Component text(String key, Object... args) {
        return Component.translatable(CreateBetterSchematics.MODID + ".schematic.mods." + key, args);
    }

    public void select(String selected) {
        if (Objects.equals(filename, selected))
            return;
        filename = selected;
        active = selected != null;
        load();
    }

    private void load() {
        int currentRequest = ++requestId;
        String selected = filename;
        firstRow = 0;
        mods = List.of();
        failed = false;
        loading = selected != null;
        if (selected == null)
            return;
        // Decompressing a large schematic must not block rendering or selection changes.
        CompletableFuture.supplyAsync(() -> {
            try {
                return SchematicMods.read(CreatePaths.SCHEMATICS_DIR, selected);
            } catch (IOException | RuntimeException e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()).whenComplete((ids, failure) -> Minecraft.getInstance().execute(() -> {
            if (currentRequest != requestId)
                return;
            loading = false;
            failed = failure != null;
            if (failed) {
                CreateBetterSchematics.LOGGER.warn("Could not read mods used by schematic {}", selected, failure);
            } else {
                mods = ids.stream().map(ModEntry::resolve)
                        .sorted(Comparator.comparing(ModEntry::installed)
                                .thenComparing(ModEntry::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ModEntry::id)).toList();
            }
        }));
    }

    @Override
    public List<Component> getToolTip() {
        List<Component> lines = new ArrayList<>();
        lines.add(text("title"));
        if (filename == null || loading || failed || mods.isEmpty()) {
            lines.add(text(filename == null ? "select" : loading ? "loading" : failed ? "error" : "empty")
                    .copy().withStyle(failed ? ChatFormatting.RED : ChatFormatting.GRAY));
            return lines;
        }
        long installed = mods.stream().filter(ModEntry::installed).count();
        lines.add(text("installed_count", installed).copy().withStyle(ChatFormatting.GREEN)
                .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(text("missing_count", mods.size() - installed).copy()
                        .withStyle(installed == mods.size() ? ChatFormatting.GREEN : ChatFormatting.RED)));
        int rows = visibleRows();
        firstRow = Math.min(firstRow, Math.max(0, mods.size() - rows));
        for (int index = firstRow; index < Math.min(mods.size(), firstRow + rows); index++) {
            ModEntry mod = mods.get(index);
            lines.add(text("entry", mod.name(), text(mod.installed() ? "installed" : "missing"))
                    .copy().withStyle(mod.installed() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (mods.size() > rows)
            lines.add(text("scroll", firstRow + 1, Math.min(mods.size(), firstRow + rows), mods.size())
                    .copy().withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private int visibleRows() {
        var screen = Minecraft.getInstance().screen;
        // Reserve room for the title, totals, scroll hint, and tooltip padding.
        return screen == null ? 10 : Math.max(1, Math.min(16, (screen.height - 70) / 12));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY) || scrollY == 0 || mods.size() <= visibleRows())
            return false;
        firstRow = Math.clamp(firstRow - (int) Math.signum(scrollY), 0, mods.size() - visibleRows());
        return true;
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
}
