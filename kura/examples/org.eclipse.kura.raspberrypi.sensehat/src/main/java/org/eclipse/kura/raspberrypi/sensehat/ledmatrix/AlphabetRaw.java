/*******************************************************************************
 * Copyright (c) 2018, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.raspberrypi.sensehat.ledmatrix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlphabetRaw {

    private static final int LETTER_SIZE = 8 * 8;

    private static final Logger logger = LoggerFactory.getLogger(AlphabetRaw.class);

    private static final String ALPHABET = " +-*/!\"#$><0123456789.=)(ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz?,;:|@%[&_']\\~";

    private static Map<Character, byte[]> letters = new HashMap<>();

    public AlphabetRaw(URL url) {

        int c;
        try (InputStream is = url.openStream();) {
            for (int i = 0; i < 91; i++) {
                final byte[] letter = new byte[LETTER_SIZE];
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        final int off = (8 - 1 - x) * 8 + (y);
                        if (y == 0 || y == 6 || y == 7) {
                            // Add whitespaces
                            letter[off] = 0;
                        } else {
                            c = is.read();
                            if (c == '\n') {
                                c = is.read();
                            }
                            if (c == 48) { // ASCII 48 -> 0 (inverted)
                                letter[off] = 1;
                            } else {
                                letter[off] = 0;
                            }
                        }
                    }
                }
                letters.put(ALPHABET.charAt(i), letter);
            }
        } catch (IOException e) {
            logger.error("Error in opening Alphabet file.", e);
        }
    }

    public byte[] getLetter(Character letter) {
        return letters.get(letter);
    }

    public boolean isAvailable(Character letter) {
        return ALPHABET.indexOf(letter) != -1;
    }
}
