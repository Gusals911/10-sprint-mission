package com.sprint.mission.discodeit.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class EnvPropertiesLoader {
    public static Properties loadEnv() {
        Properties properties = new Properties();
        Path path = Path.of(".env");

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                properties.setProperty(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(".env 파일을 읽을 수 없습니다.",e);
        }

        return properties;
    }
}
