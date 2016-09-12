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

#include "KURAService.h"

BOOL g_bTerminate;							// Set to true to indicate the service should terminate

#define KURA_CMD_LINE_SIZE			32768
char *g_pszKuraCommandLine = NULL;

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

// Caller is responsible for providing a buffer large enough for the replaced string to fit it
int str_replace( char *buf, size_t buf_size, char *strSearch, char *strNew )
{
  char *p1, *buf2, *pfound;
  int sz, cnt = 0;
  int room_left = (int)buf_size-1;  // reserve 1 char for terminating 0

  if(buf==NULL || strSearch==NULL || strNew==NULL || buf_size==0)
    return 0;

  // Create a copy of original buffer to store modified string
  buf2 = (char*)malloc(buf_size);
  memset(buf2, 0, buf_size);

  p1 = buf;
  while( (pfound = strstr(p1, strSearch)) )
  {
    // Append the string before the found substring
    sz = min(room_left, (int)(pfound-p1));
    strncat(buf2, p1, sz);
    if((room_left-=sz) <= 0)
      break;

    // Append the replaced substring
    sz = min(room_left, (int)strlen(strNew));
    strncat(buf2, strNew, sz);
    if((room_left-= sz) <= 0)
      break;

    p1 = pfound + strlen(strSearch);
    cnt++;
  }

  // Now append the rest of the string
  sz = min(room_left, (int)strlen(p1));
  strncat(buf2, p1, sz);

  // now copy back the new string into the original one based on max buffer size
  memset(buf, 0, buf_size);
  memcpy(buf, buf2, min(buf_size, strlen(buf2)));
  free(buf2);
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


void WINAPI ServiceMain(DWORD argc, LPTSTR *argv)
{
	HKEY hKey;
	DWORD dwType, dwSize;
	BYTE buf[MAX_PATH];
	LPTSTR lpInstallPath = NULL;

	ZeroMemory (&g_ServiceStatus, sizeof (g_ServiceStatus));
	g_ServiceStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	g_bTerminate = 0;

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

	g_pszKuraCommandLine = PrepareCommandLine( lpInstallPath );
	if(g_pszKuraCommandLine == NULL)
	{
		UpdateStatus(SERVICE_STOPPED, 0, 3, ERROR_PATH_NOT_FOUND);
		return;
	}

	// Register our service control handler with the SCM
	g_StatusHandle = RegisterServiceCtrlHandler(SERVICE_NAME, ServiceCtrlHandler);
 	if(g_StatusHandle == NULL)
	{
		free(g_pszKuraCommandLine);
		UpdateStatus(SERVICE_STOPPED, 0, 3,  GetLastError());
		return;
	}

	// Tell the service controller we are starting
	UpdateStatus(SERVICE_START_PENDING, 0, 0, NO_ERROR);

	RunService(g_pszKuraCommandLine);

	// Tell the service controller we are stopped
	UpdateStatus(SERVICE_STOPPED, 0, 3, NO_ERROR);

	free(g_pszKuraCommandLine);
}

int main(int argc, char* argv[])
{
	SERVICE_TABLE_ENTRY ServiceTable[] = 
	{
		{ SERVICE_NAME, (LPSERVICE_MAIN_FUNCTION)ServiceMain },
		{ NULL, NULL }
	};

	if(StartServiceCtrlDispatcher(ServiceTable) == FALSE)
	{
		return GetLastError();
	}
	return 0;
}
