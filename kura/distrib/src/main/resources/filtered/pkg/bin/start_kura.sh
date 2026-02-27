#!/bin/bash
#
#  Copyright (c) 2025, 2026 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#
# Kura should be installed to the \${kura.install.dir} directory.
export PATH=/bin:/usr/bin:/sbin:/usr/sbin:/usr/local/bin:/opt/jvm/bin:/usr/java/bin:$PATH
export MALLOC_ARENA_MAX=1

KURA_RUNNING=$(pgrep -f ".*java.*org\.eclipse\.equinox\..*")

if [ -n "$KURA_RUNNING" ] ; then
    echo "Failed to start Kura. It is already running ..."
    exit 1
fi

DIR=$(cd $(dirname $0)/..; pwd)
cd "$DIR" || exit 1

IS_DEBUG_MODE="false"
IS_DETACHED_MODE="false"

while [[ $# -gt 0 ]]; do
    key="$1"

    case $key in
    -d | --detached)
        IS_DETACHED_MODE="true"
        ;;
    -x | --debug)
        IS_DEBUG_MODE="true"
        ;;
    -h | --help)
        echo
        echo "Options:"
        echo "    -d | --detached    run Kura in detached mode"
        echo "    -x | --debug       run Kura in debug mode"
        exit 0
        ;;
    *)
        echo "Unknown option."
        exit 1
        ;;
    esac
    shift # past argument or value
done

# set up the configuration area
mkdir -p /tmp/.kura/configuration
\${DIR}/bin/gen_config_ini.sh \${DIR}/framework/config.ini \${DIR}/plugins > /tmp/.kura/configuration/config.ini

if [[ -n "${KURA_DEBUG_MODE}" && "${KURA_DEBUG_MODE}" == "true" ]]; then
    IS_DEBUG_MODE="true"
fi

DEBUG_OPTS=""
EQUINOX_DEBUG_OPTS=""
if [[ $IS_DEBUG_MODE == "true" ]]; then
    DEBUG_OPTS="-Xdebug \
        -Xrunjdwp:server=y,transport=dt_socket,address=*:8000,suspend=n \
        -Xlog:gc=info:file=/var/log/kura-gc.log:time:filecount=10,filesize=10m \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=/var/log/kura-heapdump.hprof \
        -XX:ErrorFile=/var/log/kura-error.log"

    EQUINOX_DEBUG_OPTS="-console 5002 \
    -consoleLog"
fi

KURA_LAUNCH_COMMAND="exec java"

if [[ $IS_DETACHED_MODE == "true" ]]; then
    KURA_LAUNCH_COMMAND="nohup java"
fi

KURA_CMD="${KURA_LAUNCH_COMMAND} -Xms${kura.mem.size} -Xmx${kura.mem.size} \
    $DEBUG_OPTS \
    -XX:+IgnoreUnrecognizedVMOptions \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    --add-modules=ALL-SYSTEM \
    -Dkura.os.version=${kura.os.version} \
    -Dkura.arch=${kura.arch} \
    -Dtarget.device=${target.device} \
    -Declipse.ignoreApp=true \
    -Dkura.home=\${DIR} \
    -Dkura.configuration=file:\${DIR}/framework/kura.properties \
    -Dkura.custom.configuration=file:\${DIR}/user/kura_custom.properties \
    -Ddpa.configuration=\${DIR}/packages/dpa.properties \
    -Dlog4j.configurationFile=file:\${DIR}/log4j/log4j.xml \
    -Dlog4j2.disable.jmx=true \
    -Djdk.tls.trustNameService=true \
    -Declipse.consoleLog=true \
    -jar \${DIR}/plugins/org.eclipse.equinox.launcher-${org.eclipse.equinox.launcher.version}.jar \
    -configuration /tmp/.kura/configuration \
    $EQUINOX_DEBUG_OPTS"

if [[ $IS_DETACHED_MODE == "true" ]]; then
    eval "$KURA_CMD &"

    #Save the PID
    KURA_PID=$!
    echo "Kura Started (pid=$KURA_PID) ..."
    echo $KURA_PID > /var/run/kura.pid
else
    eval "$KURA_CMD"
fi
