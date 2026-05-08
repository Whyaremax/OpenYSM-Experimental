package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.model.ModelResourceBundle;
import com.elfmcys.yesstevemodel.client.model.PlayerModelBundle;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class TouhouLittleMaidCompat {
    private TouhouLittleMaidCompat() {
    }

    public static void init() {
    }

    public static Object buildControllers(PlayerModelBundle modelBundle, ModelResourceBundle resourceBundle) {
        return null;
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isMaidEntity(Entity entity) {
        return false;
    }

    public static boolean isMaidRideable(Entity entity) {
        return false;
    }

    public static boolean isSimplePlanesEntity(Entity entity) {
        return false;
    }

    public static boolean isImmersiveAircraftEntity(Entity entity) {
        return false;
    }

    public static boolean isMaidItem(Item item) {
        return false;
    }

    public static String getMaidEntityId(Entity entity) {
        return StringPool.EMPTY;
    }

    public static boolean isMaidSitting(LivingEntity entity) {
        return false;
    }

    public static void registerMaidAnimStates(TLMBinding binding) {
        binding.livingEntityVar("is_begging", ctx -> false);
        binding.livingEntityVar("is_sitting", ctx -> false);
        binding.livingEntityVar("has_backpack", ctx -> false);
        binding.livingEntityVar("favorability_point", ctx -> 0);
        binding.livingEntityVar("favorability_level", ctx -> 0);
        binding.livingEntityVar("task_id", ctx -> StringPool.EMPTY);
        binding.livingEntityVar("schedule", ctx -> StringPool.EMPTY);
        binding.livingEntityVar("activity", ctx -> StringPool.EMPTY);
        binding.livingEntityVar("gomoku_win_count", ctx -> 0);
        binding.livingEntityVar("gomoku_rank", ctx -> 1);
        binding.livingEntityVar("game_statue", ctx -> StringPool.EMPTY);
        binding.livingEntityVar("backpack_type", ctx -> StringPool.EMPTY);
        binding.livingEntityVar("is_entity", ctx -> true);
        binding.livingEntityVar("is_statue", ctx -> false);
        binding.livingEntityVar("is_garage_kit", ctx -> false);
        binding.livingEntityVar("show_item", ctx -> StringPool.EMPTY);
    }

    @Nullable
    public static PlayState handleMaidInteraction(AnimationEvent<LivingAnimatable<?>> event, LivingEntity livingEntity, Entity entity) {
        return null;
    }

    public static void syncMaidState(LivingEntity entity) {
    }

    public static boolean isMaidChatAvailable() {
        return false;
    }

    public static void openMaidChat() {
    }
}
