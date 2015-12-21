
#include "KURAService.h"

BOOL g_bTerminate;							// Set to true to indicate the service should terminate

void WINAPI ServiceMain(DWORD argc, LPTSTR *argv)
{
	HKEY hKey;
	DWORD dwSize;
	g_bTerminate = 0;
	TCHAR szString[256];
	TCHAR *pSubKey = _T("System\\CurrentControlSet\\Services\\");


	// Register our service control handler with the SCM
	g_StatusHandle = RegisterServiceCtrlHandler(SERVICE_NAME, ServiceCtrlHandler);
 
	g_bTerminate = 0;
	if(g_StatusHandle != NULL) 
	{
		ZeroMemory (&g_ServiceStatus, sizeof (g_ServiceStatus));
		g_ServiceStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;

		// Tell the service controller we are starting
		UpdateStatus(SERVICE_START_PENDING, 0, 0, NO_ERROR);

		// Check that the sevice name will fit onto the end of the services key name
		if(((_tcslen(pSubKey) + _tcslen(argv[0]) + 1) * sizeof(TCHAR)) < 256)
		{
			_tcscpy(szString, pSubKey);
			_tcscat(szString, argv[0]);

			if(RegOpenKey(HKEY_LOCAL_MACHINE, szString, &hKey) == ERROR_SUCCESS)
			{
				dwSize = 256;
				if(RegQueryValueEx(hKey, _T("ServiceCommand"), 0, NULL, (BYTE *)szString, &dwSize) == ERROR_SUCCESS)
				{
					RunService(szString);
				}
				RegCloseKey(hKey);
			}
		}
		// Tell the service controller we are stopped
		UpdateStatus(SERVICE_STOPPED, 0, 3, NO_ERROR);
	}
	return;
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
