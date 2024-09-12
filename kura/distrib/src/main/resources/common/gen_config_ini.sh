#!/bin/sh

TEMPLATE=$1
ROOT=$2

usage() {
    >&2 echo "Usage: gen_config_ini.sh <config.ini template> <plugin root DIR_NAMEectory>"
}

abspath() {
    cd "${1}" || exit 1
    RESULT="${PWD}"
    cd "${OLDPWD}" || exit 1
    echo "${RESULT}"
}

if ! [ -e "${TEMPLATE}" ]
then
    >&2 echo "config.ini template not found"
    usage
    exit 1
fi

if ! [ -d "${ROOT}" ]
then
    >&2 echo "plugin root directory not found"
    usage
    exit 1
fi

ROOT=$(abspath "${ROOT}")

OSGI_BUNDLES=

for DIR_PATH in "${ROOT}"/*
do
    DIR_NAME=$(basename -- "${DIR_PATH}")

    if [ "${#DIR_NAME}" = 0 ] || [ "${#DIR_NAME}" -gt 2 ] || ! [ -d "${DIR_PATH}" ]
    then
        continue
    fi

    if ! expr "${DIR_NAME}" : '^[0-9]\{1,\}s\{0,1\}$' > /dev/null
    then
        continue
    fi

    START_LEVEL="${DIR_NAME%s}"
    
    if [ "${#DIR_NAME}" = "${#START_LEVEL}" ]
    then
        START=
    else
        START="\:start"
    fi

    for JAR in "${DIR_PATH}"/*.jar
    do
        if [ -n "${OSGI_BUNDLES}" ]
        then
            OSGI_BUNDLES="${OSGI_BUNDLES},"
        fi
        
        OSGI_BUNDLES="${OSGI_BUNDLES}reference\:file\:${JAR}@${START_LEVEL}${START}"
    done

done

cat "${TEMPLATE}"
echo
echo "osgi.bundles=${OSGI_BUNDLES}"