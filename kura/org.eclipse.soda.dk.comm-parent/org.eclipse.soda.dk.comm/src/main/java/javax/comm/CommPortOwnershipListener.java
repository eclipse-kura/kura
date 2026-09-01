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
public interface CommPortOwnershipListener extends EventListener {
	/**
	 * Define the port owned (int) constant.
	 */
	public static final int PORT_OWNED = 1;

	/**
	 * Define the port unowned (int) constant.
	 */
	public static final int PORT_UNOWNED = 2;

	/**
	 * Define the port ownership requested (int) constant.
	 */
	public static final int PORT_OWNERSHIP_REQUESTED = 3;

	/**
	 * Ownership change with the specified type parameter.
	 * @param type	The type (<code>int</code>) parameter.
	 */
	public abstract void ownershipChange(final int type);
}
