package org.eclipse.kura.nm;

public enum Kura8021xEAP {

    KURA_8021X_EAP_TLS("Kura8021xEapTls"),
    KURA_8021X_EAP_PEAP("Kura8021xEapPeap"),
    KURA_8021X_EAP_TTLS("Kura8021xEapTtls");

    private final String value;

    private Kura8021xEAP(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static Kura8021xEAP fromString(String name) {
        for (Kura8021xEAP eap : Kura8021xEAP.values()) {
            if (eap.getValue().equals(name)) {
                return eap;
            }
        }

        throw new IllegalArgumentException("Invalid EAP type in snapshot: " + name);
    }
}
