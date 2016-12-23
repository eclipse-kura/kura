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
