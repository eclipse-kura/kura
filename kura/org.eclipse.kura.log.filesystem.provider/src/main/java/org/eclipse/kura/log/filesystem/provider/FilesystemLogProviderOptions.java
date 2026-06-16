/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.log.filesystem.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.log.filesystem.provider.FilesystemLogProvider", //
        name = "FilesystemLogProvider", //
        description = "Implementation of a log provider that reads entries from the specified log file path.")
public @interface FilesystemLogProviderOptions {

    @AttributeDefinition(name = "Log file path", //
            cardinality = 1, //
            required = true, //
            description = "Specifies the file path from which the logs are fetched.")
    String logFilePath() default "/var/log/kura.log";

}
