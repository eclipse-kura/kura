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
import java.util.*;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public interface SerialPortEventListener extends EventListener {
	/**
	 * Serial event with the specified ev parameter.
	 * @param ev	The ev (<code>SerialPortEvent</code>) parameter.
	 */
	public abstract void serialEvent(final SerialPortEvent ev);
}
