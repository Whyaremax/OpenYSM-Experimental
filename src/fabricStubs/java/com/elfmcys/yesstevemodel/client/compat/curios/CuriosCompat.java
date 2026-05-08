package com.elfmcys.yesstevemodel.client.compat.curios;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.List;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean hasItemInSlot(LivingEntity entity, String slot, ReferenceOpenHashSet<Item> items) {
        return false;
    }

    public static boolean hasTaggedItemInSlot(LivingEntity entity, String slot, List<TagKey<Item>> tags) {
        return false;
    }

    public static boolean hasNoTaggedItemInSlot(LivingEntity entity, String slot, List<TagKey<Item>> tags) {
        return false;
    }

    public static void registerCuriosItems(ContextBinding binding) {
        binding.function("has_any_curios", Function.NOOP);
        binding.function("has_any_curios_with_all_tags", Function.NOOP);
        binding.function("has_any_curios_with_any_tag", Function.NOOP);
        binding.livingEntityVar("dump_curios", ctx -> {
            ctx.logWarning("Curios not installed.");
            return null;
        });
    }
}
