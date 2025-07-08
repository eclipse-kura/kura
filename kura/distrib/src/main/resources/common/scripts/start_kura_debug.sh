#!/bin/sh

export PATH=/bin:/usr/bin:/sbin:/usr/sbin:/usr/local/bin:/opt/jvm/bin:/usr/java/bin:$PATH
export MALLOC_ARENA_MAX=1

DIR=$(cd $(dirname $0)/..; pwd)
cd $DIR

# set up the configuration area
mkdir -p /tmp/.kura/configuration
${DIR}/bin/gen_config_ini.sh ${DIR}/framework/config.ini ${DIR}/plugins > /tmp/.kura/configuration/config.ini

KURA_RUNNING=`ps ax | grep java | grep "org.eclipse.equinox"`

if [ -z "$KURA_RUNNING" ] ; then
        exec java -Xms${kura.mem.size} -Xmx${kura.mem.size} \
        -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/var/log/kura-gc.log \
        -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M \
        -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/kura-heapdump.hprof \
        -XX:ErrorFile=/var/log/kura-error.log \
        -XX:+IgnoreUnrecognizedVMOptions \
        --add-opens java.base/java.lang=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        --add-modules=ALL-SYSTEM \
        -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=*:8000,suspend=n \
        -Dkura.os.version=${kura.os.version} \
        -Dkura.arch=${kura.arch} \
        -Dtarget.device=${target.device} \
        -Declipse.ignoreApp=true \
        -Dkura.home=${DIR} \
        -Dkura.configuration=file:${DIR}/framework/kura.properties \
        -Dkura.custom.configuration=file:${DIR}/user/kura_custom.properties \
        -Ddpa.configuration=${DIR}/packages/dpa.properties \
        -Dlog4j.configurationFile=file:${DIR}/log4j/log4j.xml \
        -Djava.util.logging.config.file=${DIR}/framework/logging.properties \
        -Djava.security.policy=${DIR}/framework/jdk.dio.policy \
        -Djdk.dio.registry=${DIR}/framework/jdk.dio.properties \
        -Djdk.tls.trustNameService=true \
        -Dosgi.console \
        -Declipse.consoleLog=true \
        -jar ${DIR}/plugins/org.eclipse.equinox.launcher-${org.eclipse.equinox.launcher.version}.jar \
        -configuration  /tmp/.kura/configuration
else
        echo "Failed to start Kura. It is already running ..."
fi
