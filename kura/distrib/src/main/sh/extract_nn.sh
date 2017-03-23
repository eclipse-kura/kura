#!/bin/bash
#
# Copyright (c) 2011, 2014 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Eurotech
#

INSTALL_DIR=/opt/eclipse
TIMESTAMP=`date +%Y%m%d%H%M%S`
LOG=/tmp/kura_install_${TIMESTAMP}.log
WD_TMP_FILE=/tmp/watchdog
REFRESH_TIME=5
TIMEOUT_TIME=300

##############################################
# UTILITY FUNCTIONS
##############################################

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
    
# Deactivate watchdog device if possible
function stop_watchdog {
    if [ -n "${WATCHDOG_DEVICE}" ]; then
        echo V > ${WATCHDOG_DEVICE}
    fi
}

##############################################
# END UTILITY FUNCTIONS
##############################################

function kura_install {
	##############################################
	# PRE-INSTALL SCRIPT
	##############################################
	
	#clean up old installation if present
	rm -fr /opt/eclipse/data >> $LOG 2>&1
	rm -fr /opt/eclipse/esf* >> $LOG 2>&1
	rm -fr /opt/eclipse/kura* >> $LOG 2>&1
	rm -fr ${INSTALL_DIR}/kura* >> $LOG 2>&1
	rm -fr /tmp/.esf/ >> $LOG 2>&1
	rm -fr /tmp/.kura/ >> $LOG 2>&1
	rm /tmp/coninfo-* >> $LOG 2>&1
	rm /var/log/esf.log >> $LOG 2>&1
	rm /var/log/kura.log >> $LOG 2>&1
	rm /etc/rc*.d/S*kura >> $LOG 2>&1
	rm kura-*.zip >> $LOG 2>&1
	rm kura_*.zip >> $LOG 2>&1
	
	echo ""
	##############################################
	# END PRE-INSTALL SCRIPT
	##############################################
	
	echo "Extracting Kura files"
	SKIP=`awk '/^__TARFILE_FOLLOWS__/ { print NR + 1; exit 0; }' $0`
	
	# take the tarfile and pipe it into tar and redirect the output
	tail -n +$SKIP $0 | tar -xz
	
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
	echo "Finished.  Kura has been installed to ${INSTALL_DIR}/kura and will start automatically after a reboot"
	##############################################
	# END POST INSTALL SCRIPT
	##############################################
}

echo ""
echo "Installing Kura..."
echo "Installing Kura..." > $LOG 2>&1

#Kill JVM and monit for installation
killall monit java >> $LOG 2>&1

run_kura_install
exit 0

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
