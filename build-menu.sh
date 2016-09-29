#!/bin/bash

#
# Copyright (c) 2016 Red Hat Inc and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     Red Hat Inc - initial API and implementation
#

set -e

## global settings

KURA_BUILD_SELECTION=~/.kura.build.selection
IGNORE_PROFILES=(default)

## Test if we have the "dialog" command

hash dialog &>/dev/null || {
  echo >&2
  echo >&2 "This script requires you to install the command 'dialog'. Exiting ..."

  if [[ "$OSTYPE" == "darwin"* ]]; then
    echo >&2 "  on Mac OS run"
    echo >&2 "     brew install dialog"

  elif [[ "$OSTYPE" == "linux-gnu" && -f /etc/redhat-release ]]; then
    echo >&2 "  on RHEL/Fedora/CentOS run:"
    if hash dnf &>/dev/null ; then
      echo >&2 "     sudo dnf install dialog"
    else
      echo >&2 "     sudo yum install dialog"
    fi

  elif [[ "$OSTYPE" == "linux-gnu" && -f /etc/debian_version ]]; then
    echo >&2 "  on Debian/Ubuntu run:"
    echo >&2 "     sudo apt-get install dialog"

  elif [[ "$OSTYPE" == "linux-gnu" && -f /etc/SuSE-release ]]; then
    echo >&2 "  on Suse run:"
    echo >&2 "     sudo zypper install dialog"

  else
    echo >&2 "  Unable to deterimine operating system"
    
  fi
  echo >&2
  exit 1
}

## detect all maven profiles of the "distrib" project

echo "Detecting profiles..."

PROFILES=$(mvn -N -f kura/distrib/pom.xml help:all-profiles  | grep "Profile Id"   | awk '{ print $3; }' | sort -u)

## clear out IGNOREs

declare -a p
for i in $PROFILES; do
  [[ " ${IGNORE_PROFILES[@]} " =~ " ${i} " ]] || p+=($i)
done
PROFILES="${p[@]}"

echo "Available profiles: $PROFILES"

## read previous selection

if ((BASH_VERSINFO[0] >= 4)); then
  test -r "$KURA_BUILD_SELECTION" && readarray -t oldsel < "$KURA_BUILD_SELECTION"
fi

## build command line

declare -a tags
for i in $PROFILES; do
  tags+=($i) # tag 
  tags+=("Profile: $i") # item

  state=$([[ " ${oldsel[@]} " =~ " ${i} " ]] && echo "on" || echo "off" )
  tags+=($state)
done

## execute dialog

set +e
exec 3>&1 
sel=$(dialog --checklist "Select Eclipse Kura build profiles" 20 70 18 "${tags[@]}" 2>&1 1>&3)
rc=$?
exec 3>&-
set -e

## test for abort

echo
test $rc -eq 0 || { echo "Selection aborted ..." ; exit 2; }

## store selection

echo "Storing as $KURA_BUILD_SELECTION ..."
echo $sel > "$KURA_BUILD_SELECTION"

## build target command

echo "Running profiles: $sel"

declare -a pl
for i in $PROFILES; do
 state=$([[ " ${sel[@]} " =~ " ${i} " ]] && echo "-P$i" || echo "-P!$i" )
 pl+=("$state")
done

echo "Arguments:" "${pl[@]}"
echo "All arguments:" "${pl[@]}" "$@"

## execute

./build-all.sh "${pl[@]}" "$@"
