
#include "KURAService.h"

//===================================================================================================================================
//	RunService
//
//	This routine actually starts the service executable. The flag g_bTerminate is set when we recieve a terminate request, so if the
//	executable terminates while this flag is clear it just died and we try to restart it. If the executable fails to start wait for
//	20 seconds (arbitratry vale) and try again.
//
void RunService(TCHAR *pszCommand)
{
	STARTUPINFO mStartInfo;
	PROCESS_INFORMATION mProcInfo;

	memset(&mStartInfo, 0, sizeof(mStartInfo));

	mStartInfo.cb = sizeof(STARTUPINFO); 
	mStartInfo.dwFlags = STARTF_USESTDHANDLES;

	// Loop until signalled by SERVICE_CONTROL_STOP
	while(g_bTerminate == 0)					
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

				// Start the actual KURA process
				if(CreateProcess(NULL, pszCommand, NULL, NULL, TRUE, 0, NULL, NULL, &mStartInfo, &mProcInfo))
				{
					UpdateStatus(SERVICE_RUNNING, SERVICE_ACCEPT_STOP, 0, NO_ERROR);

					// Wait for the KURA Service process to terminate then clean up the handles associated with the process
					WaitForSingleObject(mProcInfo.hProcess, INFINITE);
					CloseHandle(mProcInfo.hThread); CloseHandle(mProcInfo.hProcess);

					// LOG: Service process terminated unexpectedly
				}
				else
				{
					// LOG: Unable to start process
				}
			}
			else
			{
				// LOG: Unable to create STDIO thread 
			}

			// Close the handles associated with the STDIO pipes a new set gets created if required
			CloseHandle(g_hINWrite); CloseHandle(g_hINRead);
			CloseHandle(g_hOUTWrite); CloseHandle(g_hOUTRead);
		}
		else
		{
			// LOG: Unable to create pipes 
		}
		Sleep(1000);
	}
}

//====================================================================================================================================
VOID WINAPI ServiceCtrlHandler (DWORD CtrlCode)
{
	DWORD dwValue;
	switch (CtrlCode) 
	{
		case SERVICE_CONTROL_STOP :		if(g_ServiceStatus.dwCurrentState == SERVICE_RUNNING)
										{
											UpdateStatus(SERVICE_STOP_PENDING, 0, 4, NO_ERROR);
 
											g_bTerminate = 1;
											WriteFile(g_hINWrite, "exit\ny\n", 7, &dwValue, NULL);
										}
										break;
 
		default :						break;
	}
}  
