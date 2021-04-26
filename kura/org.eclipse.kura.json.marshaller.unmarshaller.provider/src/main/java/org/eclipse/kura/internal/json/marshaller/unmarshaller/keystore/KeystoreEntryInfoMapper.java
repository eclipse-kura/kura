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
import org.eclipse.kura.core.keystore.util.KeyPairInfo;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KeystoreEntryInfoMapper {

    private KeystoreEntryInfoMapper() {
        // empty constructor
    }

    public static EntryInfo unmarshal(String jsonString, Class<?> clazz) {
        Gson gson = new Gson();
        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

        if (clazz.equals(CertificateInfo.class)) {
            return gson.fromJson(json, CertificateInfo.class);
        } else if (clazz.equals(KeyPairInfo.class)) {
            return gson.fromJson(json, KeyPairInfo.class);
        } else if (clazz.equals(PrivateKeyInfo.class)) {
            return gson.fromJson(json, PrivateKeyInfo.class);
        } else if (clazz.equals(CsrInfo.class)) {
            return gson.fromJson(json, CsrInfo.class);
        } else {
            return gson.fromJson(json, EntryInfo.class);
        }

    }

}
