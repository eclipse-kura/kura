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


OLD_VERSION=
BASE_DIR=
INSTALL_DIR=
REMOVE_LIST=

TMP=/tmp/kura_upgrade
WD_TMP_FILE=/tmp/watchdog
REFRESH_TIME=5
TIMEOUT_TIME=300
TIMESTAMP=`date +%Y%m%d%H%M%S`
LOG=/tmp/kura_upgrade_${TIMESTAMP}.log
ABSOLUTE_PATH=`readlink -m $0`

HOSTAPD_FILE=/etc/hostapd
WPASUPPLICANT_FILE=/etc/wpa_supplicant

# Assume we will fail
SUCCESS=1

# Signal handler. Also called on exit
cleanup() {
	# remove .dp file and dpa.properties entry if it exists
	# wait for the dp to get written to disk first
	sync
	sleep 3
	KURA_DP=`grep -e "^kura-upgrade=" ${BASE_DIR}/kura/kura/dpa.properties |cut -d'=' -f2`
	KURA_DP=${KURA_DP#file\\:}
	if [ -n "${KURA_DP}" ]; then
	   echo "Found kura upgrade deployment package file: $KURA_DP" >> $LOG 2>&1
	   if [ -n "$KURA_DP" ]; then
		  echo "Removing kura upgrade deployment package" >> $LOG 2>&1
   	 	  sed "/^kura-upgrade=.*/d" ${BASE_DIR}/kura/kura/dpa.properties > /tmp/dpa.properties
    	  mv -f /tmp/dpa.properties ${BASE_DIR}/kura/kura/dpa.properties >> $LOG 2>&1
    	  rm -f $KURA_DP >> $LOG 2>&1
       fi
	fi

    # Remove the upgrade installation directory on fail
    if [ $SUCCESS -ne 0 ]; then
        echo "Could not upgrade - Remove the upgrade installation directory" >> $LOG 2>&1
        rm -rf "${BASE_DIR}/${INSTALL_DIR}" >> $LOG 2>&1
    fi

    # Remove temporary stuff
    rm -rf "$TMP" >> $LOG 2>&1
    rm -rf "${BASE_DIR}/${INSTALL_DIR}/install" >> $LOG 2>&1
    rm -f kura-*.zip >> $LOG 2>&1

    # Save the log file in a persistent directory
    mkdir -p ${BASE_DIR}/kura/log
    cp -f $LOG ${BASE_DIR}/kura/log

    # Always stop watchdog, sync and reboot
    stop_watchdog
    sync
    reboot
}

##############################################
# UTILITY FUNCTIONS
##############################################

# Run upgrade function and start watchdog if needed
function run_kura_upgrade {
    if [ -f "${WD_TMP_FILE}" ]; then
        WATCHDOG_DEVICE=`cat ${WD_TMP_FILE}`
        echo "Got watchdog ${WATCHDOG_DEVICE}" >> $LOG 2>&1
        kura_upgrade &
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
		kura_upgrade &
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

# Run cleanup function and start watchdog if needed
function run_cleanup {
    if [ -f "${WD_TMP_FILE}" ]; then
        WATCHDOG_DEVICE=`cat ${WD_TMP_FILE}`
        echo "Got watchdog ${WATCHDOG_DEVICE}" >> $LOG 2>&1
        cleanup &
        PID=$!

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
		cleanup &
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

# Deactivate watchdog device if possible
function stop_watchdog {
    if [ -n "${WATCHDOG_DEVICE}" ]; then
        echo V > ${WATCHDOG_DEVICE}
    fi
}

##############################################
# END UTILITY FUNCTIONS
##############################################

function kura_upgrade {
	##############################################
	# PRE-INSTALL SCRIPT
	##############################################
	sleep 3

	# remove OSGi storage directory
	if [ -d "/tmp/.kura/configuration" ]; then
		echo "Removing OSGi storage directory..." >> $LOG 2>&1
		rm -rf /tmp/.kura/configuration >> $LOG 2>&1
	fi

	# Make a copy of the previous installation using hard links
	echo "Creating hard link copy of previous version into ${INSTALL_DIR}" >> $LOG 2>&1
	mkdir "${BASE_DIR}/${INSTALL_DIR}"
	cd "${BASE_DIR}/kura" && find . -type d | cpio -dp ${BASE_DIR}/${INSTALL_DIR} >> $LOG 2>&1
	cd "${BASE_DIR}/kura" && find . -type f -exec ln {} ${BASE_DIR}/${INSTALL_DIR}/{} \; >> $LOG 2>&1

	# Replace hard links with real copies for certain files
	FILES=" \
		bin/* \
		data/* \
		log/kura_install_*.log \
		kura/config.ini \
		kura/dpa.properties \
		kura/kura.properties \
		kura/kura_custom.properties \
		kura/log4j.properties \
		kura/log4j.xml
	"
	for f in $FILES
	do
		if [ -f "${BASE_DIR}/kura/$f" ]; then
			target="${BASE_DIR}/${INSTALL_DIR}/$f"
			echo "Creating a real copy of $target" >> $LOG 2>&1
			rm -rf $target >> $LOG 2>&1
			# copy file, removing the filename from the target to support wildcards
			cp -r ${BASE_DIR}/kura/$f ${target%/*} >> $LOG 2>&1
		fi
	done

	echo "" >> $LOG 2>&1
	##############################################
	# END PRE-INSTALL SCRIPT
	##############################################

	echo "Extracting tar file..." >> $LOG 2>&1
	SKIP=`awk '/^__TARFILE_FOLLOWS__/ { print NR + 1; exit 0; }' $ABSOLUTE_PATH`
	echo "SKIP: ${SKIP}, file: ${0}" >> $LOG 2>&1

	# take the tarfile and pipe it into tar and redirect the output
	cd $TMP && tail -n +$SKIP $ABSOLUTE_PATH | tar -xz >> $LOG 2>&1
	echo "FINISHED TAR" >> $LOG 2>&1

	##############################################
	# POST INSTALL SCRIPT
	##############################################

	# Remove files not needed in the new version
	echo "Removing old files..." >> $LOG 2>&1
	while read line
	do
		# Skip comments
		if [[ $line == "#"* ]] ; then
			continue
		fi

		# TODO - remove files outside of kura directory
		rmfile="${BASE_DIR}/${INSTALL_DIR}/$line"
		echo "Removing $rmfile" >> $LOG 2>&1
		rm -f $rmfile
	done < ${TMP}/${REMOVE_LIST}

	# Extract new files
	unzip -o ${TMP}/kura_*.zip -d ${BASE_DIR} >> $LOG 2>&1

	# set permissions
	chmod +x ${BASE_DIR}/${INSTALL_DIR}/bin/*.sh >> $LOG 2>&1

	# read the absolute path of the old installation directory from the link
	OLD_INSTALL_PATH=`readlink -f ${BASE_DIR}/kura`

	# Point symlink to new version
	rm -f ${BASE_DIR}/kura
	find ${BASE_DIR} \! -name '${INSTALL_DIR}' -delete
	ln -s ${BASE_DIR}/${INSTALL_DIR} ${BASE_DIR}/kura

	# if /etc/hostapd.conf file exists and /etc/hostapd-wlan0.conf file doesn't exist
	# rename /etc/hostapd.conf to /etc/hostapd-wlan0.conf
	if [[ -f ${HOSTAPD_FILE}.conf && ! -f ${HOSTAPD_FILE}-wlan0.conf ]]; then
	    mv ${HOSTAPD_FILE}.conf ${HOSTAPD_FILE}-wlan0.conf
	fi

	# if /etc/wpa_supplicant.conf file exists and /etc/wpa_supplicant-wlan0.conf file doesn't exist
	# rename /etc/wpa_supplicant.conf to /etc/wpa_supplicant-wlan0.conf
	if [[ -f ${WPASUPPLICANT_FILE}.conf && ! -f ${WPASUPPLICANT_FILE}-wlan0.conf ]]; then
	    mv ${WPASUPPLICANT_FILE}.conf ${WPASUPPLICANT_FILE}-wlan0.conf
	fi

	# Upgrade was successful
	SUCCESS=0

	echo "Removing the old installation directory: ${OLD_INSTALL_PATH}" >> $LOG 2>&1
	rm -rf ${OLD_INSTALL_PATH} >> $LOG 2>&1

	echo "" >> $LOG 2>&1
	echo "Finished.  Kura has been upgraded in ${BASE_DIR}/kura and system will now reboot" >> $LOG 2>&1

	#############################################
	# END POST INSTALL SCRIPT
##############################################
}

mkdir -p $TMP
echo "Upgrading Kura..." > $LOG

# Check currently installed version
CURRENT_VERSION=`grep -e "^kura.version=" ${BASE_DIR}/kura/kura/kura.properties |cut -d'=' -f2`
if [ "$CURRENT_VERSION" != "$OLD_VERSION" ]; then
    echo "Could not upgrade - Currently installed version is not $OLD_VERSION" | tee $LOG 2>&1
    exit 1
fi

# Check that upgraded version does not already exist
if [ -d "${BASE_DIR}/${INSTALL_DIR}" ]; then
    echo "Could not upgrade - Updated version already exists in ${BASE_DIR}/${INSTALL_DIR}" | tee $LOG 2>&1
    exit 1
fi

# Exit performing cleanup at the first error
trap run_cleanup INT TERM EXIT
set -e

# kill JVM and monit for upgrade
echo "Stopping monit and kura" >> $LOG 2>&1
{ killall monit java || true; } >> $LOG 2>&1

run_kura_upgrade
exit 0

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
