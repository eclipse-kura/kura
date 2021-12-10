package org.eclipse.kura.linux.position;

import java.util.HashMap;
import java.util.Map;

public enum PositionProviderType {

    SERIAL("serial"),
    GPSD("gpsd");

    private String value;

    private static Map<String, PositionProviderType> valuesMap = new HashMap<>();

    static {
        for (PositionProviderType type : PositionProviderType.values()) {
            valuesMap.put(type.getValue(), type);
        }
    }

    private PositionProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PositionProviderType fromValue(String value) {
        return valuesMap.get(value);
    }

}
