package org.eclipse.kura.web.client.ui.wires;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public interface AssetModel {

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
