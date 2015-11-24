#include <time.h>
#include <windows.h>
#include <jni.h>

JNIEXPORT void JNICALL Java_org_eclipse_kura_windows_KuraNativeWin_setSystemTime(JNIEnv *env, jobject obj, jshort year, jshort month, jshort day, jshort hour, jshort minute, jshort second, jshort msec)
{
    SYSTEMTIME st;
//    GetLocalTime(&st);

    st.wYear = (short)year;
    st.wMonth = (short)month;
    st.wDay = (short)day;
    st.wHour = (short)hour;
    st.wMinute = (short)minute;
    st.wSecond = (short)second;
    st.wMilliseconds = (short)msec;

    SetLocalTime(&st);
}
