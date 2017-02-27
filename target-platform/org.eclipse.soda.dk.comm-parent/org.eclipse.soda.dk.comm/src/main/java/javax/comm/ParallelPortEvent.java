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
public class ParallelPortEvent extends EventObject {
	/**
	 * Define the serial version uid (long) constant.
	 */
	private static final long serialVersionUID = 3956965758537808226L;

	/**
	 * Define the par ev error (int) constant.
	 */
	public static final int PAR_EV_ERROR = 1;

	/**
	 * Define the par ev buffer (int) constant.
	 */
	public static final int PAR_EV_BUFFER = 2;

	/**
	 * Define the event type (int) field.
	 */
	public int eventType;

	/**
	 * Define the new val (boolean) field.
	 */
	private boolean newVal;

	/**
	 * Define the old val (boolean) field.
	 */
	private boolean oldVal;

	/**
	 * Constructs an instance of this class from the specified srcport, eventtype, oldvalue and newvalue parameters.
	 * @param srcport	The srcport (<code>ParallelPort</code>) parameter.
	 * @param eventtype	The eventtype (<code>int</code>) parameter.
	 * @param oldvalue	The oldvalue (<code>boolean</code>) parameter.
	 * @param newvalue	The newvalue (<code>boolean</code>) parameter.
	 */
	public ParallelPortEvent(final ParallelPort srcport, final int eventtype, final boolean oldvalue, final boolean newvalue) {
		super(srcport);
		this.eventType = eventtype;
		this.newVal = newvalue;
		this.oldVal = oldvalue;
	}

	/**
	 * Gets the event type (int) value.
	 * @return	The event type (<code>int</code>) value.
	 */
	public int getEventType() {
		return this.eventType;
	}

	/**
	 * Gets the new value (boolean) value.
	 * @return	The new value (<code>boolean</code>) value.
	 */
	public boolean getNewValue() {
		return this.newVal;
	}

	/**
	 * Gets the old value (boolean) value.
	 * @return	The old value (<code>boolean</code>) value.
	 */
	public boolean getOldValue() {
		return this.oldVal;
	}
}
