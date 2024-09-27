package org.eclipse.kura.linux.position;

import java.util.HashMap;
import java.util.Map;

public enum GNSSType {

    BEIDOU("Beidou"),
    GALILEO("Galileo"),
    GLONASS("Glonass"),
    GPS("GPS"),
    MIXED_GNSS_TYPE("MixedGNSSTypes"),
    OTHER("Other"),
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

    public static GNSSType fromPrn(int prnId) {
        if (prnId >= 1 && prnId <= 63) {
            return valuesMap.get("GPS");
        } else if (prnId >= 64 && prnId <= 96) {
            return valuesMap.get("Glonass");
        } else {
            return valuesMap.get("Other");
        }
    }

    public static GNSSType fromTypes(String type) {
        GNSSType gnssType = GNSSType.UNKNOWN;

        if (type.equals("GP")) {
            gnssType = GNSSType.GPS;
        } else if (type.equals("BD") || type.equals("GB")) {
            gnssType = GNSSType.BEIDOU;
        } else if (type.equals("GA")) {
            gnssType = GNSSType.GALILEO;
        } else if (type.equals("GL")) {
            gnssType = GNSSType.GLONASS;
        }

        return gnssType;
    }
}