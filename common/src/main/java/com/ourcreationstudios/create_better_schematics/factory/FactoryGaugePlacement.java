package com.ourcreationstudios.create_better_schematics.factory;

import java.util.HashMap;
import java.util.Map;

import com.ourcreationstudios.create_better_schematics.mixin.StructureTemplateAccessor;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

/** Relocates connections before Create loads block entities in its schematic level. */
public final class FactoryGaugePlacement {
    private StructureTemplate cachedTemplate;
    private final Map<BlockPos, StructureBlockInfo> layout = new HashMap<>();

    public StructureBlockInfo process(StructureBlockInfo raw, StructureBlockInfo placed,
                                      StructurePlaceSettings settings, StructureTemplate template) {
        if (raw.nbt() == null || placed.nbt() == null || !hasConnections(raw.nbt()))
            return placed;
        CompoundTag original = raw.nbt();
        int[] origin = original.getIntArray(FactoryGaugeConnections.ORIGIN);
        if (origin.length != 3 && (!original.contains("x", Tag.TAG_ANY_NUMERIC)
                || !original.contains("y", Tag.TAG_ANY_NUMERIC) || !original.contains("z", Tag.TAG_ANY_NUMERIC)))
            return placed;
        cacheLayout(template);
        BlockPos source = origin.length == 3 ? new BlockPos(origin[0], origin[1], origin[2])
                : new BlockPos(original.getInt("x"), original.getInt("y"), original.getInt("z"));
        CompoundTag data = placed.nbt().copy();
        data.remove(FactoryGaugeConnections.ORIGIN);
        for (String slot : FactoryGaugeConnections.SLOTS) {
            if (!data.contains(slot, Tag.TAG_COMPOUND))
                continue;
            CompoundTag panel = data.getCompound(slot);
            FactoryGaugeConnections.convert(panel, false);
            relocate(panel, "Targeting", false, false, source, raw.pos(), placed.pos(), settings);
            relocate(panel, "TargetedBy", true, false, source, raw.pos(), placed.pos(), settings);
            relocate(panel, "TargetedByLinks", true, true, source, raw.pos(), placed.pos(), settings);
            FactoryGaugeConnections.convert(panel, true);
        }
        FactoryGaugeConnections.convert(data, false);
        relocate(data, "LinkedGauges", false, false, source, raw.pos(), placed.pos(), settings);
        FactoryGaugeConnections.convert(data, true);
        if (raw.state().getBlock() instanceof FactoryPanelBlock) {
            CompoundTag panels = new CompoundTag();
            for (String slot : FactoryGaugeConnections.SLOTS) {
                if (data.contains(slot, Tag.TAG_COMPOUND))
                    panels.put(transformSlot(raw.state(), slot, settings), data.getCompound(slot).copy());
                data.remove(slot);
            }
            data.merge(panels);
        }
        return new StructureBlockInfo(placed.pos(), placed.state(), data);
    }

    private void relocate(CompoundTag owner, String key, boolean connection, boolean support,
                          BlockPos source, BlockPos local, BlockPos destination, StructurePlaceSettings settings) {
        if (!owner.contains(key, Tag.TAG_LIST))
            return;
        ListTag relocated = new ListTag();
        for (Tag value : owner.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag entry = ((CompoundTag) value).copy();
            CompoundTag position = connection ? entry.getCompound("position") : entry;
            if (!FactoryGaugeConnections.validPosition(position))
                continue;
            BlockPos relative = FactoryGaugeConnections.position(position).subtract(source);
            StructureBlockInfo referenced = layout.get(local.offset(relative));
            // A copied setup only owns connections whose other endpoint is included.
            if (referenced == null || referenced.nbt() == null)
                continue;
            String slot = position.getString("slot");
            if (support ? !referenced.nbt().contains("LinkedGauges")
                    : !(referenced.state().getBlock() instanceof FactoryPanelBlock)
                            || !referenced.nbt().contains(slot, Tag.TAG_COMPOUND))
                continue;
            BlockPos transformed = StructureTemplate.calculateRelativePosition(settings, local.offset(relative))
                    .subtract(StructureTemplate.calculateRelativePosition(settings, local)).offset(destination);
            position.putIntArray("pos", new int[] { transformed.getX(), transformed.getY(), transformed.getZ() });
            if (!support)
                position.putString("slot", transformSlot(referenced.state(), slot, settings));
            relocated.add(entry);
        }
        owner.put(key, relocated);
    }

    private void cacheLayout(StructureTemplate template) {
        if (cachedTemplate == template)
            return;
        cachedTemplate = template;
        layout.clear();
        var palettes = ((StructureTemplateAccessor) template).betterSchematics$getPalettes();
        if (!palettes.isEmpty())
            for (StructureBlockInfo block : palettes.get(0).blocks())
                layout.put(block.pos(), block);
    }

    private static boolean hasConnections(CompoundTag data) {
        return data.contains("LinkedGauges") || FactoryGaugeConnections.SLOTS.stream().anyMatch(data::contains);
    }

    private static String transformSlot(BlockState state, String slotName, StructurePlaceSettings settings) {
        PanelSlot slot = PanelSlot.valueOf(slotName.toUpperCase(java.util.Locale.ROOT));
        Vec3 point = center(state, slot);
        // Vanilla applies the mirror before rotating around Y.
        point = VecHelper.mirrorCentered(point, settings.getMirror());
        point = VecHelper.rotateCentered(point, -90 * settings.getRotation().ordinal(), Axis.Y);
        BlockState transformed = state.mirror(settings.getMirror()).rotate(settings.getRotation());
        PanelSlot closest = slot;
        double distance = Double.MAX_VALUE;
        for (PanelSlot candidate : PanelSlot.values()) {
            double candidateDistance = center(transformed, candidate).distanceToSqr(point);
            if (candidateDistance < distance) {
                distance = candidateDistance;
                closest = candidate;
            }
        }
        return FactoryGaugeConnections.SLOTS.get(closest.ordinal());
    }

    private static Vec3 center(BlockState state, PanelSlot slot) {
        Vec3 point = new Vec3(.25 + slot.xOffset * .5, 1.5 / 16f, .25 + slot.yOffset * .5);
        point = VecHelper.rotateCentered(point, 180, Axis.Y);
        point = VecHelper.rotateCentered(point, Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state) + 90, Axis.X);
        return VecHelper.rotateCentered(point, Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state), Axis.Y);
    }
}
