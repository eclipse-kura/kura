package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtWifiChannelFrequency extends KuraBaseModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String FREQUENCY = "frequency";
    private static final String CHANNEL = "channel";

    public Integer getChannel() {
        if (get(CHANNEL) != null) {
            return get(CHANNEL);
        } else {
            return 0;
        }
    }

    public void setChannel(int channel) {
        set(CHANNEL, channel);
    }

    public Float getFrequency() {
        if (get(FREQUENCY) != null) {
            return get(FREQUENCY);
        } else {
            return 0.0f;
        }
    }

    public void setFrequency(float frequency) {
        set(FREQUENCY, frequency);
    }
}
