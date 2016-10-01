/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.deployment.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class HashUtil {

    public static String hash(String digestAlgorithm, File file) throws IOException, Exception {
        MessageDigest cript = null;
        FileInputStream fis = null;
        try {
            cript = MessageDigest.getInstance(digestAlgorithm);
            fis = new FileInputStream(file);

            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                cript.update(byteArray, 0, bytesCount);
            }
            byte[] encodedBytes = cript.digest();
            StringBuilder sb = new StringBuilder();
            for (byte encodedByte : encodedBytes) {
                sb.append(Integer.toString((encodedByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.STORE_ERROR, null, e.getMessage());
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.STORE_ERROR, null, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

}
