package org.eclipse.kura.linux.position;

import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;

public interface PositionProvider {

    public void start();

    public void stop();

    public Position getPosition();

    public NmeaPosition getNmeaPosition();

    public String getNmeaTime();

    public String getNmeaDate();

    public boolean isLocked();

    public String getLastSentence();

    void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener);

}
