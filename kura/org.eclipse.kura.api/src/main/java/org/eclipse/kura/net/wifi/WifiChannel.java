package org.eclipse.kura.net.wifi;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class WifiChannel {
    private Integer channel;
    private Float frequency;

    public WifiChannel(int channel, float frequency) {
        this.channel = channel;
        this.frequency = frequency;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Float getFrequency() {
        return frequency;
    }

    public void setFrequency(Float frequency) {
        this.frequency = frequency;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WifiChannel [");
        sb.append("channel: ").append(channel).append(", frequency:").append(frequency).append("GHz");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 29;
        int result = super.hashCode();

        result = prime * result + (this.channel == null ? 0 : this.channel.hashCode());
        result = prime * result + (this.frequency == null ? 0 : this.frequency.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WifiChannel)) {
            return false;
        }

        WifiChannel other = (WifiChannel) obj;
        if (!compare(this.channel, other.channel)) {
            return false;
        }
        if (!compare(this.frequency, other.frequency)) {
            return false;
        }
        return true;
    }

    private boolean compare(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

}
