/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.net;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;

public interface NetworkConfigurationVisitor {

    public void visit(NetworkConfiguration config) throws KuraException;

    public void setExecutorService(CommandExecutorService executorService);
}
