package org.eclipse.kura.position;

import java.util.HashMap;
import java.util.Map;

public enum GNSSType {

    BEIDOU("Beidou"),
    GALILEO("Galileo"),
    GLONASS("Glonass"),
    GPS("Gps"),
    IRNSS("IRNSS"),
    OTHER("Other"),
    QZSS("QZSS"),
    UNKNOWN("Unknown");

    private String value;

    private static Map<String, GNSSType> valuesMap = new HashMap<>();

    static {
        for (GNSSType type : GNSSType.values()) {
            valuesMap.put(type.getValue(), type);
        }
    }

    private GNSSType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static GNSSType fromValue(String value) {
        return valuesMap.get(value);
    }
}