package io.github.gaming32.worldhost.origincheck;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface OriginChecker {
    boolean needsChecking(Path file);

    List<URI> findOrigins(Path file);

    void markChecked(Path file);
}
