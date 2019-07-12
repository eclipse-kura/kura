package org.eclipse.kura.example.led;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class LedOptions {

    private static final String PROPRTY_LED_NOME = "switchLed";

    private static final boolean PROPRTY_LED_DEFAULT = false;

    private final boolean enableLed;

    public LedOptions(Map<String, Object> properties) {

        requireNonNull(properties, "Required not null");
        this.enableLed = getProperty(properties, PROPRTY_LED_NOME, PROPRTY_LED_DEFAULT);

    }

    public boolean isEnableLed() {
        return this.enableLed;
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(Map<String, Object> properties, String propertyName, T defaultValue) {
        Object prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null && prop.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) prop;
        } else {
            return defaultValue;
        }
    }

}
