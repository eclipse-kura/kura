/*******************************************************************************
 * Copyright (c) 2017 Amit Kumar Mondal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire;

import org.eclipse.kura.wire.WireConfiguration;

/**
 * The Enum {@link WireConstants} contains all the necessary constants required for
 * wiring services
 */
public enum WireConstants {

    /** Emitter Postfix to be used for {@link WireConfiguration}s */
    EMITTER_POSTFIX("emitter"),

    /** Receiver Postfix to be used for {@link WireConfiguration}s */
    RECEIVER_POSTFIX("receiver"),

    /** Filter Postfix to be used for {@link WireConfiguration}s */
    FILTER_POSTFIX("filter");

    /** The value. */
    private String value;

    /** Constructor */
    private WireConstants(final String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the constant
     *
     * @return the string representation
     */
    public String value() {
        return this.value;
    }

}
