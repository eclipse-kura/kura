package org.eclipse.kura.nm.enums;

public enum NM8021xPhase2Auth {

    EAP("eap"),
    MSCHAPV2("mschapv2"),
    GTC("gtc"),
    OTP("otp"),
    MD5("md5"),
    TLS("tls");

    private String value;

    private NM8021xPhase2Auth(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
