package net.minecraftforge.common.capabilities;

public interface ICapabilitySerializable<T> extends ICapabilityProvider {
    T serializeNBT();

    void deserializeNBT(T tag);
}
