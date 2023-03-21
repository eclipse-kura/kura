package org.eclipse.kura.nm.status;

import java.util.List;

import org.eclipse.kura.net.wifi.WifiChannel;

public class SupportedChannelsProperties {

    private final String countryCode;
    private final List<WifiChannel> supportedChannels;

    public SupportedChannelsProperties(String countryCode, List<WifiChannel> supportedChannels) {
        this.countryCode = countryCode;
        this.supportedChannels = supportedChannels;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public List<WifiChannel> getSupportedChannels() {
        return supportedChannels;
    }
}
