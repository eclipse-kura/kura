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
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBufferRaw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FramebufferHandler implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(FramebufferHandler.class);

    private final FrameBufferRaw fb;

    public FramebufferHandler(final FrameBufferRaw fb) {
        this.fb = fb;
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Future<?> writeMessageTask;

    private void cancelWriteMessage() {
        if (writeMessageTask != null) {
            writeMessageTask.cancel(true);
            writeMessageTask = null;
        }
    }

    public synchronized void runFramebufferRequest(final FramebufferRequest request) throws IOException {

        request.getFrontColorRed().ifPresent(color -> {
            fb.setFrontColorRed(color);
        });
        request.getFrontColorGreen().ifPresent(color -> {
            fb.setFrontColorGreen(color);
        });
        request.getFrontColorBlue().ifPresent(color -> {
            fb.setFrontColorBlue(color);
        });

        request.getBackColorRed().ifPresent(color -> {
            fb.setBackColorRed(color);
        });
        request.getBackColorGreen().ifPresent(color -> {
            fb.setBackColorGreen(color);
        });
        request.getBackColorBlue().ifPresent(color -> {
            fb.setBackColorBlue(color);
        });

        request.getTransform().ifPresent(transform -> {
            fb.setTransform(transform);
        });

        final Optional<String> writeMessage = request.getMessage();
        final Optional<byte[]> writeRGB565Pixels = request.getRGB565Pixels();
        final Optional<byte[]> writeMonochromePixels = request.getMonochromePixels();

        if (request.shouldClear()) {
            cancelWriteMessage();
            fb.clearFrameBuffer();
        } else if (writeRGB565Pixels.isPresent()) {
            cancelWriteMessage();
            fb.setPixelsRGB565(writeRGB565Pixels.get());
        } else if (writeMonochromePixels.isPresent()) {
            cancelWriteMessage();
            fb.setPixelsMonochrome(writeMonochromePixels.get());
        } else if (writeMessage.isPresent()) {
            cancelWriteMessage();
            this.writeMessageTask = executor.submit(() -> {
                try {
                    fb.showMessage(writeMessage.get());
                } catch (IOException e) {
                    logger.warn("Failed to write message", e);
                }
            });
        }
    }

    @Override
    public synchronized void close() throws IOException {
        fb.closeFrameBuffer();
        this.executor.shutdown();
    }

}
