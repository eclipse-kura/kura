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

import java.util.Optional;

import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBufferRaw;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.Transform;

public class FramebufferRequest {

    private Optional<Float> frontColorRed = Optional.empty();
    private Optional<Float> frontColorGreen = Optional.empty();
    private Optional<Float> frontColorBlue = Optional.empty();

    private Optional<Float> backColorRed = Optional.empty();
    private Optional<Float> backColorGreen = Optional.empty();
    private Optional<Float> backColorBlue = Optional.empty();

    private Optional<String> message = Optional.empty();
    private Optional<byte[]> pixelsRGB565 = Optional.empty();
    private Optional<byte[]> pixelsMonochrome = Optional.empty();
    private Optional<Transform> transform = Optional.empty();
    private boolean clear = false;

    public void writeMessage(String message) {
        this.message = Optional.of(message);
    }

    public void writeRGB565Pixels(byte[] pixels) {
        assertBufferLength(pixels, FrameBufferRaw.RGB565_BUFFER_SIZE);
        this.pixelsRGB565 = Optional.of(pixels);
    }

    public void writeMonochromePixels(byte[] pixels) {
        assertBufferLength(pixels, FrameBufferRaw.MONOCHROME_BUFFER_SIZE);
        this.pixelsMonochrome = Optional.of(pixels);
    }

    public void transform(int rotation) {
        rotation = (rotation % 360) / 90;
        if (rotation == 1) {
            transform(Transform.ROTATE_90);
        } else if (rotation == 2) {
            transform(Transform.ROTATE_180);
        } else if (rotation == 3) {
            transform(Transform.ROTATE_270);
        } else {
            transform(Transform.IDENTITY);
        }
    }

    public void transform(Transform transform) {
        this.transform = Optional.of(transform);
    }

    public void clear() {
        this.clear = true;
    }

    public void setFrontColorRed(float color) {
        validateColor(color);
        this.frontColorRed = Optional.of(color);
    }

    public void setFrontColorGreen(float color) {
        validateColor(color);
        this.frontColorGreen = Optional.of(color);
    }

    public void setFrontColorBlue(float color) {
        validateColor(color);
        this.frontColorBlue = Optional.of(color);
    }

    public void setBackColorRed(float color) {
        validateColor(color);
        this.backColorRed = Optional.of(color);
    }

    public void setBackColorGreen(float color) {
        validateColor(color);
        this.backColorGreen = Optional.of(color);
    }

    public void setBackColorBlue(float color) {
        validateColor(color);
        this.backColorBlue = Optional.of(color);
    }

    public Optional<String> getMessage() {
        return message;
    }

    public Optional<byte[]> getRGB565Pixels() {
        return pixelsRGB565;
    }

    public Optional<byte[]> getMonochromePixels() {
        return pixelsMonochrome;
    }

    public Optional<Transform> getTransform() {
        return transform;
    }

    public boolean shouldClear() {
        return clear;
    }

    public Optional<Float> getFrontColorRed() {
        return frontColorRed;
    }

    public Optional<Float> getFrontColorGreen() {
        return frontColorGreen;
    }

    public Optional<Float> getFrontColorBlue() {
        return frontColorBlue;
    }

    public Optional<Float> getBackColorRed() {
        return backColorRed;
    }

    public Optional<Float> getBackColorGreen() {
        return backColorGreen;
    }

    public Optional<Float> getBackColorBlue() {
        return backColorBlue;
    }

    private void assertBufferLength(byte[] buf, int expectedLen) {
        if (buf.length != expectedLen) {
            throw new IllegalArgumentException("Buffer lenght must be: " + expectedLen);
        }
    }

    private void validateColor(float color) {
        if (color < 0 || color > 1) {
            throw new IllegalArgumentException("Color value must be a floating point number between 0 and 1");
        }
    }
}
