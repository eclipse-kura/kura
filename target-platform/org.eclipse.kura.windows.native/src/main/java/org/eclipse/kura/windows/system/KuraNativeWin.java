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
