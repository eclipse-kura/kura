#!/bin/bash
#
# Copyright (c) 2011, 2016 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Eurotech
#

set -e
#Install failure trap
trap 'run_do_restore; exit 1' SIGINT SIGTERM ERR

INSTALL_DIR=/opt/eclipse
TIMESTAMP=`date +%Y%m%d%H%M%S`
LOG=/tmp/kura_install_${TIMESTAMP}.log
WD_TMP_FILE=/tmp/watchdog
REFRESH_TIME=5
TIMEOUT_TIME=300

##############################################
# UTILITY FUNCTIONS
##############################################

function abspath {
  OLDPWD="$PWD"
  cd "$(dirname "$1")"
  echo "$PWD/$(basename "$1")"
  cd "$OLDPWD"
}

function require {
  if ! [[ $(which "$1") ]];
  then
    echo "$1 not found, please install it"
    exit 1
  fi
}

# Run install function and start watchdog if needed
function run_kura_install {
    if [ -f "${WD_TMP_FILE}" ]; then
        WATCHDOG_DEVICE=`cat ${WD_TMP_FILE}`
        echo "Got watchdog ${WATCHDOG_DEVICE}" >> $LOG 2>&1
        kura_install &
        PID=$!
        START=$(date +%s)
        
        while [ -d "/proc/$PID" ]; do
            echo w > ${WATCHDOG_DEVICE}
            DELTA=$(($(date +%s) - $START))
            if [ "$DELTA" -ge "$TIMEOUT_TIME" ]; then
                echo "The installation process is not responding. It'll be stopped."
                kill -9 $PID >> /dev/null 2>&1
            fi
            sleep $REFRESH_TIME
        done
        stop_watchdog
    else
        kura_install &
        PID=$!
        START=$(date +%s)
        
        while [ -d "/proc/$PID" ]; do
            DELTA=$(($(date +%s) - $START))
            if [ "$DELTA" -ge "$TIMEOUT_TIME" ]; then
                echo "The installation process is not responding. It'll be stopped."
                kill -9 $PID >> /dev/null 2>&1
            fi
            sleep $REFRESH_TIME
        done
    fi
}

# Run do_restore function and start watchdog if needed
function run_do_restore {
    if [ -f "${WD_TMP_FILE}" ]; then
        WATCHDOG_DEVICE=`cat ${WD_TMP_FILE}`
        echo "Got watchdog ${WATCHDOG_DEVICE}" >> $LOG 2>&1
        do_restore &
        PID=$!
        START=$(date +%s)
        
        while [ -d "/proc/$PID" ]; do
            echo w > ${WATCHDOG_DEVICE}
            DELTA=$(($(date +%s) - $START))
            if [ "$DELTA" -ge "$TIMEOUT_TIME" ]; then
                echo "The restore process is not responding. It'll be stopped."
                kill -9 $PID >> /dev/null 2>&1
            fi
            sleep $REFRESH_TIME
        done
        stop_watchdog
    else
        do_restore &
        PID=$!
        START=$(date +%s)
        
        while [ -d "/proc/$PID" ]; do
            DELTA=$(($(date +%s) - $START))
            if [ "$DELTA" -ge "$TIMEOUT_TIME" ]; then
                echo "The restore process is not responding. It'll be stopped."
                kill -9 $PID >> /dev/null 2>&1
            fi
            sleep $REFRESH_TIME
        done
    fi
}

# Deactivate watchdog device if possible
function stop_watchdog {
    if [ -n "${WATCHDOG_DEVICE}" ]; then
        echo V > ${WATCHDOG_DEVICE}
    fi
}

##############################################
# END UTILITY FUNCTIONS
##############################################

##############################################
# BACKUP FUNCTIONS
##############################################

#backup root can be overridden setting the BACKUP_ROOT environment variable
BACKUP_ROOT=${BACKUP_ROOT:-"/tmp/kura_backup_root_${TIMESTAMP}"}
BACKUP_FILES=()
BACKED_UP_FILES=()

function is_already_backed_up {
  for BACKED_UP_FILE in ${BACKED_UP_FILES[@]};
  do
    [[ "$BACKED_UP_FILE" == "$1" ]] && echo "true" && return 0
  done
  echo "false"
  return 0
}

function do_backup {
  printf "Creating backup...\n"

  install -d "${BACKUP_ROOT}_"

  for GLOB in ${BACKUP_FILES[@]};
  do
  	for TARGET in ${GLOB}
  	do
    	if ! [[ $TARGET ]] || ! [ -e $TARGET ] || [[ $(is_already_backed_up "$TARGET") == "true" ]];
    	then
      		continue
    	fi
    	SRC=$(abspath "$TARGET")
    	SRC_DIR=$(dirname "$SRC")
    	printf "\r\033[Kbackup: $TARGET -> ${BACKUP_ROOT}_${SRC}"
    	echo "backup: $TARGET -> ${BACKUP_ROOT}_${SRC}" >> $LOG 2>&1
    	install -d "${BACKUP_ROOT}_/$SRC_DIR"
    	cp -r "$SRC" "${BACKUP_ROOT}_/$SRC_DIR/"
    	BACKED_UP_FILES+=("$TARGET")
    done
  done

   mv "${BACKUP_ROOT}_" "$BACKUP_ROOT"

   printf "\r\033[K"
}

function do_restore {
  #remove signal handler
  trap - SIGINT SIGTERM ERR

  if ! [[ -e "$BACKUP_ROOT" ]];
  then
    echo "backup not present"
    exit
  fi

  echo "Restoring backup..."
  cp -r "$BACKUP_ROOT"/* /

  sync
  
  do_delete_backup
}

function do_delete_backup {
  echo "Removing backup..."
  rm -rf "$BACKUP_ROOT"
}

##############################################
# END BACKUP FUNCTIONS
##############################################

##############################################
# JAVA 8 CHECK FUNCTION
##############################################

function require_java_8 {
  require java
  if [[ $SKIP_JAVA_VERSION_CHECK == "true" ]];
  then
    return 0;
  fi
  JAVA_VERSION=$(java -version 2>&1 | head -n 1 | sed 's/[^ ]* [^ ]* ["][^.]*[.]\([[:digit:]]*\).*/\1/g')
  if ! [[ $JAVA_VERSION =~ ^[0-9]+$ ]];
  then
  	echo "Failed to determine Java version"
    echo "If you are sure that Java 8 or greater is installed please re run this script setting the following environment variable:"
    echo "SKIP_JAVA_VERSION_CHECK=\"true\""
  	exit 1
  fi
  if [ $JAVA_VERSION -lt 8 ];
  then
    echo "Java version 8 or greater is required for running Kura, please upgrade"
    exit 1
  fi
}

##############################################
# END JAVA 8 CHECK FUNCTION
##############################################

function kura_install {
	##############################################
	# PRE-INSTALL SCRIPT
	##############################################
	
	rm -rf kura-*.zip >> $LOG 2>&1
	rm -rf kura_*.zip >> $LOG 2>&1
	
	#add existing files to backup
	BACKUP_FILES+=("/opt/eclipse/data")
	BACKUP_FILES+=("/opt/eclipse/kura*")
	BACKUP_FILES+=("${INSTALL_DIR}/kura*")
	BACKUP_FILES+=("/tmp/.kura/")
	BACKUP_FILES+=("/tmp/coninfo-*")
	BACKUP_FILES+=("/var/log/esf.log")
	BACKUP_FILES+=("/var/log/kura.log")
	BACKUP_FILES+=("/etc/rc*.d/S*esf")
	BACKUP_FILES+=("/etc/rc*.d/S*kura")
	BACKUP_FILES+=("/etc/rc*.d/K*esf")
	BACKUP_FILES+=("/etc/rc*.d/K*kura")
	BACKUP_FILES+=("/etc/init.d/kura*")
	BACKUP_FILES+=("/etc/init.d/firewall")
	BACKUP_FILES+=("/etc/dhcpd-*.conf")
	BACKUP_FILES+=("/etc/named.conf")
	BACKUP_FILES+=("/etc/wpa_supplicant.conf")
	BACKUP_FILES+=("/etc/hostapd.conf")
	BACKUP_FILES+=("/etc/ppp/chat")
	BACKUP_FILES+=("/etc/ppp/peers")
	BACKUP_FILES+=("/etc/ppp/scripts")
	BACKUP_FILES+=("/etc/ppp/*ap-secrets")
	
	do_backup
	
	#remove existing files
	for BACKED_UP_FILE in ${BACKUP_FILES[@]};
	do
		rm -rf "$BACKED_UP_FILE" >> $LOG 2>&1
	done
	
	echo ""
	##############################################
	# END PRE-INSTALL SCRIPT
	##############################################
	
	echo "Extracting Kura files"
	SKIP=`awk '/^__TARFILE_FOLLOWS__/ { print NR + 1; exit 0; }' $0`
	
	# take the tarfile and pipe it into tar and redirect the output
	tail -n +$SKIP $0 | tar -xz >> $LOG 2>&1
	
	##############################################
	# POST INSTALL SCRIPT
	##############################################
	mkdir -p ${INSTALL_DIR} >> $LOG 2>&1
	unzip kura_*.zip -d ${INSTALL_DIR} >> $LOG 2>&1
	
	#install Kura files
	sed -i "s|^INSTALL_DIR=.*|INSTALL_DIR=${INSTALL_DIR}|" ${INSTALL_DIR}/kura_*/install/kura_install.sh
	sh ${INSTALL_DIR}/kura_*/install/kura_install.sh >> $LOG 2>&1
	
	#clean up
	rm -rf ${INSTALL_DIR}/kura/install >> $LOG 2>&1
	rm kura_*.zip >> $LOG 2>&1
	
	#move the log file
	mkdir -p ${INSTALL_DIR}/kura/log
	mv $LOG ${INSTALL_DIR}/kura/log/
	
	#flush all cached filesystem to disk
	sync
	
	echo ""
	echo ""
	echo "Finished.  Kura has been installed to ${INSTALL_DIR}/kura and will start automatically after a reboot"
	##############################################
	# END POST INSTALL SCRIPT
	##############################################
}

##############################################
# RUN INSTALLATION
##############################################
require_java_8

require install
require basename
require dirname
require tar
require unzip

echo ""
echo "Installing Kura..."
echo "Installing Kura..." > $LOG 2>&1

#Kill JVM and monit for installation
{ killall monit java || true; } >> $LOG 2>&1
	
run_kura_install
exit 0

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
