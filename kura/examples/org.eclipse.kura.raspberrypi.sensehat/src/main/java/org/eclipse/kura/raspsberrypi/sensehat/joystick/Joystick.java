/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.raspsberrypi.sensehat.joystick;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Joystick {

    private static final Logger s_logger = LoggerFactory.getLogger(Joystick.class);

    private static final String SENSE_HAT_EVDEV_NAME = "Raspberry Pi Sense HAT Joystick";
    private static final int EVENT_SIZE = 16;
    public static final int EV_KEY = 1;
    public static final int STATE_RELEASE = 0;
    public static final int STATE_PRESS = 1;
    public static final int STATE_HOLD = 2;

    public static final int KEY_UP = 103;
    public static final int KEY_LEFT = 105;
    public static final int KEY_RIGHT = 106;
    public static final int KEY_DOWN = 108;
    public static final int KEY_ENTER = 28;

    private static Joystick SenseHatJoystick = new Joystick();
    private static ByteBuffer bb;
    private static FileChannel deviceInput;
    private static FileInputStream fis;
    private static JoystickEvent je;

    private static File inputFolder = new File("/sys/class/input/");
    private static File joystickEventFile = null;

    private Joystick() {
    }

    public static Joystick getJoystick() {

        bb = ByteBuffer.allocate(EVENT_SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        je = new JoystickEvent();

        BufferedReader br = null;
        String currentLine;
        for (final File eventFolder : inputFolder.listFiles()) {
            if (eventFolder.getName().contains("event")) {

                try {
                    br = new BufferedReader(new FileReader(eventFolder + "/device/name"));
                    currentLine = br.readLine();
                    if (null != currentLine && currentLine.equals(SENSE_HAT_EVDEV_NAME)) {
                        String eventFolderPath = eventFolder.getAbsolutePath();
                        joystickEventFile = new File(
                                "/dev/input/event" + eventFolderPath.substring(eventFolderPath.length() - 1));
                        br.close();
                        break;
                    }
                } catch (IOException e) {
                    s_logger.error("Error in opening file {}", e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            s_logger.error("Error in closing file {}", e);
                        }
                    }
                }

            }
        }

        try {
            fis = new FileInputStream(joystickEventFile);
        } catch (FileNotFoundException e) {
            s_logger.error("Joystick resource not found. {}", e);
        }
        deviceInput = fis.getChannel();

        return SenseHatJoystick;
    }

    public JoystickEvent read() {

        bb.clear();
        je.clear();
        try {
            deviceInput.read(bb);
            if (!bb.hasRemaining()) {
                bb.flip();
                je.parse(bb.asShortBuffer());
            }
        } catch (IOException e) {
        }

        return je;

    }

    public static void closeJoystick() {

        try {
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            s_logger.error("Error in closing resources", e);
        }

    }
}
