!include "Library.nsh"
!include "nsDialogs.nsh"
!include "winmessages.nsh"
!include "logiclib.nsh"
!include "StrFunc.nsh"

${StrStr} # Supportable for Install Sections and Functions
${StrRep} # Supportable for Install Sections and Functions

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

ShowInstDetails show
ShowUninstDetails show

;--------------------------------

; Pages

Page license
Page directory
Page custom inst_as_init inst_as_leave
Page instfiles

UninstPage uninstConfirm
UninstPage custom un.custom_init un.custom_leave
UninstPage instfiles

;--------------------------------

!macro _ReplaceInFile SOURCE_FILE SEARCH_TEXT REPLACEMENT
  Push "${SOURCE_FILE}"
  Push "${SEARCH_TEXT}"
  Push "${REPLACEMENT}"
  Call RIF
!macroend

Function RIF
 
  ClearErrors  ; want to be a newborn
 
  Exch $0      ; REPLACEMENT
  Exch
  Exch $1      ; SEARCH_TEXT
  Exch 2
  Exch $2      ; SOURCE_FILE
 
  Push $R0     ; SOURCE_FILE file handle
  Push $R1     ; temporary file handle
  Push $R2     ; unique temporary file name
  Push $R3     ; a line to sar/save
  Push $R4     ; shift puffer
 
  IfFileExists $2 +1 RIF_error      ; knock-knock
  FileOpen $R0 $2 "r"               ; open the door
 
  GetTempFileName $R2               ; who's new?
  FileOpen $R1 $R2 "w"              ; the escape, please!
 
  RIF_loop:                         ; round'n'round we go
    FileRead $R0 $R3                ; read one line
    IfErrors RIF_leaveloop          ; enough is enough
    RIF_sar:                        ; sar - search and replace
      Push "$R3"                    ; (hair)stack
      Push "$1"                     ; needle
      Push "$0"                     ; blood
      Call StrRep                   ; do the bartwalk
      StrCpy $R4 "$R3"              ; remember previous state
      Pop $R3                       ; gimme s.th. back in return!
      StrCmp "$R3" "$R4" +1 RIF_sar ; loop, might change again!
    FileWrite $R1 "$R3"             ; save the newbie
  Goto RIF_loop                     ; gimme more
 
  RIF_leaveloop:                    ; over'n'out, Sir!
    FileClose $R1                   ; S'rry, Ma'am - clos'n now
    FileClose $R0                   ; me 2
 
    Delete "$2.old"                 ; go away, Sire
    Rename "$2" "$2.old"            ; step aside, Ma'am
    Rename "$R2" "$2"               ; hi, baby!

    Delete "$2.old"                 ; we don't need you anymore, Sire
 
    ClearErrors                     ; now i AM a newborn
    Goto RIF_out                    ; out'n'away
 
  RIF_error:                        ; ups - s.th. went wrong...
    SetErrors                       ; ...so cry, boy!
 
  RIF_out:                          ; your wardrobe?
  Pop $R4
  Pop $R3
  Pop $R2
  Pop $R1
  Pop $R0
  Pop $2
  Pop $0
  Pop $1
 
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

var dialog
var hwnd
var inst_as_service
 
Function inst_as_init
	nsDialogs::Create 1018
		Pop $dialog

	Push false
	Pop $inst_as_service
 
	${NSD_CreateLabel} 0 0 100% 10u "Select how to install Kura:"

	${NSD_CreateRadioButton} 10 20 90% 10u "Run Kura automatically at logon (allows user interaction)"
		Pop $hwnd
		${NSD_AddStyle} $hwnd ${WS_GROUP}
		${NSD_OnClick} $hwnd RadioClickInstAsLogon
		SendMessage $hwnd ${BM_SETCHECK} 1 0
	${NSD_CreateRadioButton} 10 40 90% 10u "Install Kura as a service (no user interaction)"
		Pop $hwnd
		${NSD_OnClick} $hwnd RadioClickInstAsSvc
 
	nsDialogs::Show
FunctionEnd
 
Function RadioClickInstAsLogon
	Pop $hwnd
	Push false
	Pop $inst_as_service
FunctionEnd

Function RadioClickInstAsSvc
	Pop $hwnd
	Push true
	Pop $inst_as_service
FunctionEnd
 
Function inst_as_leave
FunctionEnd

;--------------------------------

Var CompleteUninstCheckbox
Var CompleteUninstCheckboxState

Function un.custom_init
	nsDialogs::Create 1018

	${NSD_CreateCheckbox} 0 10 100% 10u "Perform a complete uninstall (including custom settings and snapshots)"
	Pop $CompleteUninstCheckbox
	${NSD_SetState} $CompleteUninstCheckbox ${BST_UNCHECKED}

	nsDialogs::Show
FunctionEnd

Function un.custom_leave
	${NSD_GetState} $CompleteUninstCheckbox $CompleteUninstCheckboxState
FunctionEnd

;--------------------------------

; The stuff to install

var JREx64
var inst_dir_
var data_dir
var data_dir_
var temp_dir_

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


	;=================================================================================================
	; If reinstalling first make sure to stop the service and/or stop+delete the Kura task, so we can overwrite it

	DetailPrint "Terminating Kura..."
	ExecWait '$WINDIR\Sysnative\sc stop KURAService'		; Issue stop
	Sleep 2000												; Small delay to wait for it to stop
	ExecWait '$WINDIR\Sysnative\sc delete KURAService'		; Delete the service from the system

	; In case of auto-run install, stop and delete the task
	ExecWait 'schtasks /End /TN "Kura"'
	ExecWait 'schtasks /Delete /F /TN "Kura"'


	DetailPrint "Copying files..."
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
	!insertmacro InstallLib DLL NOTSHARED NOREBOOT_NOTPROTECTED system\x64\dkcomm.dll $SYSDIR\dkcomm.dll $SYSDIR
	!insertmacro InstallLib DLL NOTSHARED NOREBOOT_NOTPROTECTED system\x64\KuraNativeWin.dll $SYSDIR\KuraNativeWin.dll $SYSDIR

	;==========================================================================================================================
	; Now replace fixed paths in startup and config files
	; Config files will need to change path to Unix-style / and remove "
	DetailPrint "Updating settings files..."

	StrCpy     $inst_dir_ $INSTDIR
	ReadEnvStr $data_dir "ALLUSERSPROFILE"
	ReadEnvStr $temp_dir_ "TEMP"

	${StrRep} $inst_dir_ $inst_dir_ `"` ``
	${StrRep} $inst_dir_ $inst_dir_ `\` `/`

	${StrRep} $data_dir_ $data_dir `"` ``
	${StrRep} $data_dir_ $data_dir_ `\` `/`

	${StrRep} $temp_dir_ $temp_dir_ `"` ``
	${StrRep} $temp_dir_ $temp_dir_ `\` `/`
;  DetailPrint "Replacing..."
;  DetailPrint $inst_dir_
;  DetailPrint $data_dir_
;  DetailPrint $temp_dir_

	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\kura.properties"  "/opt/eclipse/kura/kura/plugins" "$inst_dir_/kura/plugins"
	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\kura.properties"  "/opt/eclipse/kura"              "$data_dir_/kura"
	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\kura.properties"  "/tmp/.kura"                     "$temp_dir_/kura"

	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\log4j.properties" "/var"                           "$temp_dir_/kura"

	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\config.ini"       "/tmp/kura"                      "$temp_dir_/kura"
	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\config.ini"       "/opt/eclipse/kura/plugins"      "../plugins"
	!insertmacro _ReplaceInFile "$data_dir\Kura\kura\config.ini"       "/opt/eclipse/kura/kura/plugins" "../kura/plugins"

	!insertmacro _ReplaceInFile "$INSTDIR\start_kura_debug.bat"        "c:\opt\eclipse"                 `%ALLUSERSPROFILE%`
	!insertmacro _ReplaceInFile "$INSTDIR\start_kura_debug.bat"        "\tmp\.kura"                     `%TEMP%\kura`

	!insertmacro _ReplaceInFile "$INSTDIR\SCH_Kura.xml"                "C:\Program Files\Kura\"         "$INSTDIR\"


	${If} $inst_as_service == 1

		;=============================================================================================================
		; Install as a service

		DetailPrint "Installing Kura service..."

		; Copy the Service helper
		SetOutPath $WINDIR\Sysnative
		File system\x64\KURAService.exe

		;=============================================================================================================
		; Now setup the service that will run Kura using the service manager sc. NB: the installer will be running as
		; a 32 bit process but we want a 64 bit service so we must call sc with it's full path using Sysnative. The
		; Sysnative directory is only available to 32 bit processes and is the real System32 directory not SysWOW64

		ExecWait '$WINDIR\Sysnative\sc create KURAService binpath= $SYSDIR\KURAService.exe'
		ExecWait '$WINDIR\Sysnative\sc config KURAService start= auto displayname= "KURA Service"'
		ExecWait '$WINDIR\Sysnative\sc description KURAService "KURA MQTT communitaction service for IOT devices."'

		; Add a registry entry with the command that actually starts KURA then start the service
		;WriteRegStr HKLM System\CurrentControlSet\Services\KURAService "ServiceCommand" 'cmd /C "$INSTDIR\start_kura.bat"'
		ExecWait '$WINDIR\Sysnative\sc start KURAService'
	${Else}

		;=============================================================================================================
		; Install as an application automatically started after logon
		; This is done using Windows Task Schduler - supplying an XML configuration file for the new task to create

		DetailPrint "Installing Kura autorun task..."

		; First adjust the default path in the xml file with the chosen install directory
		ExecWait 'schtasks /Create /TN "Kura" /XML "$INSTDIR\SCH_Kura.xml"'

		; And start the task manually
		Exec 'schtasks /Run /TN "Kura"'

	${Endif}

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
	; First we stop and then delete the KURA service. Take care here we must run the 64 bit version
	; of sc so must use the full path with sysnative not system32

	DetailPrint "Terminating Kura..."

	ExecWait '$WINDIR\Sysnative\sc stop KURAService'		; Issue stop
	Sleep 2000												; Small delay to wait for it to stop
	ExecWait '$WINDIR\Sysnative\sc delete KURAService'		; Delete the service from the system

	; In case of auto-run install, stop and delete the task
	ExecWait 'schtasks /End /TN "Kura"'
	ExecWait 'schtasks /Delete /F /TN "Kura"'
  
	; Remove registry keys
	DetailPrint "Removing Kura settings..."
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kura"
	DeleteRegKey HKLM SOFTWARE\Kura

	; Remove files and uninstaller
	DetailPrint "Removing Kura files..."
	Delete $INSTDIR\kura\plugins\*.*
	RMDir "$INSTDIR\kura\plugins"
	RMDir "$INSTDIR\kura"
	Delete $INSTDIR\plugins\*.*
	RMDir "$INSTDIR\plugins"
	Delete $INSTDIR\*.*
	Delete $INSTDIR\uninstall.exe
	RMDir "$INSTDIR"

	; Remove shortcuts, if any
	Delete "$SMPROGRAMS\Kura\*.*"
	RMDir "$SMPROGRAMS\Kura"

	${If} $CompleteUninstCheckboxState == ${BST_CHECKED}

		; Full uninstall including all settings and temporary data
		DetailPrint "Removing Kura user settings..."
		RMDir /r "$TEMP\kura"
		ReadEnvStr $0 "ALLUSERSPROFILE"
		RMDir /r "$0\kura"

	${EndIf}


SectionEnd
