/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.hook.file.move;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.KuraException;

public class FileMoveDeploymentHookOptions {

    private static final String DESTINATION_PATH_PROPERTY_NAME = "destination";
    private static final String MODE_PROPERTY_NAME = "mode";
    private static final String OVERWRITE_DESTINATION = "overwrite";

    private String destinationPath;
    private boolean overwrite;
    private Mode mode;

    public enum Mode {
        COPY,
        MOVE
    }

    public FileMoveDeploymentHookOptions(Map<String, Object> properties) throws KuraException {
        final Object rawDestinationPath = properties.get(DESTINATION_PATH_PROPERTY_NAME);
        final Object rawMode = properties.get(MODE_PROPERTY_NAME);
        final Object rawOverwrite = properties.get(OVERWRITE_DESTINATION);

        requireNonNull(rawDestinationPath, DESTINATION_PATH_PROPERTY_NAME + " must be specified");

        this.destinationPath = rawDestinationPath.toString();

        if (rawMode == null) {
            mode = Mode.COPY;
        } else {
            final String modeString = rawMode.toString();
            if (modeString.equalsIgnoreCase(Mode.COPY.name())) {
                this.mode = Mode.COPY;
            } else if (modeString.equalsIgnoreCase(Mode.MOVE.name())) {
                this.mode = Mode.MOVE;
            } else {
                throw new IllegalArgumentException("Invalid mode: " + modeString);
            }
        }

        if (rawOverwrite != null) {
            if (rawOverwrite instanceof Boolean) {
                this.overwrite = (Boolean) rawOverwrite;
            }
            try {
                this.overwrite = Boolean.parseBoolean(rawOverwrite.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for overwrite field: " + overwrite);
            }
        }
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean shouldOverwrite() {
        return overwrite;
    }
}
