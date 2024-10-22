package org.eclipse.kura.linux.position.mm;

import java.time.LocalDateTime;
import java.util.Set;

import org.eclipse.kura.linux.position.GpsDeviceAvailabilityListener;
import org.eclipse.kura.linux.position.PositionProvider;
import org.eclipse.kura.linux.position.PositionProviderType;
import org.eclipse.kura.linux.position.PositionServiceOptions;
import org.eclipse.kura.linux.position.serial.GpsDevice.Listener;
import org.eclipse.kura.position.GNSSType;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;

public class ModemManagerPositionProvider implements PositionProvider {

    DbusCon
    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public Position getPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNmeaTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNmeaDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LocalDateTime getDateTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getLastSentence() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public PositionProviderType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<GNSSType> getGnssTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
