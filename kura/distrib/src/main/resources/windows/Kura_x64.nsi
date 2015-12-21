!include "Library.nsh"

; The name of the installer
Name "Kura for Windows"

; The output file, actual name set by ANT build file
OutFile {build.output.name}_x64.exe

;!ifdef MAKE_64BIT
  !define BITS 64
  !define NAMESUFFIX " (64 bit)"
;!else
;  !define BITS 32
;  !define NAMESUFFIX ""
;!endif

; The default installation directory
InstallDir $PROGRAMFILES${BITS}\Kura

; Registry key to check for directory (so if you install again, it will overwrite the old one automatically)
InstallDirRegKey HKLM "Software\Kura" "Install_Dir"

; Request application privileges for Windows Vista
RequestExecutionLevel admin

LicenseData "KuraFiles\license.rtf"

;--------------------------------

; Pages

Page license
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------

!define StrStr "!insertmacro StrStr"
 
!macro StrStr ResultVar String SubString
  Push `${String}`
  Push `${SubString}`
  Call StrStr
  Pop `${ResultVar}`
!macroend
 
Function StrStr
/*After this point:
  ------------------------------------------
  $R0 = SubString (input)
  $R1 = String (input)
  $R2 = SubStringLen (temp)
  $R3 = StrLen (temp)
  $R4 = StartCharPos (temp)
  $R5 = TempStr (temp)*/
 
  ;Get input from user
  Exch $R0
  Exch
  Exch $R1
  Push $R2
  Push $R3
  Push $R4
  Push $R5
 
  ;Get "String" and "SubString" length
  StrLen $R2 $R0
  StrLen $R3 $R1
  ;Start "StartCharPos" counter
  StrCpy $R4 0
 
  ;Loop until "SubString" is found or "String" reaches its end
  loop:
    ;Remove everything before and after the searched part ("TempStr")
    StrCpy $R5 $R1 $R2 $R4
 
    ;Compare "TempStr" with "SubString"
    StrCmp $R5 $R0 done
    ;If not "SubString", this could be "String"'s end
    IntCmp $R4 $R3 done 0 done
    ;If not, continue the loop
    IntOp $R4 $R4 + 1
    Goto loop
  done:
 
/*After this point:
  ------------------------------------------
  $R0 = ResultVar (output)*/
 
  ;Remove part before "SubString" on "String" (if there has one)
  StrCpy $R0 $R1 `` $R4
 
  ;Return output to user
  Pop $R5
  Pop $R4
  Pop $R3
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd

;--------------------------------

; Oracle binary files can't be download now, they require accepting of license, so we have to point to the page
;!define JRE_URL "http://download.oracle.com/otn-pub/java/jdk/8u65-b17/jre-8u65-windows-x64.exe"
!define JRE_URL "http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html"

Function GetJRE
/*
  MessageBox MB_OK "${PRODUCT_NAME} requires Java, it will now be downloaded and installed"
  StrCpy $2 "$TEMP\Java Runtime Environment.exe"
  nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
  Pop $R0 ;Get the return value
  StrCmp $R0 "success" +3
    MessageBox MB_OK "Download failed: $R0"
    Quit
  ExecWait $2
  Delete $2
*/
	ExecShell "open" ${JRE_URL}
FunctionEnd

;--------------------------------

; The stuff to install

var JREx64

Section "kura (required)"

	SectionIn RO

  DetailPrint "Checking Java JRE presence..."
  ClearErrors
  Exec 'java -version'
  ${If} ${Errors}
    MessageBox MB_ICONEXCLAMATION|MB_YESNO "Error: Java Runtime Engine (JRE) is not installed. Please install a JRE and make sure it's in the path before starting the setup. Recommended JRE is Oracle Java SE JRE 8u60 for Windows x64. Please note, that installing the Oracle JRE might be subject to licensing fees - please consult the License Terms. Click Yes to open the recommended download page." IDNO +2
    Call GetJRE
    Abort
  ${EndIf}

  ; Execut Java version test to determine if x86 version installed
  DetailPrint "Checking Java JRE version..."
  FileOpen $0 '$TEMP\java_test.bat' "w"
  FileWrite $0 'java -version >"$TEMP\java_ver.txt" 2>&1'
  FileClose $0

  ExecWait '$TEMP\java_test.bat'
  Delete $TEMP\java_test.bat

  StrCpy $JREx64 0
  FileOpen $0 '$TEMP\java_ver.txt' "r"
  IfErrors ReadDone
  ReadAgain:
  FileRead $0 $1
  IfErrors ReadDone
  DetailPrint $1
  ${StrStr} $3 $1 "64-Bit"
  ${If} $3 != ""
    DetailPrint "Is x64"
    StrCpy $JREx64 1
    Goto ReadDone
  ${EndIf}
  Goto ReadAgain
  FileClose $0
  ReadDone:

  Delete '$TEMP\java_ver.txt'

  ${If} JREx64 == ""
    MessageBox MB_ICONEXCLAMATION|MB_YESNO "Error: This is a 64-bit system, but installed Java Runtime is not a 64-bit version. Please install a 64-bit Java JRE and make sure it's in path before starting the setup. Click Yes to open the recommended download page." IDNO +2
    Call GetJRE
    Abort
  ${EndIf}


	; Executable and other files that are not modified during runtime go to dest dir
	SetOutPath $INSTDIR
	File    KuraFiles\*
	SetOutPath $INSTDIR\kura\plugins
	File /r KuraFiles\kura\plugins\*
	SetOutPath $INSTDIR\plugins
	File /r KuraFiles\plugins\*

	; Config and other stuff (snapshots, etc.) that needs write-access goes to \ProgramData\kura
	ReadEnvStr $0 "ALLUSERSPROFILE"
	SetOutPath $0\Kura\data
	File /r KuraFiles\data\*
	SetOutPath $0\Kura\kura
	File KuraFiles\kura\*

	;==========================================================================================================================
	; Copy the supporting system .dll and .exe files. These need to go in $WINDIR\System32 but take care here. We are running
	; 32 bit so file system redirection is on. For the dlls we must define LIBRARY_X64 to disable it, for .exe use Sysnative
	; rather than System32

	!define LIBRARY_X64
	!insertmacro InstallLib DLL NOTSHARED NOREBOOT_NOTPROTECTED system\x64\dkcomm64.dll $SYSDIR\dkcomm64.dll $SYSDIR
	!insertmacro InstallLib DLL NOTSHARED NOREBOOT_NOTPROTECTED system\x64\KuraNativeWin64.dll $SYSDIR\KuraNativeWin64.dll $SYSDIR

	SetOutPath $WINDIR\Sysnative
	File system\x64\KURAService.exe

	; batch file to replace Kura paths in config files
	SetOutPath $INSTDIR
	File "KuraFiles\set_kura_paths.bat"
	ExecWait '"$INSTDIR\set_kura_paths.bat" "$INSTDIR"'

	;=============================================================================================================
	; Now setup the service that will run Kura using the service manager sc. NB: the installer will be running as
	; a 32 bit process but we want a 64 bit service so we must call sc with it's full path using Sysnative. The
	; Sysnative directory is only available to 32 bit processes and is the real System32 directory not SysWOW64

	ExecWait '$WINDIR\Sysnative\sc create KURAService binpath= $SYSDIR\KURAService.exe'
	ExecWait '$WINDIR\Sysnative\sc config KURAService start= auto displayname= "KURA Service"'
	ExecWait '$WINDIR\Sysnative\sc description KURAService "KURA MQTT communitaction service for IOT devices."'

	; Add a registry entry with the command that actually starts KURA then start the service
	WriteRegStr HKLM System\CurrentControlSet\Services\KURAService "ServiceCommand" 'cmd /C "$INSTDIR\start_kura.bat"'
	ExecWait '$WINDIR\Sysnative\sc start KURAService'

	; Write the installation path into the registry
	WriteRegStr HKLM SOFTWARE\Kura "Install_Dir" "$INSTDIR"

	; Write the uninstall keys for Windows
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura" "DisplayName" "Kura for Windows"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura" "UninstallString" '"$INSTDIR\uninstall.exe"'
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura" "NoModify" 1
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura" "NoRepair" 1
	WriteUninstaller "uninstall.exe"
  
SectionEnd

;=====================================================================================================
; Uninstaller

Section "Uninstall"

	;=================================================================================================
	; First we stop and thene delete the KURA service. Take care here we must run the 64 bit version
	; of sc so must use the full path with sysnative not system32

	ExecWait '$WINDIR\Sysnative\sc stop KURAService'		; Issue stop
	Sleep 2000												; Small delay to wait for it to stop
	ExecWait '$WINDIR\Sysnative\sc delete KURAService'		; Delete the service from the system
  
	; Remove registry keys
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura"
	DeleteRegKey HKLM SOFTWARE\Kura

	; Remove files and uninstaller
	Delete $INSTDIR\kura\plugins\*.*
	RMDir "$INSTDIR\kura\plugins"
	RMDir "$INSTDIR\kura"
	Delete $INSTDIR\plugins\*.*
	RMDir "$INSTDIR\plugins"
	Delete $INSTDIR\*.*
	Delete $INSTDIR\uninstall.exe
	RMDir "$INSTDIR"
	;Delete $%ALLUSERSPROFILE%\Kura\*.*

	; Remove shortcuts, if any
	Delete "$SMPROGRAMS\Kura\*.*"
	RMDir "$SMPROGRAMS\Kura"

SectionEnd
