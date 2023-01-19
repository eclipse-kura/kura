package org.eclipse.kura.net2.admin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NetworkProperties {

    private Map<String, Object> properties;

    public NetworkProperties(Map<String, Object> rawProperties) {
        this.properties = Objects.requireNonNull(rawProperties);
    }

    public <T> T get(Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);
        return clazz.cast(this.properties.get(formattedKey));
    }

    public <T> Optional<T> getOpt(Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);
        if (this.properties.containsKey(formattedKey)) {
            return Optional.of(clazz.cast(this.properties.get(formattedKey)));
        } else {
            return Optional.empty();
        }
    }
}
