/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

#include <time.h>
#include <windows.h>
#include <jni.h>

JNIEXPORT void JNICALL Java_org_eclipse_kura_windows_system_KuraNativeWin_setSystemTime(JNIEnv *env, jobject obj, jshort year, jshort month, jshort day, jshort hour, jshort minute, jshort second, jshort msec)
{
    SYSTEMTIME st;
//    GetLocalTime(&st);

    UNREFERENCED_PARAMETER( env );
    UNREFERENCED_PARAMETER( obj );

    st.wYear = (short)year;
    st.wMonth = (short)month;
    st.wDay = (short)day;
    st.wHour = (short)hour;
    st.wMinute = (short)minute;
    st.wSecond = (short)second;
    st.wMilliseconds = (short)msec;

    SetLocalTime(&st);
}

typedef ULONGLONG (WINAPI *__GetTickCount64)(void);

JNIEXPORT jlong JNICALL Java_org_eclipse_kura_windows_system_KuraNativeWin_getTickCount(JNIEnv *env, jobject obj)
{
    HMODULE hDllKenel32;
    __GetTickCount64 pGetTickCount64;
    ULONGLONG ull;

    UNREFERENCED_PARAMETER( env );
    UNREFERENCED_PARAMETER( obj );

    // GetTickCount 32-bit counter will overflow in ~50 days.
    // Use GetTickCount64, which is available on Vista and later, so we need to implement dynamic DLL import

    hDllKenel32 = LoadLibrary("kernel32.dll");
    if( hDllKenel32 && hDllKenel32 != INVALID_HANDLE_VALUE )
    {
        pGetTickCount64 = (__GetTickCount64)GetProcAddress( hDllKenel32, "GetTickCount64" );
        if( pGetTickCount64 )
        {
            ull = pGetTickCount64();
            FreeLibrary( hDllKenel32 );
            return ull;
        }
        FreeLibrary( hDllKenel32 );
    }

    return GetTickCount();
}
