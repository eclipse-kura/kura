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
package org.eclipse.kura.core.internal.linux.executor;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.executor.ExecutorFactory;

public class DefaultExecutorFactory implements ExecutorFactory {

    @Override
    public Executor getExecutor() {
        return new DefaultExecutor();
    }

}
