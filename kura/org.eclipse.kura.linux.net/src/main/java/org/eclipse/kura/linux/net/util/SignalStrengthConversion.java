/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

public class SignalStrengthConversion {

    private static final byte[] s_lookup = { -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102,
            -101, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80,
            -79, -78, -77, -75, -74, -73, -72, -70, -69, -68, -67, -65, -64, -63, -62, -60, -59, -58, -56, -55, -53,
            -52, -50, -50, -49, -48, -48, -47, -46, -45, -44, -44, -43, -42, -42, -41, -40, -39, -38, -37, -35, -34,
            -33, -32, -30, -29, -28, -27, -25, -24, -23, -22, -20, -19, -18, -17, -16, -15, -14, -13, -12, -10, -10,
            -10, -10, -10, -10, -10 };

    private SignalStrengthConversion() {
    }

    public static int getRssi(int percents) {
        int pcents = percents;
        if (pcents < 0) {
            pcents = 0;
        } else if (pcents > 100) {
            pcents = 100;
        }
        return s_lookup[pcents];
    }
}
