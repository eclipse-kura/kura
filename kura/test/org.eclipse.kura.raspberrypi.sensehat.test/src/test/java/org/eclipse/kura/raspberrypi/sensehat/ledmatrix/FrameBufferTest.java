/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.kura.raspberrypi.sensehat.ledmatrix;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.URL;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class FrameBufferTest {

    @Test
    public void testFlipVertical() throws Exception {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.flipVertical(Images.ARROW_UP);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.ARROW_DOWN, pixels);
        FrameBuffer.closeFrameBuffer();
    }

    @Test
    public void testFlipHorizontal() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        FrameBuffer.setRotation(270);
        frameBuffer.flipHorizontal(Images.ARROW_RIGHT);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.ARROW_DOWN, pixels);
        FrameBuffer.closeFrameBuffer();

    }

    @Test
    public void testSetPixels() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.setPixels(Images.TRIANGLE);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.TRIANGLE, pixels);
        FrameBuffer.closeFrameBuffer();
    }

    @Test
    public void testSetPixel() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.clearFrameBuffer();
        frameBuffer.setPixel(new int[] { 4, 3 }, Images.B);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.B, pixels[4][3]);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i != 4 && j != 3) {
                    assertArrayEquals(Images.W, pixels[i][j]);
                }
            }
        }
    }

    @Test
    public void testGetPixels() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.setPixels(Images.TRIANGLE);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.TRIANGLE, pixels);
        FrameBuffer.closeFrameBuffer();
    }

    @Test
    public void testGetPixel() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.setPixels(Images.TRIANGLE);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i < j) {
                    assertArrayEquals(Images.W, frameBuffer.getPixel(new int[] { i, j }));
                } else {
                    assertArrayEquals(Images.B, frameBuffer.getPixel(new int[] { i, j }));
                }
            }
        }
    }

    @Test
    public void testShowMessage() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        frameBuffer.setPixels(Images.TRIANGLE);
        frameBuffer.showMessage("test", Images.B, Images.W);
        // showMessage() will show blank display at the end of the message
        assertArrayEquals(Images.BLANK, frameBuffer.getPixels());
    }

    @Test
    public void testShowLetter() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.showLetter("T", Images.B, Images.W);
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.LETTER_T, pixels);
        FrameBuffer.closeFrameBuffer();
    }

    @Test
    public void testClearFrameBuffer() {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        frameBuffer.setPixels(Images.TRIANGLE);
        frameBuffer.clearFrameBuffer();
        short[][][] pixels = frameBuffer.getPixels();
        assertArrayEquals(Images.BLANK, pixels);
        FrameBuffer.closeFrameBuffer();
    }

    @Test
    public void testCloseFrameBuffer() throws Exception {
        configureVirtualFrameBuffer();
        FrameBuffer frameBuffer = FrameBuffer.getFrameBuffer(createMockedContext());
        assertNotNull(frameBuffer);
        Field field = FrameBuffer.class.getDeclaredField("raf");
        field.setAccessible(true);
        RandomAccessFile raf = (RandomAccessFile) field.get(null);
        assertNotNull(raf);
        FrameBuffer.closeFrameBuffer();
        raf = (RandomAccessFile) field.get(null);
        boolean closed = (boolean) TestUtil.getFieldValue(raf, "closed");
        assertTrue("FrameBuffer not closed!", closed);
    }

    private ComponentContext createMockedContext() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getResource(anyString())).thenAnswer(new Answer<URL>() {

            @Override
            public URL answer(InvocationOnMock invocation) throws Throwable {
                return new URL("file:./src/test/resources/test-sensehat_text.pbm");
            }
        });
        BundleContext bundleCtx = mock(BundleContext.class);
        when(bundleCtx.getBundle()).thenReturn(bundle);
        ComponentContext ctx = mock(ComponentContext.class);
        when(ctx.getBundleContext()).thenReturn(bundleCtx);
        return ctx;
    }

    private void configureVirtualFrameBuffer() {
        try {
            Field field = FrameBuffer.class.getDeclaredField("FrameBufferFile");
            field.setAccessible(true);
            field.set(null, new File("target/fb-test.output"));
            Field graphicsFolderField = FrameBuffer.class.getDeclaredField("graphicsFolder");
            graphicsFolderField.setAccessible(true);
            graphicsFolderField.set(null, new File("src/test/resources/"));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
