package com.ourcreationstudios.create_better_schematics.compat;

import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import com.ourcreationstudios.create_better_schematics.mixin.StructureTemplateAccessor;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Relocates reciprocal universal-joint rods without copying ship identities or live kinetic data. */
public final class TransmissionLinks {
    public static final String PENDING = "create_better_schematics:transmission_link";
    public static final String ORIGIN = "create_better_schematics:transmission_origin";
    private static final String MOD = "aeronautics_utility_objects";
    private StructureTemplate cachedTemplate;
    private final Map<BlockPos, StructureBlockInfo> layout = new HashMap<>();
    private final Map<BlockPos, UUID> pairs = new HashMap<>();

    public StructureBlockInfo process(StructureBlockInfo raw, StructureBlockInfo placed,
                                     StructurePlaceSettings settings, StructureTemplate template) {
        if (placed == null || placed.nbt() == null || raw.nbt() == null || !isJoint(raw.state()))
            return placed;
        CompoundTag data = placed.nbt().copy();
        data.remove(PENDING);
        // All physical references come from the selected source layout, never from a saved ship UUID.
        BlockPos source = origin(raw.nbt());
        if (!raw.nbt().contains("LinkedSubLevel") && source != null) {
            cache(template);
            BlockPos linked = NbtUtils.readBlockPos(raw.nbt(), "LinkedPos").orElse(null);
            BlockPos otherLocal = linked == null ? null : raw.pos().offset(linked.subtract(source));
            StructureBlockInfo other = layout.get(otherLocal);
            String variant = variant(raw);
            if (other != null && !other.pos().equals(raw.pos()) && isJoint(other.state()) && other.nbt() != null
                    && !other.nbt().contains("LinkedSubLevel")
                    && linked.equals(origin(other.nbt()))
                    && source.equals(NbtUtils.readBlockPos(other.nbt(), "LinkedPos").orElse(null))
                    && !variant.isEmpty() && variant.equals(variant(other))) {
                BlockPos target = StructureTemplate.calculateRelativePosition(settings, otherLocal)
                        .subtract(StructureTemplate.calculateRelativePosition(settings, raw.pos())).offset(placed.pos());
                CompoundTag pending = new CompoundTag();
                BlockPos owner = raw.pos().compareTo(otherLocal) < 0 ? raw.pos() : otherLocal;
                pending.putUUID("Pair", pairs.computeIfAbsent(owner, ignored -> UUID.randomUUID()));
                pending.put("Source", NbtUtils.writeBlockPos(placed.pos()));
                pending.put("Target", NbtUtils.writeBlockPos(target));
                pending.putBoolean("Owner", raw.pos().equals(owner));
                pending.putString("Variant", variant);
                double rest = raw.nbt().getDouble("LinkedRestLength");
                if (Double.isFinite(rest) && rest > 1.0e-4)
                    pending.putDouble("RestLength", rest);
                data.put(PENDING, pending);
            }
        }
        for (String key : java.util.List.of(ORIGIN, "LinkedPos", "LinkedSubLevel", "LinkedVariant", "LinkedRestLength",
                "TransmissionAxisAligned", "LinkStrainEffect", "LinkStrained"))
            data.remove(key);
        return new StructureBlockInfo(placed.pos(), placed.state(), data);
    }

    private static BlockPos origin(CompoundTag data) {
        BlockPos origin = NbtUtils.readBlockPos(data, ORIGIN).orElse(null);
        if (origin != null)
            return origin;
        return data.contains("x", Tag.TAG_ANY_NUMERIC) && data.contains("y", Tag.TAG_ANY_NUMERIC)
                && data.contains("z", Tag.TAG_ANY_NUMERIC)
                ? new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z")) : null;
    }

    private void cache(StructureTemplate template) {
        if (cachedTemplate == template)
            return;
        cachedTemplate = template;
        layout.clear();
        pairs.clear();
        var palettes = ((StructureTemplateAccessor) template).betterSchematics$getPalettes();
        if (!palettes.isEmpty())
            for (StructureBlockInfo block : palettes.getFirst().blocks())
                layout.put(block.pos(), block);
    }

    private static boolean isJoint(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(MOD)
                && (id.getPath().equals("universal_joint") || id.getPath().equals("brass_universal_joint"));
    }

    private static String variant(StructureBlockInfo info) {
        String variant = info.nbt().getString("LinkedVariant");
        if (variant.isEmpty())
            return BuiltInRegistries.BLOCK.getKey(info.state().getBlock()).getPath().equals("brass_universal_joint")
                    ? "brass" : "andesite";
        return variant.equals("brass") || variant.equals("andesite") ? variant : "";
    }

    public static CompoundTag prepare(BlockEntity entity, CompoundTag safe) {
        if (!(entity instanceof TransmissionLinkAccess access))
            return safe;
        CompoundTag pending = access.betterSchematics$getPendingLink();
        if (pending.isEmpty())
            return safe;
        CompoundTag result = safe == null ? new CompoundTag() : safe.copy();
        result.put(PENDING, pending.copy());
        return result;
    }

    public static ItemRequirement requirements(ItemRequirement base, BlockEntity entity) {
        if (!(entity instanceof TransmissionLinkAccess access) || base.isInvalid())
            return base;
        CompoundTag link = access.betterSchematics$getPendingLink();
        if (link.isEmpty() || !link.getBoolean("Owner"))
            return base;
        String variant = link.getString("Variant");
        if (!variant.equals("andesite") && !variant.equals("brass"))
            return ItemRequirement.INVALID;
        var rod = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD,
                variant.equals("brass") ? "universal_joint_rod" : "universal_joint_rod2"));
        return rod == Items.AIR ? ItemRequirement.INVALID
                : base.union(new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, rod));
    }

    public static void tick(BlockEntity entity, TransmissionLinkAccess access) {
        CompoundTag link = access.betterSchematics$getPendingLink();
        if (link.isEmpty() || !(entity.getLevel() instanceof ServerLevel level))
            return;
        BlockPos target = NbtUtils.readBlockPos(link, "Target").orElse(null);
        if (!link.hasUUID("Pair") || !entity.getBlockPos().equals(NbtUtils.readBlockPos(link, "Source").orElse(null))
                || target == null || target.equals(entity.getBlockPos())) {
            access.betterSchematics$setPendingLink(new CompoundTag());
            return;
        }
        if (!level.hasChunkAt(target))
            return;
        BlockEntity other = level.getBlockEntity(target);
        if (!(other instanceof TransmissionLinkAccess otherAccess))
            return;
        CompoundTag counterpart = otherAccess.betterSchematics$getPendingLink();
        if (!counterpart.hasUUID("Pair") || !link.getUUID("Pair").equals(counterpart.getUUID("Pair"))
                || !entity.getBlockPos().equals(NbtUtils.readBlockPos(counterpart, "Target").orElse(null))
                || !target.equals(NbtUtils.readBlockPos(counterpart, "Source").orElse(null))
                || !link.getString("Variant").equals(counterpart.getString("Variant"))
                || link.getBoolean("Owner") == counterpart.getBoolean("Owner"))
            return;
        // A manually changed endpoint must never be disconnected by a delayed schematic operation.
        if (entity.saveWithoutMetadata(level.registryAccess()).contains("LinkedPos")
                || other.saveWithoutMetadata(level.registryAccess()).contains("LinkedPos")) {
            clear(entity, access, other, otherAccess);
            return;
        }
        try {
            Class<?> type = entity.getClass();
            while (!type.getName().equals("com.enxv.aerouniversaljoint.content.UniversalJointBlockEntity"))
                type = type.getSuperclass();
            Object result = type.getMethod("createMutualLink", type, boolean.class)
                    .invoke(entity, other, link.getString("Variant").equals("brass"));
            clear(entity, access, other, otherAccess);
            if (result.toString().equals("SUCCESS")) {
                restoreRestLength(entity, link);
                restoreRestLength(other, counterpart);
            }
        } catch (ReflectiveOperationException e) {
            clear(entity, access, other, otherAccess);
            CreateBetterSchematics.LOGGER.warn("Could not restore Transmission universal-joint rod at {}", entity.getBlockPos(), e);
        }
    }

    private static void restoreRestLength(BlockEntity entity, CompoundTag link) {
        double rest = link.getDouble("RestLength");
        if (!Double.isFinite(rest) || rest <= 1.0e-4)
            return;
        var registries = entity.getLevel().registryAccess();
        CompoundTag data = entity.saveWithoutMetadata(registries);
        data.putDouble("LinkedRestLength", rest);
        entity.loadWithComponents(data, registries);
        entity.setChanged();
    }

    private static void clear(BlockEntity entity, TransmissionLinkAccess access,
                              BlockEntity other, TransmissionLinkAccess otherAccess) {
        access.betterSchematics$setPendingLink(new CompoundTag());
        otherAccess.betterSchematics$setPendingLink(new CompoundTag());
        entity.setChanged();
        other.setChanged();
    }
}
