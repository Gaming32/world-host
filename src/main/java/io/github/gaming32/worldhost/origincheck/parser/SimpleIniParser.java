package io.github.gaming32.worldhost.origincheck.parser;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleIniParser {
    public static Map<String, Map<String, String>> parse(BufferedReader reader) throws IOException {
        final var result = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> section = new LinkedHashMap<>();
        result.put("", section);

        String line;
        while ((line = reader.readLine()) != null) {
            line = StringUtils.substringBefore(line, ';').trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                section = result.computeIfAbsent(line.substring(1, line.length() - 1), k -> new LinkedHashMap<>());
                continue;
            }
            final var split = line.split("=", 2);
            if (split.length != 2) continue;
            section.put(split[0].trim(), split[1].trim());
        }

        return result;
    }
}
