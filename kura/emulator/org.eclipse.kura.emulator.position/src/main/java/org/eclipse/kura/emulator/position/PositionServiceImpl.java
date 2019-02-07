/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator.position;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionListener;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl implements PositionService, ConfigurableComponent {

    private static final String USE_GPSD_PROPERTY_NAME = "useGpsd";

    private static final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class);

    private static String LOCATION = "boston";

    private ComponentContext ctx;
    private EventAdmin eventAdmin;

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private GpsPoint[] gpsPoints;
    private Position currentPosition;
    private NmeaPosition currentNmeaPosition;
    private Date currentTime;
    private int index = 0;
    private boolean useGpsd;

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        //
        // save the bundle context
        this.ctx = componentContext;
        this.useGpsd = false;
        if (properties != null) {
            if (properties.get(USE_GPSD_PROPERTY_NAME) != null) {
                this.useGpsd = (Boolean) properties.get(USE_GPSD_PROPERTY_NAME);
            }
            if (this.useGpsd) {
                logger.info("USE GPSD");
            }
        }

        start();
    }

    public void updated(Map<String, Object> properties) {
        if (properties != null) {
            if (properties.get(USE_GPSD_PROPERTY_NAME) != null) {
                this.useGpsd = (Boolean) properties.get(USE_GPSD_PROPERTY_NAME);
            }
            if (this.useGpsd) {
                logger.info("USE GPSD");
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Stopping position service");
        stop();
    }

    @Override
    public Position getPosition() {
        return this.currentPosition;
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return this.currentNmeaPosition;
    }

    @Override
    public String getNmeaTime() {
        return this.currentTime.toString();
    }

    @Override
    public String getNmeaDate() {
        return this.currentTime.toString();
    }

    @Override
    public boolean isLocked() {
        // Always return true
        return true;
    }

    @Override
    public String getLastSentence() {
        // Not supported in emulator mode since this is not NMEA
        return null;
    }

    public void start() {

        this.index = 0;

        String fileName = null;
        if ("boston".equals(LOCATION)) {
            fileName = "boston.gpx";
        } else if ("denver".equals(LOCATION)) {
            fileName = "denver.gpx";
        } else if ("paris".equals(LOCATION)) {
            fileName = "paris.gpx";
        } else if ("test".equals(LOCATION)) {
            fileName = "test.gpx";
        }

        GpsXmlHandler handler = new GpsXmlHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setValidating(false);

            // Create the builder and parse the file
            SAXParser parser = factory.newSAXParser();
            logger.debug("Parsing: {}", fileName);

            BundleContext bundleContext = this.ctx.getBundleContext();
            URL url = bundleContext.getBundle().getResource(fileName);
            InputStream is = url.openStream();

            parser.parse(is, handler);
            this.gpsPoints = handler.getGpsPoints();
        } catch (Exception e) {
            logger.warn("Exception while parsing the position file", e);
        }

        // schedule a new worker based on the properties of the service
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updateGps();
            }
        }, 0, 5, TimeUnit.SECONDS);

        logger.debug("posting event");
        this.eventAdmin.postEvent(new PositionLockedEvent(new HashMap<String, Object>()));
    }

    public void stop() {
        if (this.handle != null) {
            this.handle.cancel(true);
            this.handle = null;
        }

        this.worker = null;
    }

    private void updateGps() {
        logger.debug("GPS Emulator index: {}", this.index);
        if (this.index + 1 == this.gpsPoints.length) {
            logger.debug("GPS Emulator - wrapping index");
            this.index = 0;
        }

        Measurement latitude = new Measurement(java.lang.Math.toRadians(this.gpsPoints[this.index].getLatitude()),
                Unit.rad);
        Measurement longitude = new Measurement(java.lang.Math.toRadians(this.gpsPoints[this.index].getLongitude()),
                Unit.rad);
        Measurement altitude = new Measurement(this.gpsPoints[this.index].getAltitude(), Unit.m);

        logger.debug("Updating latitude: {}", latitude);
        logger.debug("Updating longitude: {}", longitude);
        logger.debug("Updating altitude: {}", altitude);

        // Measurement lat, Measurement lon, Measurement alt, Measurement speed, Measurement track
        this.currentTime = new Date();
        this.currentPosition = new Position(latitude, longitude, altitude, null, null);
        this.currentNmeaPosition = new NmeaPosition(this.gpsPoints[this.index].getLatitude(),
                this.gpsPoints[this.index].getLongitude(), this.gpsPoints[this.index].getAltitude(), 0, 0);

        this.index++;

        return;
    }

    @Override
    public void registerListener(String listenerId, PositionListener positionListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterListener(String listenerId) {
        // TODO Auto-generated method stub

    }
}
