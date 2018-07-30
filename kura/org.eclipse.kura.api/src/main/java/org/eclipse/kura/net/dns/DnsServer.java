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
package org.eclipse.kura.net.dns;

import org.eclipse.kura.KuraException;

/**
 * @since 2.0
 */
public interface DnsServer {

    public boolean isEnabled();

    public void enable() throws KuraException;

    public void disable() throws KuraException;

    public void restart() throws KuraException;

    public boolean isConfigured();

    public void setConfig(DnsServerConfig dnsServerConfig) throws KuraException;

    public DnsServerConfig getDnsServerConfig();

    public String getConfigFilename();

}
