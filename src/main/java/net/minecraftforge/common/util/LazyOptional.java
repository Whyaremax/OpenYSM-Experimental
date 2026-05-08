package net.minecraftforge.common.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LazyOptional<T> {
    private static final LazyOptional<?> EMPTY = new LazyOptional<>(null);

    private final Supplier<? extends T> supplier;
    private boolean resolved;
    private T value;

    private LazyOptional(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    public static <T> LazyOptional<T> of(Supplier<? extends T> supplier) {
        return new LazyOptional<>(supplier);
    }

    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> empty() {
        return (LazyOptional<T>) EMPTY;
    }

    public boolean isPresent() {
        return resolve().isPresent();
    }

    public void ifPresent(Consumer<? super T> consumer) {
        resolve().ifPresent(consumer);
    }

    public T orElse(T other) {
        return resolve().orElse(other);
    }

    public T orElseGet(Supplier<? extends T> other) {
        return resolve().orElseGet(other);
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        return resolve().map(mapper);
    }

    public Optional<T> resolve() {
        if (supplier == null) {
            return Optional.empty();
        }
        if (!resolved) {
            value = supplier.get();
            resolved = true;
        }
        return Optional.ofNullable(value);
    }

    @SuppressWarnings("unchecked")
    public <U> LazyOptional<U> cast() {
        return (LazyOptional<U>) this;
    }
}
