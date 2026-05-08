package net.minecraftforge.common;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.function.Supplier;

public final class ForgeMod {
    public static final Supplier<Attribute> BLOCK_REACH = () -> Attributes.ATTACK_DAMAGE;
    public static final Supplier<Attribute> ENTITY_REACH = () -> Attributes.ATTACK_DAMAGE;
    public static final Supplier<Attribute> SWIM_SPEED = () -> Attributes.MOVEMENT_SPEED;
    public static final Supplier<Attribute> ENTITY_GRAVITY = () -> Attributes.MOVEMENT_SPEED;
    public static final Supplier<Attribute> STEP_HEIGHT_ADDITION = () -> Attributes.MOVEMENT_SPEED;
    public static final Supplier<Attribute> NAMETAG_DISTANCE = () -> Attributes.FOLLOW_RANGE;

    private ForgeMod() {
    }
}
