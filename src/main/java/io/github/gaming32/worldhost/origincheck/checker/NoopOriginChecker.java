package io.github.gaming32.worldhost.origincheck.checker;

import io.github.gaming32.worldhost.origincheck.OriginChecker;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class NoopOriginChecker implements OriginChecker {
    @Override
    public boolean needsChecking(Path file) {
        return false;
    }

    @Override
    public List<URI> findOrigins(Path file) {
        return List.of();
    }

    @Override
    public void markChecked(Path file) {
    }
}
