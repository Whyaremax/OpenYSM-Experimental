package net.minecraftforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.tags.ITagManager;

public final class ForgeRegistries {
    public static final RegistryFacade<Item> ITEMS = new RegistryFacade<>(BuiltInRegistries.ITEM);
    public static final RegistryFacade<Block> BLOCKS = new RegistryFacade<>(BuiltInRegistries.BLOCK);
    public static final RegistryFacade<EntityType<?>> ENTITY_TYPES = new RegistryFacade<>(BuiltInRegistries.ENTITY_TYPE);
    public static final RegistryFacade<MobEffect> MOB_EFFECTS = new RegistryFacade<>(BuiltInRegistries.MOB_EFFECT);
    public static final RegistryFacade<Enchantment> ENCHANTMENTS = new RegistryFacade<>(BuiltInRegistries.ENCHANTMENT);
    public static final RegistryFacade<Biome> BIOMES = new RegistryFacade<>(Registries.BIOME);
    public static final RegistryFacade<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = new RegistryFacade<>(BuiltInRegistries.SOUND_EVENT);

    private ForgeRegistries() {
    }

    public static final class RegistryFacade<T> {
        private final Registry<T> registry;
        private final ResourceKey<? extends Registry<T>> registryKey;

        private RegistryFacade(Registry<T> registry) {
            this.registry = registry;
            this.registryKey = registry.key();
        }

        private RegistryFacade(ResourceKey<? extends Registry<T>> registryKey) {
            this.registry = null;
            this.registryKey = registryKey;
        }

        public ResourceLocation getKey(T value) {
            if (registry == null) {
                return null;
            }
            return registry.getKey(value);
        }

        public T getValue(ResourceLocation id) {
            if (registry == null) {
                return null;
            }
            return registry.get(id);
        }

        public ITagManager<T> tags() {
            return registry != null ? new ITagManager<>(registry) : new ITagManager<>(registryKey);
        }

        public Registry<T> vanilla() {
            return registry;
        }
    }
}
