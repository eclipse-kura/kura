/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.emulator.position;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class GpsXmlHandler extends DefaultHandler {

    private static final String LABEL = "org.eclipse.kura.app.demo.kura.training.console.GpsXmlHandler: ";

    // Valid Elements in the xml file
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_ELEVATION = "ele";
    private static final String TAG_TIME = "time";

    private final ArrayList<GpsPoint> gpsPoints;
    private String latitude;
    private String longitude;
    private String elevation;
    private String time;

    private boolean foundTrackPoint = false;
    private boolean foundElevation = false;
    private boolean foundTime = false;

    public GpsXmlHandler() {
        this.gpsPoints = new ArrayList<GpsPoint>();
        this.latitude = null;
        this.longitude = null;
        this.elevation = null;
        this.time = null;
    }

    @Override
    public void startElement(String uri, String localName, String elementName, Attributes attributes) {

        if (TAG_TRACK_POINT.equals(elementName)) {
            this.foundTrackPoint = true;

            if (attributes.getLength() == 2) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).compareTo("lat") == 0) {
                        this.latitude = attributes.getValue(i);
                    } else if (attributes.getQName(i).compareTo("lon") == 0) {
                        this.longitude = attributes.getValue(i);
                    } else {
                        System.out.println(LABEL + "invalid attribute in trkpt element: " + attributes.getQName(i));
                    }
                }
            } else {
                System.out.println(LABEL + "there must be two attributes (lat and lon) in the trkpt element");
            }

            this.elevation = null;
            this.time = null;
        } else if (TAG_ELEVATION.equals(elementName)) {
            this.foundElevation = true;
        } else if (TAG_TIME.equals(elementName)) {
            this.foundTime = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String elementName) {

        if (TAG_TRACK_POINT.equals(elementName)) {
            this.foundTrackPoint = false;

            if (this.latitude != null && this.longitude != null && this.elevation != null && this.time != null) {
                this.gpsPoints.add(new GpsPoint(Double.parseDouble(this.latitude), Double.parseDouble(this.longitude),
                        Double.parseDouble(this.elevation), this.time));
            } else {
                System.out.println(LABEL + "the XML file is malformed");
            }
        } else if (TAG_ELEVATION.equals(elementName)) {
            this.foundElevation = false;
        } else if (TAG_TIME.equals(elementName)) {
            this.foundTime = false;
        }
    }

    @Override
    public void characters(char[] buf, int offset, int length) {
        String tag = new String(buf).substring(offset, offset + length).trim();

        if (!this.foundTrackPoint && !this.foundElevation && !this.foundTime) {
            return;
        }

        if (this.foundElevation) {
            if (this.elevation == null) {
                this.elevation = new String(buf, offset, length);
                return;
            } else {
                this.elevation = this.elevation + new String(buf, offset, length);
                return;
            }
        }

        if (this.foundTime) {
            if (this.time == null) {
                this.time = new String(buf, offset, length);
                return;
            } else {
                this.time = this.time + new String(buf, offset, length);
                return;
            }
        }

        System.out.println(LABEL + "found some odd data in services.xml");
        logDump(tag.getBytes());
    }

    public GpsPoint[] getGpsPoints() {
        GpsPoint[] data = new GpsPoint[this.gpsPoints.size()];

        for (int i = 0; i < this.gpsPoints.size(); i++) {
            data[i] = this.gpsPoints.get(i);
        }

        return data;
    }

    private void logDump(byte[] message) {
        for (int i = 0; i < message.length; i++) {
            if (i % 16 == 0) {
                if (i > 0) {
                    System.out.println();
                }
                System.out.print('\t');
            }
            if (message[i] < 0x10) {
                System.out.print("0x0" + Integer.toHexString(message[i] & 0x0ff) + " ");
            } else {
                System.out.print("0x" + Integer.toHexString(message[i] & 0x0ff) + " ");
            }
        }
    }

}
