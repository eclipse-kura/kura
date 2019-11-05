/*******************************************************************************
 * Copyright (c) 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.dns;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.dns.DnsServerConfig;

/**
 * Interface to manage the DNS Service running in the underlying operating system.
 * Allows to start, stop and restart the managed server, get the status and manage the configuration.
 *
 */
public interface DnsServerService {

    public boolean isRunning();

    public void start() throws KuraException;

    public void stop() throws KuraException;

    public void restart() throws KuraException;

    public boolean isConfigured();

    public void setConfig(DnsServerConfig dnsServerConfig);

    public DnsServerConfig getConfig();

}
