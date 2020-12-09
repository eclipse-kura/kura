/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
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

public class FrameBuffer {

    private static final Logger s_logger = LoggerFactory.getLogger(FrameBuffer.class);

    private static final String SENSE_HAT_FB_NAME = "RPi-Sense FB";

    private static FrameBuffer frameBuffer = new FrameBuffer();

    private static File graphicsFolder = new File("/sys/class/graphics/");
    private static File FrameBufferFile = null;
    private static RandomAccessFile raf = null;

    private static int rotation = 0;

    private static Alphabet alphabet;

    private FrameBuffer() {
    }

    public static FrameBuffer getFrameBuffer(ComponentContext ctx) {

        rotation = 0;

        alphabet = new Alphabet(
                ctx.getBundleContext().getBundle().getResource("src/main/resources/sense_hat_text.pbm"));

        BufferedReader br = null;
        String currentLine;
        for (final File fbFolder : graphicsFolder.listFiles()) {
            if (fbFolder.getName().contains("fb")) {

                try {
                    br = new BufferedReader(new FileReader(fbFolder + "/name"));
                    currentLine = br.readLine();
                    if (null != currentLine && currentLine.equals(SENSE_HAT_FB_NAME)) {
                        String eventFolderPath = fbFolder.getAbsolutePath();
                        FrameBufferFile = new File("/dev/fb" + eventFolderPath.substring(eventFolderPath.length() - 1));
                        br.close();
                        break;
                    }
                } catch (IOException e) {
                    s_logger.error("Error in opening file.", e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            s_logger.error("Error in closing file.", e);
                        }
                    }
                }

            }
        }

        try {
            raf = new RandomAccessFile(FrameBufferFile, "rw");
        } catch (FileNotFoundException e) {
            s_logger.error("FrameBuffer not found!", e);
        }

        return frameBuffer;
    }

    public static void setRotation(int rotate) {
        rotation = rotate;
    }

    public int getRotation() {
        return rotation;
    }

    public void flipVertical(short[][][] image) {

        if (image.length != 8 || image[0].length != 8 || image[0][0].length != 3) {
            s_logger.error("Image is a 8x8 matrix of RGB pixels.");
            return;
        }

        int coordinates[] = new int[2];

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                coordinates[0] = x;
                coordinates[1] = y;
                if (rotation == 0 || rotation == 180) {
                    setPixel(rotate(flipH(coordinates)), image[x][y]);
                } else {
                    setPixel(rotate(flipV(coordinates)), image[x][y]);
                }
            }
        }

    }

    public void flipHorizontal(short[][][] image) {

        if (image.length != 8 || image[0].length != 8 || image[0][0].length != 3) {
            s_logger.error("Image is a 8x8 matrix of RGB pixels.");
            return;
        }

        int coordinates[] = new int[2];

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                coordinates[0] = x;
                coordinates[1] = y;
                if (rotation == 90 || rotation == 270) {
                    setPixel(rotate(flipH(coordinates)), image[x][y]);
                } else {
                    setPixel(rotate(flipV(coordinates)), image[x][y]);
                }
            }
        }

    }

    public void setPixels(short[][][] image) {

        if (image.length != 8 || image[0].length != 8 || image[0][0].length != 3) {
            s_logger.error("Image is a 8x8 matrix of RGB pixels.");
            return;
        }

        int coordinates[] = new int[2];

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                coordinates[0] = x;
                coordinates[1] = y;
                setPixel(rotate(coordinates), image[x][y]);
            }
        }

    }

    public void setPixel(int[] coordinates, short[] pixel) {

        int x = coordinates[0];
        int y = coordinates[1];
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            s_logger.error("Invalid pixel position.");
            return;
        }
        if (pixel.length != 3) {
            s_logger.error("Pixel has to be in rgb format.");
            return;
        }

        byte[] packedPixel = packPixel(pixel);
        try {
            raf.seek(2 * (x * 8 + y));
            raf.write(packedPixel[0]);
            raf.write(packedPixel[1]);
        } catch (IOException e) {
            s_logger.error("Error in writing on framebuffer.");
        }

    }

    public short[][][] getPixels() {

        short[][][] image = new short[8][8][3];
        int[] coordinates = new int[2];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                coordinates[0] = x;
                coordinates[1] = y;
                image[x][y] = getPixel(coordinates);
            }
        }

        return image;
    }

    public short[] getPixel(int[] coordinates) {

        int x = coordinates[0];
        int y = coordinates[1];
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            s_logger.error("Invalid pixel position.");
            return null;
        }

        byte[] pixel = new byte[2];
        try {
            raf.seek(2 * (x * 8 + y));
            raf.read(pixel);
        } catch (IOException e) {
            s_logger.error("Error in writing on framebuffer.");
        }

        return unpackPixel(pixel);

    }

    public void loadImage() {

    }

    public void getCharPixels() {

    }

    public void showMessage(String text, short[] textColor, short[] backColor) {

        short[][][] message = new short[(text.length() + 2) * 8][8][3];
        System.arraycopy(alphabet.getLetter(" "), 0, message, 0, 8);
        for (int i = 0; i < text.length(); i++) {
            if (!alphabet.isAvailable(String.valueOf(text.charAt(i)))) {
                s_logger.warn("Letter not available");
                clearFrameBuffer();
                return;
            }
            System.arraycopy(alphabet.getLetter(String.valueOf(text.charAt(i))), 0, message, (i + 1) * 8, 8);
        }
        System.arraycopy(alphabet.getLetter(" "), 0, message, message.length - 8, 8);

        short[][][] currentFrame = new short[8][8][3];
        for (int i = 0; i < message.length - 8; i++) {
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    currentFrame[x][y][0] = message[x + i][y][0] == 0 ? backColor[0] : textColor[0];
                    currentFrame[x][y][1] = message[x + i][y][1] == 0 ? backColor[1] : textColor[1];
                    currentFrame[x][y][2] = message[x + i][y][2] == 0 ? backColor[2] : textColor[2];
                }
            }

            frameBuffer.setPixels(currentFrame);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                s_logger.error(e.toString());
            }

        }

    }

    public void showLetter(String letter, short[] textColor, short[] backColor) {

        if (!alphabet.isAvailable(letter)) {
            s_logger.warn("Letter not available");
            clearFrameBuffer();
            return;
        }

        short[][][] letterPixel = new short[8][8][3];
        letterPixel = alphabet.getLetter(letter);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                letterPixel[x][y][0] = letterPixel[x][y][0] == 0 ? backColor[0] : textColor[0];
                letterPixel[x][y][1] = letterPixel[x][y][1] == 0 ? backColor[1] : textColor[1];
                letterPixel[x][y][2] = letterPixel[x][y][2] == 0 ? backColor[2] : textColor[2];
            }
        }
        frameBuffer.setPixels(letterPixel);

    }

    public void clearFrameBuffer() {

        try {
            raf.seek(0);
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    raf.write((char) 0x00);
                    raf.write((char) 0x00);
                }
            }
        } catch (IOException e) {
            s_logger.error("Error in writing on framebuffer", e);
        }
    }

    public static void closeFrameBuffer() {

        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                s_logger.error("Error in closing framebuffer.", e);
            }
        }
    }

    private byte[] packPixel(short[] pixel) {
        byte[] pixelByte = new byte[3];
        byte[] outPixel = new byte[2];

        pixelByte[0] = (byte) (pixel[0] >> 3 & 0x1F);
        pixelByte[1] = (byte) (pixel[1] >> 2 & 0x3F);
        pixelByte[2] = (byte) (pixel[2] >> 3 & 0x1F);

        outPixel[0] = (byte) ((pixelByte[1] << 5) + pixelByte[0]);
        outPixel[1] = (byte) ((pixelByte[2] << 3) + (pixelByte[1] >> 3));

        return outPixel;
    }

    private short[] unpackPixel(byte[] pixel) {

        short[] unpackedPixel = new short[3];
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(pixel[0]);
        bb.put(pixel[1]);
        short shortPixel = bb.getShort(0);

        unpackedPixel[2] = (short) ((shortPixel & 0xF800) >> 8);
        unpackedPixel[1] = (short) ((shortPixel & 0x07E0) >> 3);
        unpackedPixel[0] = (short) ((shortPixel & 0x001F) << 3);

        return unpackedPixel;

    }

    private int[] rotate(int[] coordinates) {

        int[] rotated = new int[2];

        if (rotation == 0) {
            rotated = coordinates;
        } else if (rotation == 90) {
            rotated = transpose(coordinates);
            rotated[0] = 8 - 1 - rotated[0];
        } else if (rotation == 180) {
            rotated[0] = 8 - 1 - coordinates[0];
            rotated[1] = 8 - 1 - coordinates[1];
        } else if (rotation == 270) {
            rotated = transpose(coordinates);
            rotated[1] = 8 - 1 - rotated[1];
        } else {
            s_logger.warn("Incorrect rotation value. Only 0, 90, 180, 270 values are allowed.");
        }

        return rotated;
    }

    private int[] transpose(int[] coordinates) {

        int[] transposed = new int[2];
        transposed[0] = coordinates[1];
        transposed[1] = coordinates[0];

        return transposed;

    }

    private int[] flipV(int[] coordinates) {

        int[] flipped = new int[2];
        flipped[0] = coordinates[0];
        flipped[1] = 8 - 1 - coordinates[1];

        return flipped;
    }

    private int[] flipH(int[] coordinates) {

        int[] flipped = new int[2];
        flipped[0] = 8 - 1 - coordinates[0];
        flipped[1] = coordinates[1];

        return flipped;
    }
}
