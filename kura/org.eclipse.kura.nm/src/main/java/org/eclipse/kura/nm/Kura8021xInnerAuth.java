package org.eclipse.kura.nm;

public enum Kura8021xInnerAuth {

    KURA_8021X_INNER_AUTH_NONE("Kura8021xInnerAuthNone"),
    KURA_8021X_INNER_AUTH_MSCHAPV2("Kura8021xInnerAuthMschapv2");

    private final String value;

    private Kura8021xInnerAuth(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static Kura8021xInnerAuth fromString(String name) {
        for (Kura8021xInnerAuth auth : Kura8021xInnerAuth.values()) {
            if (auth.getValue().equals(name)) {
                return auth;
            }
        }

        throw new IllegalArgumentException("Invalid inner auth type in snapshot: " + name);
    }
}
