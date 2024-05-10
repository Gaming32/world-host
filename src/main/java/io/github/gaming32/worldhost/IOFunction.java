package io.github.gaming32.worldhost;

import java.io.IOException;

// TODO: Remove when 1.18.2 is minimum
@FunctionalInterface
public interface IOFunction<T, R> {
    R apply(T t) throws IOException;
}
