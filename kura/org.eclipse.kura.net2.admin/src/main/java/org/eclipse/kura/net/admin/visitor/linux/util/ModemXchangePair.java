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

/**
 * Defines Modem Exchange "Send/Expect" pair
 *
 * @author ilya.binshtok
 *
 */
public class ModemXchangePair {

    /* send string */
    private String sendString = null;

    /* expect string */
    private String expectString = null;

    /**
     * ModemXchangePair constructor
     *
     * @param send
     *            - 'send' string
     * @param expect
     *            - 'expect' string
     */
    public ModemXchangePair(String send, String expect) {
        this.sendString = send;
        this.expectString = expect;
    }

    /**
     * Reports 'send' string
     *
     * @return 'send' string
     */
    public String getSendString() {
        return this.sendString;
    }

    /**
     * Reports 'expect' string
     *
     * @return 'expect string
     */
    public String getExpectString() {
        return this.expectString;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.expectString);
        buf.append('\t');
        buf.append(this.sendString);

        return buf.toString();
    }
}
