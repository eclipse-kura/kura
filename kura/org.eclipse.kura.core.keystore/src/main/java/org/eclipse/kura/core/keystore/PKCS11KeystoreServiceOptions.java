/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.keystore;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.configuration.Property;

public class PKCS11KeystoreServiceOptions {

    private static final Property<String> PKCS11_LIBRARY_PROPERTY = new Property<>("library.path", String.class);
    private static final Property<String> PIN_PROPERTY = new Property<>("pin", String.class);
    private static final Property<Integer> SLOT_PROPERTY = new Property<>("slot", Integer.class);
    private static final Property<Integer> SLOT_LIST_INDEX_PROPERTY = new Property<>("slot.list.index", Integer.class);
    private static final Property<String> ENABLED_MECHANISMS_PROPERTY = new Property<>("enabled.mechanisms",
            String.class);
    private static final Property<String> DISABLED_MECHANISMS_PROPERTY = new Property<>("disabled.mechanisms",
            String.class);
    private static final Property<String> ATTRIBUTES_PROPERTY = new Property<>("attributes", String.class);
    private static final Property<String> CRL_STORE_PATH = new Property<>("crl.store.path", String.class);

    private final String ownPid;
    private final Optional<String> libraryPath;
    private final Optional<String> pin;
    private final Optional<Integer> slot;
    private final Optional<Integer> slotListIndex;
    private final Optional<String> enabledMechanisms;
    private final Optional<String> disabledMechanisms;
    private final Optional<String> attributes;
    private final Optional<String> crlStorePath;

    public PKCS11KeystoreServiceOptions(final Map<String, Object> properties, final String ownPid) {
        this.libraryPath = PKCS11_LIBRARY_PROPERTY.getOptional(properties);
        this.pin = PIN_PROPERTY.getOptional(properties);
        this.ownPid = ownPid;
        this.slot = SLOT_PROPERTY.getOptional(properties);
        this.slotListIndex = SLOT_LIST_INDEX_PROPERTY.getOptional(properties);
        this.enabledMechanisms = ENABLED_MECHANISMS_PROPERTY.getOptional(properties).filter(s -> !s.trim().isEmpty());
        this.disabledMechanisms = DISABLED_MECHANISMS_PROPERTY.getOptional(properties).filter(s -> !s.trim().isEmpty());
        this.attributes = ATTRIBUTES_PROPERTY.getOptional(properties).filter(s -> !s.trim().isEmpty());
        this.crlStorePath = CRL_STORE_PATH.getOptional(properties).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getLibraryPath() {
        return libraryPath;
    }

    public Optional<char[]> getPin(final CryptoService cryptoService) throws KuraException {
        if (!pin.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(cryptoService.decryptAes(pin.get().toCharArray()));
    }

    public String getOwnPid() {
        return ownPid;
    }

    public Optional<Integer> getSlot() {
        return slot;
    }

    public Optional<Integer> getSlotListIndex() {
        return slotListIndex;
    }

    public Optional<String> getEnabledMechanisms() {
        return enabledMechanisms;
    }

    public Optional<String> getDisabledMechanisms() {
        return disabledMechanisms;
    }

    public Optional<String> getAttributes() {
        return attributes;
    }

    public Optional<String> getCrlStorePath() {
        return crlStorePath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, crlStorePath, disabledMechanisms, enabledMechanisms, libraryPath, ownPid, pin,
                slot, slotListIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PKCS11KeystoreServiceOptions)) {
            return false;
        }
        PKCS11KeystoreServiceOptions other = (PKCS11KeystoreServiceOptions) obj;
        return Objects.equals(attributes, other.attributes) && Objects.equals(crlStorePath, other.crlStorePath)
                && Objects.equals(disabledMechanisms, other.disabledMechanisms)
                && Objects.equals(enabledMechanisms, other.enabledMechanisms)
                && Objects.equals(libraryPath, other.libraryPath) && Objects.equals(ownPid, other.ownPid)
                && Objects.equals(pin, other.pin) && Objects.equals(slot, other.slot)
                && Objects.equals(slotListIndex, other.slotListIndex);
    }

    public Optional<String> buildSunPKCS11ProviderConfig() {
        if (!this.libraryPath.isPresent()) {
            return Optional.empty();
        }

        final StringBuilder builder = new StringBuilder();

        builder.append("library = ").append(libraryPath.get()).append('\n');

        builder.append("name = kura.provider.").append(ownPid).append('\n');

        if (slot.isPresent()) {
            builder.append("slot = ").append(slot.get()).append('\n');
        }

        if (slotListIndex.isPresent()) {
            builder.append("slotListIndex = ").append(slotListIndex.get()).append('\n');
        }

        if (enabledMechanisms.isPresent()) {
            builder.append("enabledMechanisms = { ").append(enabledMechanisms.get()).append(" }\n");
        }

        if (disabledMechanisms.isPresent()) {
            builder.append("disabledMechanisms = { ").append(disabledMechanisms.get()).append(" }\n");
        }

        if (attributes.isPresent()) {
            builder.append(attributes.get());
        }

        return Optional.of(builder.toString());
    }

}