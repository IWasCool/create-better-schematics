package com.ourcreationstudios.create_better_schematics.cannon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** A display ingredient and its total requirement, independent of Minecraft's stack limit. */
public record CannonMaterial(ItemStack stack, int count) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CannonMaterial> CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CannonMaterial::stack, ByteBufCodecs.VAR_INT, CannonMaterial::count, CannonMaterial::new);

    public CannonMaterial {
        if (stack.isEmpty() || count <= 0)
            throw new IllegalArgumentException("A material must have an item and a positive amount");
        stack = stack.copyWithCount(1);
    }

    public Key key() {
        return new Key(stack);
    }

    /** Matches JEI ingredients without inventing custom item components for the quantity. */
    public static final class Key {
        private final ItemStack stack;

        public Key(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

        @Override public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }

        @Override public boolean equals(Object other) {
            return other instanceof Key key && ItemStack.isSameItemSameComponents(stack, key.stack);
        }
    }
}
