package org.eclipse.kura.linux.position;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialDevicePositionProvider implements PositionProvider {

    private static final Logger logger = LoggerFactory.getLogger(SerialDevicePositionProvider.class);

    private GpsDeviceTracker gpsDeviceTracker;
    private ModemGpsStatusTracker modemGpsStatusTracker;
    private ConnectionFactory connectionFactory;

    private GpsDevice gpsDevice;

    private PositionServiceOptions configuration;

    private Listener gpsDeviceListener;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
    }

    public void setGpsDeviceTracker(final GpsDeviceTracker tracker) {
        this.gpsDeviceTracker = tracker;
    }

    public void unsetGpsDeviceTracker(final GpsDeviceTracker tracker) {
        tracker.setListener(null);
    }

    public void setModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        this.modemGpsStatusTracker = modemGpsDeviceTracker;
    }

    public void unsetModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        modemGpsDeviceTracker.setListener(null);
        this.modemGpsStatusTracker = null;
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {

        this.gpsDeviceTracker.setListener(gpsDeviceAvailabilityListener);
        this.modemGpsStatusTracker.setListener(gpsDeviceAvailabilityListener);

        this.gpsDeviceListener = gpsDeviceListener;
        this.configuration = configuration;
    }

    @Override
    public void start() {
        this.gpsDevice = openGpsDevice();
    }

    @Override
    public void stop() {
        this.gpsDeviceTracker.reset();

        if (this.gpsDevice != null) {
            this.gpsDevice.disconnect();
            this.gpsDevice = null;
        }
    }

    private GpsDevice openGpsDevice() {

        logger.info("Opening GPS device...");

        GpsDevice device = openGpsDevice(this.modemGpsStatusTracker.getGpsDeviceUri());

        if (device != null) {
            logger.info("Opened modem GPS device");
            return device;
        } else {
            this.modemGpsStatusTracker.reset();
        }

        device = openGpsDevice(this.configuration.getGpsDeviceUri());

        if (device != null) {
            logger.info("Opened GPS device from configuration");
        } else {
            logger.info("GPS device not available");
        }

        return device;
    }

    private GpsDevice openGpsDevice(CommURI uri) {

        uri = this.gpsDeviceTracker.track(uri);

        if (uri == null) {
            return null;
        }

        try {
            return new GpsDevice(this.connectionFactory, uri, this.gpsDeviceListener);
        } catch (Exception e) {
            logger.warn("Failed to open GPS device: {}", uri, e);
            return null;
        }
    }

    @Override
    public Position getPosition() {
        return this.gpsDevice.getPosition();
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return this.gpsDevice.getNmeaPosition();
    }

    @Override
    public String getNmeaTime() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getTimeNmea();
        } else {
            return null;
        }
    }

    @Override
    public String getNmeaDate() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getDateNmea();
        } else {
            return null;
        }
    }

    @Override
    public boolean isLocked() {
        if (!this.configuration.isEnabled()) {
            return false;
        }
        if (this.configuration.isStatic()) {
            return true;
        }
        return this.gpsDevice != null && this.gpsDevice.isValidPosition();
    }

    @Override
    public String getLastSentence() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getLastSentence();
        } else {
            return null;
        }
    }

}
