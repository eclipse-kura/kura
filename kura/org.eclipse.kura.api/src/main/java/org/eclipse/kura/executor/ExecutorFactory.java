/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.executor;

import org.apache.commons.exec.Executor;

/**
 * This interface provides a method for getting the executor to be used to run commands.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ExecutorFactory {

    /**
     * Get the command executor.
     *
     */
    public Executor getExecutor();
}
