package net.minecraftforge.common.capabilities;

import net.minecraftforge.common.util.LazyOptional;

public final class Capability<T> {
    private final int id;

    Capability(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public <U> LazyOptional<U> orEmpty(Capability<U> requested, LazyOptional<? extends T> value) {
        if (this == requested) {
            return value.cast();
        }
        return LazyOptional.empty();
    }
}
