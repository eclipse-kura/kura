#*******************************************************************************
# Copyright (c) 2011, 2016 Eurotech and/or its affiliates
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Eurotech
#*******************************************************************************
#!/bin/sh
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


#set the path
export PATH=$PATH:/opt/jvm/bin

#clean up old runs
cd /tmp/
killall java
rm -fr kura
rm kura.log
rm kura_test_report.txt
rm -fr kura-wrl-4.3_1.0.0-SNAPSHOT

#start the test
unzip kura-wrl-4.3_1.0.0-SNAPSHOT.zip
cd kura-wrl-4.3_1.0.0-SNAPSHOT
nohup bin/start_kura.sh &>/tmp/kura.log &
