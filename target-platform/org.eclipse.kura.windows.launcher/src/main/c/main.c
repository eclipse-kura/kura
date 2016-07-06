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

#include <windows.h>
#include "KURALauncher.h"


HINSTANCE hInst;								// current instance
TCHAR szTitle[] = "KURALauncher";
TCHAR szWindowClass[] = "KURALAUNCHER";

ATOM				MyRegisterClass(HINSTANCE hInstance);
BOOL				InitInstance(HINSTANCE, int);
LRESULT CALLBACK	WndProc(HWND, UINT, WPARAM, LPARAM);

#define KURA_CMD_LINE_SIZE			32768
char *g_pszKuraCommandLine = NULL;

BOOL g_fKuraStarterActive = FALSE;
HANDLE g_hKuraStarterThread = NULL;
HANDLE g_hKuraProcess = NULL;

// This thread starts a child process which executes the Kura command (java).
// In case the kura process terminates unexpectedly, it attempts to restart it
DWORD WINAPI KuraStarter( LPVOID lpThreadParameter )
{
	STARTUPINFO mStartInfo;
	PROCESS_INFORMATION mProcInfo;

	UNREFERENCED_PARAMETER( lpThreadParameter );

	memset(&mStartInfo, 0, sizeof(mStartInfo));
	mStartInfo.cb = sizeof(STARTUPINFO); 
	mStartInfo.dwFlags = STARTF_USESTDHANDLES;

	while( g_fKuraStarterActive )
	{
		// Create the two pipes used for the STDIN and STDOUT of the KURA process
		if(CreatePipes())
		{
			// Create a thread to read the STDOUT data comming from the KURA process
			if(CreateThread(NULL, 0, STDOUT_Thread, NULL, 0, NULL))
			{
				mStartInfo.hStdInput  = g_hINRead;
				mStartInfo.hStdError = g_hOUTWrite;
				mStartInfo.hStdOutput = g_hOUTWrite;

				// Start the KURA process (java) and don't show the console window
				if(CreateProcess(NULL, g_pszKuraCommandLine, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &mStartInfo, &mProcInfo))
				{
					g_hKuraProcess = mProcInfo.hProcess;
					// Wait for the KURA process to terminate, then clean up the handles associated with the process
					WaitForSingleObject(mProcInfo.hProcess, INFINITE);
					CloseHandle(mProcInfo.hThread);
					CloseHandle(mProcInfo.hProcess);
				}
				else
				{
					// LOG: Unable to start process
					Sleep( 2000 );
				}
			}
			else
			{
				// LOG: Unable to create STDIO thread 
				Sleep( 2000 );
			}

			// Close the handles associated with the STDIO pipes a new set gets created if required
			CloseHandle(g_hINWrite);
			CloseHandle(g_hINRead);
			CloseHandle(g_hOUTWrite);
			CloseHandle(g_hOUTRead);
		}
		else
		{
			// LOG: Unable to create pipes 
			Sleep( 2000 );
		}
	}

	return 0;
}

#if defined(_AMD64_)
char KuraJavaArgs[] = "java \
-Xms256m -Xmx256m -Dkura.os.version=win32 \
-Dkura.arch=x86_64 \
-Dtarget.device=windows \
-Declipse.ignoreApp=true \
-Dkura.home=%KURA_HOME% \
-Dkura.data.dir=%KURA_DATA% \
-Dkura.configuration=file:%KURA_HOME_URI%/kura.properties \
-Dkura.custom.configuration=file:%KURA_HOME_URI%/kura_custom.properties \
-Ddpa.configuration=%KURA_HOME%\\dpa.properties \
-Dlog4j.configuration=file:%KURA_HOME_URI%/log4j.properties \
-Djdk.tls.trustNameService=true \
-jar \"%~dp0\\plugins\\org.eclipse.osgi_3.8.1.v20120830-144521.jar\" \
-configuration %KURA_TEMP%\\Configuration \
-console \
-consoleLog \
";
#else
char KuraJavaArgs[] = "java \
-Xms256m -Xmx256m -Dkura.os.version=win32 \
-Dkura.arch=x86 \
-Dtarget.device=windows \
-Declipse.ignoreApp=true \
-Dkura.home=%KURA_HOME% \
-Dkura.data.dir=%KURA_DATA% \
-Dkura.configuration=file:%KURA_HOME_URI%/kura.properties \
-Dkura.custom.configuration=file:%KURA_HOME_URI%/kura_custom.properties \
-Ddpa.configuration=%KURA_HOME%\\dpa.properties \
-Dlog4j.configuration=file:%KURA_HOME_URI%/log4j.properties \
-Djdk.tls.trustNameService=true \
-jar \"%~dp0\\plugins\\org.eclipse.osgi_3.8.1.v20120830-144521.jar\" \
-configuration %KURA_TEMP%\\Configuration \
-console \
-consoleLog \
";
#endif

// Replace substr with replacement in string and return number of replacements made
// Caller should provide a buffer large enough for the replaced string to fit it
int str_replace ( char *string, size_t max_buf, const char *substr, const char *replacement )
{
  char *tok = NULL;
  char *newstr = NULL;
  char *oldstr = NULL;
  char *head = NULL;
  int cnt = 0;
 
  if ( substr == NULL || replacement == NULL )
	  return 0;

  newstr = _strdup (string);
  head = newstr;

  while ( (tok = strstr ( head, substr )) )
  {
    oldstr = newstr;
    newstr = malloc ( strlen ( oldstr ) - strlen ( substr ) + strlen ( replacement ) + 1 );
    /*failed to alloc mem, free old string and return NULL */
    if ( newstr == NULL )
	{
      free (oldstr);
      return cnt;
    }
    memcpy ( newstr, oldstr, tok - oldstr );
    memcpy ( newstr + (tok - oldstr), replacement, strlen ( replacement ) );
    memcpy ( newstr + (tok - oldstr) + strlen( replacement ), tok + strlen ( substr ), strlen ( oldstr ) - strlen ( substr ) - ( tok - oldstr ) );
    memset ( newstr + strlen ( oldstr ) - strlen ( substr ) + strlen ( replacement ) , 0, 1 );
    /* move back head right after the last replacement */
    head = newstr + (tok - oldstr) + strlen( replacement );
    free (oldstr);
	cnt++;
  }

  // now copy back the new string into the original one based on max buffer size
  memset( string, 0, max_buf );
  memcpy( string, newstr, min(max_buf, strlen(newstr)) );
  free( newstr );
  return cnt;
}

// This appends a backslash at the end of string if not already present
void append_backslash( char *str, size_t max_buf )
{
	if( str[ strlen(str)-1 ] != '\\' )
		strcat_s( str, max_buf, "\\" );
}


char *PrepareCommandLine( LPTSTR lpInstallDir )
{
	char szInstallPath[MAX_PATH*2];
	char szProgramDataPath[MAX_PATH*2];
	char szKuraHomePath[MAX_PATH*2];
	char szKuraHomeURI[MAX_PATH*2];
	char szKuraDataPath[MAX_PATH*2];
	char szKuraTempPath[MAX_PATH*2];
	char szKuraHomePathCfg[MAX_PATH*2];
	char szKuraTempPathCfg[MAX_PATH*2];
	char *buf;

	buf = malloc( KURA_CMD_LINE_SIZE );
	if( !buf )
	{
		MessageBox( NULL, "Kura Launcher v1.0\n\nNot enough memory", "Error", MB_ICONERROR );
		return NULL;
	}

	// Get the first argument, which should contain the installation path
	strcpy_s( szInstallPath, sizeof(szInstallPath), lpInstallDir );
	// Remove quotes in case they were present
	str_replace( szInstallPath, sizeof(szInstallPath), "\"", "" );
	append_backslash( szInstallPath, sizeof(szInstallPath) );

	// Get the folder where Kura Data files are stored (this is by default C:\ProgramData)
	GetEnvironmentVariable( "ALLUSERSPROFILE", szProgramDataPath, sizeof(szProgramDataPath) );

	// Now create the KURA_HOME and KURA_DATA paths
	strcpy_s( szKuraHomePath, sizeof(szKuraHomePath), szProgramDataPath );
	strcpy_s( szKuraDataPath, sizeof(szKuraDataPath), szProgramDataPath );
	append_backslash( szKuraHomePath, sizeof(szKuraHomePath) );
	append_backslash( szKuraDataPath, sizeof(szKuraDataPath) );
	strcat_s( szKuraHomePath, sizeof(szKuraHomePath), "kura\\kura" );
	strcat_s( szKuraDataPath, sizeof(szKuraDataPath), "kura\\data" );

	// Now need to create the URI version of the KURA_HOME path that's using in some settings
	strcpy_s( szKuraHomeURI, sizeof(szKuraHomeURI), "/" );
	strcat_s( szKuraHomeURI, sizeof(szKuraHomeURI), szKuraHomePath );
	str_replace( szKuraHomeURI, sizeof(szKuraHomeURI), "\\", "/" );  // need to replace \ with /

	// Get the %TEMP% path
	GetEnvironmentVariable( "TEMP", szKuraTempPath, sizeof(szKuraTempPath) );
	append_backslash( szKuraTempPath, sizeof(szKuraTempPath) );
	strcat_s( szKuraTempPath, sizeof(szKuraTempPath), "kura" );

	// Now we need to create a temporary directory and copy the CONFIG.INI file there
	strcpy_s( szKuraTempPathCfg, sizeof(szKuraTempPathCfg), szKuraTempPath );
	append_backslash( szKuraTempPathCfg, sizeof(szKuraTempPathCfg) );
	strcat_s( szKuraTempPathCfg, sizeof(szKuraTempPathCfg), "Configuration" );

	strcpy_s( szKuraHomePathCfg, sizeof(szKuraHomePathCfg), szKuraHomePath );
	append_backslash( szKuraHomePathCfg, sizeof(szKuraHomePathCfg) );
	strcat_s( szKuraHomePathCfg, sizeof(szKuraHomePathCfg), "config.ini" );

	CreateDirectory( szKuraTempPath, NULL );
	CreateDirectory( szKuraTempPathCfg, NULL );
	strcat_s( szKuraTempPathCfg, sizeof(szKuraTempPathCfg), "\\config.ini" );
	CopyFile( szKuraHomePathCfg, szKuraTempPathCfg, FALSE );

	// Now we need to replace the variables in KuraJavaArgs with actual paths
	strcpy_s( buf, KURA_CMD_LINE_SIZE, KuraJavaArgs );
	str_replace( buf, KURA_CMD_LINE_SIZE, "%KURA_HOME%", szKuraHomePath );
	str_replace( buf, KURA_CMD_LINE_SIZE, "%KURA_DATA%", szKuraDataPath );
	str_replace( buf, KURA_CMD_LINE_SIZE, "%KURA_HOME_URI%", szKuraHomeURI );
	str_replace( buf, KURA_CMD_LINE_SIZE, "%KURA_TEMP%", szKuraTempPath );
	str_replace( buf, KURA_CMD_LINE_SIZE, "%~dp0\\", szInstallPath );

	return buf;
}

int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPTSTR lpCmdLine, int nCmdShow)
{
	MSG msg;
	HKEY hKey;
	DWORD dwType, dwSize;
	BYTE buf[MAX_PATH];
	LPTSTR lpInstallPath = NULL;

	// Register class and initialize - needed to receive messages
	MyRegisterClass(hInstance);
	if( !InitInstance (hInstance, SW_HIDE) )
		return 0;

	// First check the installation dir path in registry (created during setup), which should tell us where Kura is installed
	if( RegOpenKeyEx( HKEY_LOCAL_MACHINE, "SOFTWARE\\Kura", 0, KEY_ALL_ACCESS, &hKey ) == ERROR_SUCCESS ||
		RegOpenKeyEx( HKEY_LOCAL_MACHINE, "SOFTWARE\\Wow6432Node\\Kura", 0, KEY_ALL_ACCESS, &hKey ) == ERROR_SUCCESS )
	{
	    dwSize = sizeof(buf);
	    if( RegQueryValueEx( hKey, "Install_Dir", 0, &dwType, buf, &dwSize) == ERROR_SUCCESS )
		{
			lpInstallPath = (LPTSTR)buf;
		}
		RegCloseKey( hKey );
	}

	// Otherwise get the intall path from command line parameter
	if( !lpInstallPath )
	{
		if( lpCmdLine && lpCmdLine[0] != 0 )
		{
			lpInstallPath = lpCmdLine;
		}
	}

	if( !lpInstallPath )
	{
		MessageBox( NULL, "Kura Launcher v1.0\n\nUsage:\nKuraLauncher <install_path>", "Syntax Error", MB_ICONERROR );
		return 0;
	}

	g_pszKuraCommandLine = PrepareCommandLine( lpInstallPath );
	if( g_pszKuraCommandLine == NULL )
		return 0;

	// OK, now we should have the complete argument list, so let's launch it !
	g_fKuraStarterActive = TRUE;
	g_hKuraStarterThread = CreateThread( NULL, 0, KuraStarter, NULL, 0, NULL );

	// Main message loop:
	while (GetMessage(&msg, NULL, 0, 0))
	{
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	free( g_pszKuraCommandLine );

	return (int) msg.wParam;
}

ATOM MyRegisterClass(HINSTANCE hInstance)
{
	WNDCLASSEX wcex;

	wcex.cbSize = sizeof(WNDCLASSEX);

	wcex.style			= 0;
	wcex.lpfnWndProc	= WndProc;
	wcex.cbClsExtra		= 0;
	wcex.cbWndExtra		= 0;
	wcex.hInstance		= hInstance;
	wcex.hIcon			= NULL;
	wcex.hCursor		= NULL;
	wcex.hbrBackground	= (HBRUSH)(COLOR_WINDOW+1);
	wcex.lpszMenuName	= NULL;
	wcex.lpszClassName	= szWindowClass;
	wcex.hIconSm		= NULL;

	return RegisterClassEx(&wcex);
}

BOOL InitInstance(HINSTANCE hInstance, int nCmdShow)
{
   HWND hWnd;

   hInst = hInstance; // Store instance handle in our global variable

   hWnd = CreateWindow(szWindowClass, szTitle, WS_OVERLAPPEDWINDOW,
      CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL, hInstance, NULL);

   if (!hWnd)
   {
      return FALSE;
   }

   ShowWindow(hWnd, nCmdShow);
   UpdateWindow(hWnd);

   return TRUE;
}

LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	int wmId, wmEvent;
	DWORD dwValue;

	switch (message)
	{
	case WM_COMMAND:
		wmId    = LOWORD(wParam);
		wmEvent = HIWORD(wParam);
		switch (wmId)
		{
		default:
			return DefWindowProc(hWnd, message, wParam, lParam);
		}
		break;

	case WM_DESTROY:
		// If the launcher is being terminated, kill the child Kura process too 
		g_fKuraStarterActive = FALSE;
		// Stop Kura gracefully
		// Sending "exit" will terminate Kura and the console, but it doesn't send a Death Certificate (DC)
		// So we better send "shutdown" which does send a DC too. After this we can't use other commands like 'exit', so need to kill it
		WriteFile(g_hINWrite, "shutdown\n", 9, &dwValue, NULL);
		// "shutdown" takes a few seconds to finish...
		if( WaitForSingleObject( g_hKuraStarterThread, 6000 ) == WAIT_TIMEOUT )
		{
			// If the console is still running, kill it
			if( g_hKuraProcess )
				TerminateProcess( g_hKuraProcess, 0 );
			WaitForSingleObject( g_hKuraStarterThread, 2000 );		// wait for the starter to terminate
		}
		PostQuitMessage(0);
		break;

	// Perform tasks necessary for suspend/resume
	case WM_POWERBROADCAST:
		switch( wParam )
		{
		case PBT_APMSUSPEND:		// System going into sleep/suspend
			break;
		case PBT_APMRESUMESUSPEND:	// System waking up from sleep/suspend
			break;
		}
		break;

	default:
		return DefWindowProc(hWnd, message, wParam, lParam);
	}
	return 0;
}
