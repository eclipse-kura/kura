package org.eclipse.kura.web.shared.model;

public enum Gwt8021xInnerAuth {

    NONE("Kura8021xInnerAuthNone"),
    MSCHAPV2("Kura8021xInnerAuthMschapv2");

    private final String label;

    Gwt8021xInnerAuth(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Gwt8021xInnerAuth fromMetatypeString(String label) {
        for (Gwt8021xInnerAuth innerAuth : Gwt8021xInnerAuth.values()) {
            if (innerAuth.getLabel().equals(label)) {
                return innerAuth;
            }
        }
        return Gwt8021xInnerAuth.NONE;
    }
}
