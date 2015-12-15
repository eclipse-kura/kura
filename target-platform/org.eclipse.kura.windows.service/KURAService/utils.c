
#include <windows.h>

#include "KURAService.h"

SERVICE_STATUS g_ServiceStatus;
SERVICE_STATUS_HANDLE g_StatusHandle;

HANDLE g_hOUTRead, g_hOUTWrite, g_hINRead, g_hINWrite;

//====================================================================================================================================
void UpdateStatus(DWORD dwStatus, DWORD dwAccepts, DWORD dwCheck, DWORD dwError)
{
	g_ServiceStatus.dwCheckPoint = dwCheck;
	g_ServiceStatus.dwWin32ExitCode = dwError;
	g_ServiceStatus.dwCurrentState = dwStatus;
	g_ServiceStatus.dwControlsAccepted = dwAccepts;

	if(SetServiceStatus(g_StatusHandle, &g_ServiceStatus) == FALSE)
	{
		// TODO Log status update failure
	}
}

//===================================================================================================================================
//	STDOUT_Thread
//
//	This function is used in a thread to monitor the pipe used for STDOUT of our child process (the actual KURA service). For now
//	just consume the characters so that the child doesn't block on printf etc and that it doesn't require a windows for output.
//	NB: when the child process closes, either due to a service stop or an error the hOUTRead handle is closed. This causes ReadFile
//	to return an error which it turn causes this thread to exit.
//
DWORD WINAPI STDOUT_Thread(void *lpParameter)
{
	DWORD dwValue;
	char cBuffer[64];

	while(ReadFile(g_hOUTRead, cBuffer, 64, &dwValue, NULL))
	{
		// Should probably parse the output here to make sure the service has started OK
	}
	return 0;
}

//===================================================================================================================================
//	CreatePipes
//
//	Two pipes are used for the STDIN & STDOUT of the service process, this allows the KURA process to be controller by the service
//	manager. This is important as it allows the OSGI process to be killed in response to a service stop request. The STDIN pipe is
//	created so that the read handle is inherited but the right end is not, the STDOUT pipe is created so that the write handle is
//	inherited but the read handle is not. The KURA process is then started with the inhertied handles as STDIN & STDOUT.
//
BOOL CreatePipes(void)
{
	HANDLE hDuplicate;
	SECURITY_DESCRIPTOR sd; 
	SECURITY_ATTRIBUTES saAttr;

	InitializeSecurityDescriptor(&sd,SECURITY_DESCRIPTOR_REVISION);

	// Set the bInheritHandle flag so pipe handles are inherited. 
	saAttr.bInheritHandle = TRUE;
	saAttr.lpSecurityDescriptor = &sd; 
	saAttr.nLength = sizeof(SECURITY_ATTRIBUTES); 

	//Assigns a NULL DACL to the security descriptor
	SetSecurityDescriptorDacl(&sd, TRUE, NULL, FALSE);

	// Create the pipe for STDOUT write end will be inherited
	if(CreatePipe(&hDuplicate, &g_hOUTWrite, &saAttr, 0))
	{
		// Duplicate the read end so that is doesn't get inherited then close the original handle
		if(DuplicateHandle(GetCurrentProcess(), hDuplicate, GetCurrentProcess(), &g_hOUTRead, 0, FALSE, DUPLICATE_SAME_ACCESS))
		{
			CloseHandle(hDuplicate);
			
			// Create the pipe for STDIN read end will be inherited
			if(CreatePipe(&g_hINRead, &hDuplicate, &saAttr, 0))
			{
				// Duplicate the write end so that is doesn't get inherited then close the original handle
				if(DuplicateHandle(GetCurrentProcess(), hDuplicate, GetCurrentProcess(), &g_hINWrite, 0, FALSE, DUPLICATE_SAME_ACCESS))
				{
					CloseHandle(hDuplicate);
					return TRUE;
				}
				CloseHandle(g_hINWrite);
			}
		}
		CloseHandle(hDuplicate); CloseHandle(g_hOUTWrite);
	}
	return FALSE;
}
