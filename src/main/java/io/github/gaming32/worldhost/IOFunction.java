package io.github.gaming32.worldhost;

import java.io.IOException;

@FunctionalInterface
public interface IOFunction<T, R> {
    R apply(T t) throws IOException;
}
