/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.keystore;

import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.CsrInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.EntryType;
import org.eclipse.kura.core.keystore.util.KeyPairInfo;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KeystoreEntryInfoMapper {

    // Expected resulting json:
    // {
    // "keystoreName" : "name",
    // "alias" : "alias"
    // }
    //
    // or
    //
    // {
    // "keystoreName" : "name",
    // "alias" : "alias",
    // "type" : "TRUSTED_CERTIFICATE",
    // "certificate" : "..."
    // }
    //
    // or
    //
    // {
    // "keystoreName" : "name",
    // "alias" : "alias",
    // "type" : "L",
    // "privateKey" : "..."
    // "certificateChain" : "..."
    // }
    //
    // or
    //
    // {
    // "keystoreName" : "name",
    // "alias" : "alias",
    // "type" : "PRIVATE_KEY",
    // "privateKey" : "..."
    // "certificateChain" : "..."
    // }

    private static final String TYPE = "type";

    private KeystoreEntryInfoMapper() {
        // empty constructor
    }

    public static EntryInfo unmarshal(String jsonString) {
        Gson gson = new Gson();
        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
        JsonElement typeElement = json.get(TYPE);

        if (typeElement == null) {
            return gson.fromJson(json, EntryInfo.class);
        } else {
            String type = typeElement.getAsString();
            if (EntryType.valueOfType(type) == EntryType.TRUSTED_CERTIFICATE) {
                return gson.fromJson(json, CertificateInfo.class);
            } else if (EntryType.valueOfType(type) == EntryType.KEY_PAIR) {
                return gson.fromJson(json, KeyPairInfo.class);
            } else if (EntryType.valueOfType(type) == EntryType.PRIVATE_KEY) {
                return gson.fromJson(json, PrivateKeyInfo.class);
            } else if (EntryType.valueOfType(type) == EntryType.CSR) {
                return gson.fromJson(json, CsrInfo.class);
            } else {
                return gson.fromJson(json, EntryInfo.class);
            }
        }
    }

}
