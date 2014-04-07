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


##############################################
# PRE-INSTALL SCRIPT
##############################################
echo ""
echo "Installing Kura..."
echo "Installing Kura..." > /tmp/kura_install.log 2>&1

#Kill JVM and monit for installation
killall monit java >> /tmp/kura_install.log 2>&1

#remove old Kura if present
(rpm -ev `rpm -qa | grep -i -e kura -e denali -e eclipse -e dynacor -e reliagate -e helios -e duracor | grep -v atom | grep -v jvm`) >> /tmp/kura_install.log 2>&1

#clean up old installation if present
rm -fr /opt/eclipse/* >> /tmp/kura_install.log 2>&1
rm -fr /tmp/.kura/ >> /tmp/kura_install.log 2>&1
rm /etc/init.d/firewall >> /tmp/kura_install.log 2>&1
rm /etc/dhcpd-*.conf >> /tmp/kura_install.log 2>&1
rm /etc/named.conf >> /tmp/kura_install.log 2>&1
rm /etc/wpa_supplicant.conf >> /tmp/kura_install.log 2>&1
rm /etc/hostapd.conf >> /tmp/kura_install.log 2>&1
rm /tmp/coninfo-* >> /tmp/kura_install.log 2>&1
rm /var/log/kura.log >> /tmp/kura_install.log 2>&1
rm -fr /etc/ppp/chat >> /tmp/kura_install.log 2>&1
rm -fr /etc/ppp/peers >> /tmp/kura_install.log 2>&1
rm -fr /etc/ppp/scripts >> /tmp/kura_install.log 2>&1
rm /etc/ppp/*ap-secrets >> /tmp/kura_install.log 2>&1
rm /etc/rc*.d/S*kura >> /tmp/kura_install.log 2>&1
rm kura-*.zip >> /tmp/kura_install.log 2>&1

#clean up and/or install OS specific stuff
HOSTNAME=`hostname`
if [ ${HOSTNAME} == "mini-gateway" ] ; then
	#MGW specific items
	mkdir /var/named >> /tmp/kura_install.log 2>&1

	#remove ntpd
	rm /etc/rc2.d/S20ntpd >> /tmp/kura_install.log 2>&1
	rm /etc/rc3.d/S20ntpd >> /tmp/kura_install.log 2>&1
	rm /etc/rc4.d/S20ntpd >> /tmp/kura_install.log 2>&1
	rm /etc/rc5.d/S20ntpd >> /tmp/kura_install.log 2>&1
fi

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
mkdir -p /opt/eclipse >> /tmp/kura_install.log 2>&1
unzip -o kura-*.zip -d /opt/eclipse >> /tmp/kura_install.log 2>&1

#install Kura files
sh /opt/eclipse/kura-*/install/kura_install.sh >> /tmp/kura_install.log 2>&1

#clean up
rm -rf /opt/eclipse/kura/install >> /tmp/kura_install.log 2>&1
rm kura-*.zip >> /tmp/kura_install.log 2>&1

#move the log file
mv /tmp/kura_install.log /opt/eclipse/kura/configuration/

#flush all cached filesystem to disk
sync

echo ""
echo "Finished.  Kura has been installed to /opt/eclipse/kura and will start automatically after a reboot"
exit 0
#############################################
# END POST INSTALL SCRIPT
##############################################

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
