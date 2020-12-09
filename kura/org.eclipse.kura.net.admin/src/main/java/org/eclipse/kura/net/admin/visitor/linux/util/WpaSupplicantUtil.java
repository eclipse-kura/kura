/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.visitor.linux.util;

public class WpaSupplicantUtil {

    public static final int MODE_INFRA = 0;
    public static final int MODE_IBSS = 1;
    public static final int MODE_AP = 2;

    private WpaSupplicantUtil() {

    }

    public static int convChannelToFrequency(int channel) {
        int frequency = -1;
        if (channel >= 1 && channel <= 13) {
            frequency = 2407 + channel * 5;
        }
        if (channel == 14) {
            frequency = 2484;
        }
        return frequency;
    }

    public static int convFrequencyToChannel(int mhz) {
        if (mhz == 2484) {
            return 14;
        }
        return (mhz - 2407) / 5;
    }

}
