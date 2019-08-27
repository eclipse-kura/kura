package javax.comm;

/*************************************************************************
 * Copyright (c) 1999, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class SerialPortException extends Exception {
    /**
     * Define the serial version uid (long) constant.
     */
    private static final long serialVersionUID = 1409005327231826071L;

    /**
     * Constructs an instance of this class from the specified msg parameter.
     *
     * @param msg The msg (<code>String</code>) parameter.
     */
    public SerialPortException(final String msg) {
        super(msg);
    }
}
