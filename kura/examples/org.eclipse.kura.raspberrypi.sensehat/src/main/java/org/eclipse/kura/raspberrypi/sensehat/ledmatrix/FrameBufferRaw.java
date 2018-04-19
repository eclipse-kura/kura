/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.raspberrypi.sensehat.ledmatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameBufferRaw {

    private static final Logger s_logger = LoggerFactory.getLogger(FrameBufferRaw.class);

    private static final String SENSE_HAT_FB_NAME = "RPi-Sense FB";

    public static final int MONOCHROME_BUFFER_SIZE = 8 * 8;
    public static final int RGB565_BUFFER_SIZE = MONOCHROME_BUFFER_SIZE * 2;

    private static File graphicsFolder = new File("/sys/class/graphics/");
    private static final byte[] ZEROES = new byte[RGB565_BUFFER_SIZE];

    private static FrameBufferRaw INSTANCE;

    private File frameBufferFile;
    private RandomAccessFile raf;

    private Transform transform;

    private AlphabetRaw alphabet;

    private ByteBuffer frontColor = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    private ByteBuffer backColor = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);

    private final byte[] tempBuffer = new byte[RGB565_BUFFER_SIZE];

    private FrameBufferRaw(ComponentContext ctx) throws IOException {
        this.setTransform(Transform.IDENTITY);

        this.alphabet = new AlphabetRaw(
                ctx.getBundleContext().getBundle().getResource("src/main/resources/sense_hat_text.pbm"));

        String currentLine;
        for (final File fbFolder : graphicsFolder.listFiles()) {
            if (fbFolder.getName().contains("fb")) {
                try (final BufferedReader br = new BufferedReader(new FileReader(fbFolder + "/name"))) {
                    currentLine = br.readLine();
                    if (null != currentLine && currentLine.equals(SENSE_HAT_FB_NAME)) {
                        String eventFolderPath = fbFolder.getAbsolutePath();
                        frameBufferFile = new File("/dev/fb" + eventFolderPath.substring(eventFolderPath.length() - 1));
                        br.close();
                        break;
                    }
                }
            }
        }

        try {
            raf = new RandomAccessFile(frameBufferFile, "rw");
        } catch (FileNotFoundException e) {
            s_logger.error("FrameBuffer not found!", e);
        }
    }

    public static FrameBufferRaw getFrameBuffer(ComponentContext ctx) throws IOException {

        if (INSTANCE == null) {
            INSTANCE = new FrameBufferRaw(ctx);
        }
        return INSTANCE;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public void setPixelsRGB565(byte[] data) throws IOException {

        if (data.length != RGB565_BUFFER_SIZE) {
            throw new IllegalArgumentException("Data length must be: " + RGB565_BUFFER_SIZE);
        }

        if (transform == Transform.IDENTITY) {
            writeFramebuffer(data);
        } else {
            transformAndMove(data, tempBuffer);
            writeFramebuffer(tempBuffer);
        }

    }

    public void setPixelsMonochrome(byte[] data) throws IOException {
        if (data.length != MONOCHROME_BUFFER_SIZE) {
            throw new IllegalArgumentException("Data length must be: " + MONOCHROME_BUFFER_SIZE);
        }

        render(data, this.frontColor.array(), this.backColor.array(), this.tempBuffer);
        writeFramebuffer(this.tempBuffer);
    }

    public void setFrontColor(byte[] frontColor) {
        if (frontColor == null || frontColor.length != 2) {
            throw new IllegalArgumentException("Illegal color buffer");
        }

        this.frontColor.position(0);
        this.frontColor.put(frontColor);
    }

    public void setBackColor(byte[] backColor) {
        if (backColor == null || backColor.length != 2) {
            throw new IllegalArgumentException("Illegal color buffer");
        }

        this.backColor.position(0);
        this.backColor.put(backColor);
    }

    private void setBlue(final ByteBuffer dst, final float blueFloat) {
        short value = dst.getShort(0);

        final short mask = ((1 << 5) - 1);
        final short red = (short) (blueFloat * mask);
        value &= ~mask;
        value |= red & mask;

        dst.putShort(0, value);
    }

    private void setGreen(final ByteBuffer dst, final float greenFloat) {
        short value = dst.getShort(0);

        final short max = ((1 << 6) - 1);
        final short mask = max << 5;
        final short green = (short) (greenFloat * max);

        value &= ~mask;
        value |= (green << 5) & mask;

        dst.putShort(0, value);
    }

    private void setRed(final ByteBuffer dst, final float redFloat) {
        short value = dst.getShort(0);

        final int max = ((1 << 5) - 1);
        final int mask = max << 11;
        final int red = (int) (redFloat * max);

        value &= (short) (~mask & 0xffff);
        value |= (short) (((red << 11) & mask) & 0xffff);

        dst.putShort(0, value);
    }

    public void setFrontColorRed(float color) {
        setRed(frontColor, color);
    }

    public void setFrontColorGreen(float color) {
        setGreen(frontColor, color);
    }

    public void setFrontColorBlue(float color) {
        setBlue(frontColor, color);
    }

    public void setBackColorRed(float color) {
        setRed(backColor, color);
    }

    public void setBackColorGreen(float color) {
        setGreen(backColor, color);
    }

    public void setBackColorBlue(float color) {
        setBlue(backColor, color);
    }

    public void showMessage(String text) throws IOException {
        for (int i = 0; i < text.length(); i++) {
            try {
                showLetter(text.charAt(i));
                Thread.sleep(500);
                clearFrameBuffer();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void showMessage(String text, byte[] textColor, byte[] backColor) throws IOException {

        setFrontColor(textColor);
        setBackColor(backColor);

        showMessage(text);
    }

    public void showLetter(char letter) throws IOException {

        if (!alphabet.isAvailable(letter)) {
            clearFrameBuffer();
            s_logger.warn("Letter not available");
            return;
        }

        byte[] letterData = alphabet.getLetter(letter);
        render(letterData, this.frontColor.array(), this.backColor.array(), this.tempBuffer);
        writeFramebuffer(this.tempBuffer);
    }

    public void showLetter(char letter, byte[] textColor, byte[] backColor) throws IOException {

        setFrontColor(textColor);
        setBackColor(backColor);

        this.showLetter(letter);
    }

    public void clearFrameBuffer() throws IOException {
        writeFramebuffer(ZEROES);
    }

    public void closeFrameBuffer() throws IOException {

        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    private void render(byte[] img, byte[] frontColor, byte[] backColor, byte[] dst) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                final int srcOffset = Transform.IDENTITY.apply(x, y);
                final int dstOffset = this.transform.apply(x, y) * 2;
                System.arraycopy(img[srcOffset] != 0 ? frontColor : backColor, 0, dst, dstOffset, 2);
            }
        }
    }

    private void transformAndMove(byte[] src, byte[] dst) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                final int srcOffset = Transform.IDENTITY.apply(x, y) * 2;
                final int dstOffset = this.transform.apply(x, y) * 2;
                System.arraycopy(src, srcOffset, dst, dstOffset, 2);
            }
        }
    }

    private void writeFramebuffer(byte[] data) throws IOException {
        raf.seek(0);
        raf.write(data);
    }
}
