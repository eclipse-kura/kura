/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public interface AssetModel {

    public String getAssetPid();

    public Set<String> getChannelNames();

    public List<ChannelModel> getChannels();

    public ChannelModel createNewChannel(String channelName);

    public void deleteChannel(String channelName);

    public GwtConfigComponent getChannelDescriptor();

    public GwtConfigComponent getConfiguration();

    public interface ChannelModel {

        public GwtConfigParameter getParameter(int index);

        public void setValue(String id, String value);

        public String getValue(String id);

        public String getChannelName();
    }
}
