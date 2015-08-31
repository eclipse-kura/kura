package org.eclipse.kura.windows.clock;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
//import com.sun.jna.win32.StdCallLibrary;

/**
 * Provides access to the Windows SetSystemTime native API call.
 * This class is based on examples found in
 * <a href="https://github.com/twall/jna/blob/master/www/GettingStarted.md">JNA Getting Started</a>
 */
public class WindowsSetSystemTime {

    public boolean SetLocalTime(int wYear, int wMonth, int wDay, int wHour, int wMinute, int wSecond) {
        WinBase.SYSTEMTIME st = new WinBase.SYSTEMTIME();
        st.wYear = (short)wYear;
        st.wMonth = (short)wMonth;
        st.wDay = (short)wDay;
        st.wHour = (short)wHour;
        st.wMinute = (short)wMinute;
        st.wSecond = (short)wSecond;
        return Kernel32.INSTANCE.SetLocalTime(st);
    }       
}
