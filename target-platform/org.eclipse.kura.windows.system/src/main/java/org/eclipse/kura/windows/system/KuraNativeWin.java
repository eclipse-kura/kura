/*******************************************************************************
 * Copyright (c) 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.windows.system;

import java.io.IOException;

public class KuraNativeWin {

	public native void setSystemTime( short year, short month, short day, short hour, short minutes, short second, short msec );
	public native long getTickCount( );

	static {
		try {
			System.loadLibrary("KuraNativeWin");
		} 
	
		catch (final UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
    }
}
