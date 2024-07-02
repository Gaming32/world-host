package io.github.gaming32.worldhost.config.option;

import com.google.common.collect.ImmutableMap;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.config.ConfigProperty;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import org.jetbrains.annotations.Nullable;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ConfigOptions {
    public static final Map<String, ? extends ConfigOption<?>> OPTIONS = initConfigOptions();

    static {
        OPTIONS.get("UPnP").onSet(WorldHost::scanUpnp);
    }

    private ConfigOptions() {
    }

    private static Map<String, ? extends ConfigOption<?>> initConfigOptions() {
        try {
            return Arrays.stream(Introspector.getBeanInfo(WorldHostConfig.class).getPropertyDescriptors())
                .map(ConfigOptions::createConfigOption)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ConfigOption::getOrder))
                .collect(ImmutableMap.toImmutableMap(ConfigOption::getName, Function.identity()));
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public static ConfigOption<?> createConfigOption(PropertyDescriptor property) {
        if (property.getReadMethod() == null || !property.getReadMethod().isAnnotationPresent(ConfigProperty.class)) {
            return null;
        }
        if (property.getWriteMethod() == null) {
            throw new IllegalArgumentException("Read-only config option " + property.getName());
        }
        if (property.getPropertyType() == boolean.class) {
            return new YesNoOption(property);
        }
        if (property.getPropertyType() == String.class) {
            return new StringOption(property);
        }
        if (property.getPropertyType().isEnum()) {
            return new EnumOption<>(property);
        }
        throw new IllegalArgumentException("Config option with unsupported type: " + property);
    }
}
