#include <windows.h>
#include <tchar.h>

#include "KURAService.h"

BOOL g_bTerminate;							// Set to true to indicate the service should terminate

extern void WINAPI ServiceCtrlHandler(DWORD CtrlCode);

void WINAPI ServiceMain(DWORD argc, LPTSTR *argv)
{
	g_bTerminate = 0;

	// Register our service control handler with the SCM
	g_StatusHandle = RegisterServiceCtrlHandler(SERVICE_NAME, ServiceCtrlHandler);
 
	if(g_StatusHandle != NULL) 
	{
		ZeroMemory (&g_ServiceStatus, sizeof (g_ServiceStatus));
		g_ServiceStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;

		// Tell the service controller we are starting
		UpdateStatus(SERVICE_START_PENDING, 0, 0, NO_ERROR);

		RunService();

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
