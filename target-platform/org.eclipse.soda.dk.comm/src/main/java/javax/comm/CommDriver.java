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
public interface CommDriver {
	/**
	 * Get comm port with the specified port name and port type parameters and return the CommPort result.
	 * @param portName	The port name (<code>String</code>) parameter.
	 * @param portType	The port type (<code>int</code>) parameter.
	 * @return	Results of the get comm port (<code>CommPort</code>) value.
	 */
	public abstract CommPort getCommPort(final String portName, final int portType);

	/**
	 * Initialize.
	 */
	public abstract void initialize();
}
