package org.eclipse.kura.windows.clock;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase.SYSTEMTIME;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Provides access to the Windows SetSystemTime native API call.
 * This class is based on examples found in
 * <a href="https://github.com/twall/jna/blob/master/www/GettingStarted.md">JNA Getting Started</a>
 */
public class WindowsSetSystemTime {

    /**
     * Kernel32 DLL Interface.
     * kernel32.dll uses the __stdcall calling convention (check the function 
     * declaration for "WINAPI" or "PASCAL"), so extend StdCallLibrary
     * Most C libraries will just extend com.sun.jna.Library,
     */
    public interface Kernel32 extends StdCallLibrary {

        boolean SetLocalTime(SYSTEMTIME st);

        Kernel32 instance = (Kernel32) Native.loadLibrary("kernel32.dll", Kernel32.class);

    }

    public boolean SetLocalTime(SYSTEMTIME st) {
        return Kernel32.instance.SetLocalTime(st);
    }

    public boolean SetLocalTime(short wYear, short wMonth, short wDay, short wHour, short wMinute, short wSecond) {
        SYSTEMTIME st = new SYSTEMTIME();
        st.wYear = wYear;
        st.wMonth = wMonth;
        st.wDay = wDay;
        st.wHour = wHour;
        st.wMinute = wMinute;
        st.wSecond = wSecond;
        return SetLocalTime(st);
    }       
}
