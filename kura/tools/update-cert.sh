#!/bin/bash

#
#  Copyright (c) 2025 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#     Eurotech
#

set -e

if [ "$#" -lt 4 ]; then
    echo "Usage: $0 <host:port> <keystore_path> <alias> <keystore_password>"
    echo "Example: $0 marketplace.eclipse.org:443 /path/to/keystore.jks marketplace changeit"
    exit 1
fi

HOST_PORT="$1"
KEYSTORE_PATH="$2"
ALIAS="$3"
KEYSTORE_PASSWORD="$4"

HOST=$(echo "$HOST_PORT" | cut -d':' -f1)
PORT=$(echo "$HOST_PORT" | cut -d':' -f2)

if [ -z "$PORT" ]; then
    echo "Error: Port not specified. Use format host:port"
    exit 1
fi

CERT_FILE=$(mktemp /tmp/cert_XXXXXX.pem)

echo "Retrieving certificate from $HOST:$PORT..."
openssl s_client -showcerts -connect "$HOST:$PORT" </dev/null 2>/dev/null | \
    sed -n '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/p' | \
    sed -n '1,/-----END CERTIFICATE-----/p' > "$CERT_FILE"

if [ ! -s "$CERT_FILE" ]; then
    echo "Error: Failed to retrieve certificate"
    rm -f "$CERT_FILE"
    exit 1
fi

echo "Certificate retrieved successfully"

echo "Removing existing alias '$ALIAS' from keystore (if exists)..."
keytool -delete -alias "$ALIAS" -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null || true

echo "Importing certificate with alias '$ALIAS'..."
keytool -import -noprompt -alias "$ALIAS" -file "$CERT_FILE" -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD"

rm -f "$CERT_FILE"

echo "Certificate updated successfully in keystore"
