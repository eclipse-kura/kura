/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.driver.sensehat;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.example.driver.sensehat.SenseHatInterface.JoystickEventListener;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.JoystickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoystickHandler implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(JoystickHandler.class);

    private final Set<JoystickEventListener> joystickListeners = new CopyOnWriteArraySet<>();
    private final JoystickEventDispatcher joystickEventDispatcher;
    private final Map<Resource, Long> lastJoystickEventTimestamps = new HashMap<>();
    private Joystick joystick;

    public JoystickHandler(Joystick joystick) {
        this.joystick = joystick;
        this.joystickEventDispatcher = new JoystickEventDispatcher();
        this.joystickEventDispatcher.start();
    }

    public void addJoystickEventListener(JoystickEventListener listener) {
        this.joystickListeners.add(listener);
    }

    public void removeJoystickEventListener(JoystickEventListener listener) {
        this.joystickListeners.remove(listener);
    }

    private class JoystickEventDispatcher extends Thread {

        private AtomicBoolean run = new AtomicBoolean(true);

        public void run() {
            logger.info("JoystickEventDispatcher - starting...");
            while (run.get()) {
                final JoystickEvent event = joystick.read();
                if (event == null) {
                    logger.warn("JoystickEventDispatcher - got null event");
                    continue;
                }
                final Optional<Resource> resource = Resource.from(event);
                if (!resource.isPresent()) {
                    continue;
                }
                final long timestamp = event.getTimeSec() * 1000 + event.getTimeUSec() / 1000;
                lastJoystickEventTimestamps.put(resource.get(), timestamp);
                joystickListeners.forEach(listener -> listener.onJoystickEvent(resource.get(), timestamp));
            }
            logger.info("JoystickEventDispatcher - exiting...");
        }

        private void cancel() {
            this.run.getAndSet(false);
            this.interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        this.joystickEventDispatcher.cancel();
        Joystick.closeJoystick();
    }

    public Long getLastJoystickEventTimestamp(Resource event) {
        return this.lastJoystickEventTimestamps.get(event);
    }
}
