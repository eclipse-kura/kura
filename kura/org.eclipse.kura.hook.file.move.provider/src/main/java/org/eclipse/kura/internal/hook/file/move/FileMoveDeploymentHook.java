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

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.eclipse.kura.deployment.hook.RequestContext;
import org.eclipse.kura.internal.hook.file.move.FileMoveDeploymentHookOptions.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMoveDeploymentHook implements DeploymentHook {

    private static final Logger logger = LoggerFactory.getLogger(FileMoveDeploymentHook.class);

    @Override
    public void preDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
    }

    @Override
    public void postDownload(RequestContext context, Map<String, Object> properties) throws KuraException {

        try {
            FileMoveDeploymentHookOptions options = new FileMoveDeploymentHookOptions(properties);

            File sourceFile = new File(context.getDownloadFilePath());
            File destinationFile = new File(options.getDestinationPath());

            if (!sourceFile.exists()) {
                throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "Source file not found: " + sourceFile);
            }

            if (sourceFile.isDirectory()) {
                throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "Moving directories is not supported");
            }

            if (!destinationFile.isAbsolute()) {
                throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "Destination path must be absolute");
            }

            if (destinationFile.isDirectory()) {
                final String sourceFileName = sourceFile.getName();
                // DEPLOY-V2 downloaded files always end with the -<version>.dp or -<version>.sh suffixes, remove the
                // suffix
                destinationFile = new File(destinationFile,
                        sourceFileName.substring(0, sourceFileName.lastIndexOf('-')));
            }

            final ArrayList<CopyOption> copyOptions = new ArrayList<>();
            if (options.shouldOverwrite()) {
                copyOptions.add(StandardCopyOption.REPLACE_EXISTING);
            }
            final CopyOption[] copyOptionsArray = copyOptions.toArray(new CopyOption[copyOptions.size()]);

            if (options.getMode() == Mode.COPY) {
                logger.info("copying {} to {}...", sourceFile, destinationFile);
                Files.copy(sourceFile.toPath(), destinationFile.toPath(), copyOptionsArray);
                logger.info("copying {} to {}...done", sourceFile, destinationFile);
            } else {
                logger.info("moving {} to {}...", sourceFile, destinationFile);
                Files.move(sourceFile.toPath(), destinationFile.toPath(), copyOptionsArray);
                logger.info("moving {} to {}...done", sourceFile, destinationFile);
            }
        } catch (KuraException e) {
            throw e;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void postInstall(RequestContext context, Map<String, Object> properties) throws KuraException {
    }

}
