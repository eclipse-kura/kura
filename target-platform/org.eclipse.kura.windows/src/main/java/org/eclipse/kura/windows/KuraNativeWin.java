package org.eclipse.kura.windows;

import java.io.IOException;

public class KuraNativeWin {

    public native void setSystemTime( short year, short month, short day, short hour, short minutes, short second, short msec );
    public native long getTickCount( );

    static {
	String processor = System.getProperty("org.osgi.framework.processor");
	try {
/*		if (processor.equalsIgnoreCase("x86-64")) {
			System.loadLibrary("KuraNativeWin64");
		} else {*/
			System.loadLibrary("KuraNativeWin");
/*		}*/
	} catch (final UnsatisfiedLinkError e) {
		e.printStackTrace();
	}
    }
}
