package org.eclipse.kura.windows.clock;

import org.eclipse.kura.windows.system.KuraNativeWin;

public class WindowsSetSystemTime {

    public boolean SetLocalTime(int wYear, int wMonth, int wDay, int wHour, int wMinute, int wSecond) {
        KuraNativeWin nativeWin = new KuraNativeWin();
        nativeWin.setSystemTime( (short)wYear, (short)wMonth, (short)wDay, (short)wHour, (short)wMinute, (short)wSecond, (short)0);
        return true;
    }       
}
