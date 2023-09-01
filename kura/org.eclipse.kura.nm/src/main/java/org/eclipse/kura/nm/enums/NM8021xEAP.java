package org.eclipse.kura.nm.enums;

public enum NM8021xEAP {

    LEAP("leap"),
    MD5("md5"),
    TLS("tls"),
    PEAP("peap"),
    TTLS("ttls"),
    PWD("pwd"),
    FAST("fast");

    private String value;

    private NM8021xEAP(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
