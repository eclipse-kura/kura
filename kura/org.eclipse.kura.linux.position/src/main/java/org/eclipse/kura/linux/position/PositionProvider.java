package org.eclipse.kura.linux.position;

import java.time.LocalDateTime;

import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;

public interface PositionProvider {

    public void start();

    public void stop();

    public Position getPosition();

    /**
     * @deprecated
     * 
     */

    @Deprecated
    public NmeaPosition getNmeaPosition();

    /**
     * @deprecated
     * 
     */

    @Deprecated
    public String getNmeaTime();

    /**
     * @deprecated
     * 
     */

    @Deprecated
    public String getNmeaDate();

    public LocalDateTime getDateTime();

    public boolean isLocked();

    /**
     * @deprecated
     * 
     */

    @Deprecated
    public String getLastSentence();

    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener);

    public PositionProviderType getType();

}
