/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
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

    public void addAllChannels(final AssetModel other);

    public void replaceChannels(final AssetModel other);

    public boolean isValid();

    public interface ChannelModel {

        public GwtConfigParameter getParameter(int index);

        public void setValue(String id, String value);

        public boolean isValid(String id);

        public String getValue(String id);

        public String getChannelName();
    }
}
