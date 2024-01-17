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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.example.framegrabber.gstreamer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeInfo;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.PadProbeType;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Version;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleFramegrabber implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleFramegrabber.class);

    // private ScheduledExecutorService worker;
    // private ScheduledFuture<?> handle;
    private ExecutorService worker;
    private Future<?> handle;
    private static Pipeline pipeline;
    private static int cameraId = 0;
    // private byte[] buff = new byte[10];
    // private byte[] byteImage;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private ExampleFramegrabberOptions examplePublisherOptions;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating ExampleFramegrabber...");

        Gst.init(Version.BASELINE, "FrameGrabberExample", new String[] {});

        // start worker
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.examplePublisherOptions = new ExampleFramegrabberOptions(properties);

        doUpdate();

        logger.info("Activating ExampleFramegrabber... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating ExampleFramegrabber...");

        stopAcquisition();

        logger.info("Deactivating ExampleFramegrabber... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated ExampleFramegrabber...");

        this.examplePublisherOptions = new ExampleFramegrabberOptions(properties);

        // try to kick off a new job
        doUpdate();
        logger.info("Updated ExampleFramegrabber... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate() {
        stopAcquisition();
        startAcquisition();
    }

    private void stopAcquisition() {
        if (this.handle != null) {
            this.handle.cancel(true);
        }
        if (this.worker != null && !this.worker.isShutdown()) {
            try {
                // stop the timer
                this.worker.shutdown();
                this.worker.awaitTermination(this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (pipeline != null && pipeline.isPlaying()) {
            pipeline.stop();
            pipeline.close();
            pipeline = null;
        }
        // if (this.capture.isOpened()) {
        // // release the camera
        // this.capture.release();
        // }
    }

    private void startAcquisition() {
        // schedule a new worker based on the properties of the service
        boolean isEnabled = this.examplePublisherOptions.isEnabled();
        if (isEnabled) {

            // this.byteImage = new byte[640 * 480 * 3];

            // // gst-launch-1.0 v4l2src device=/dev/video0 io-mode=0 ! image/jpeg, format=mjpg, width=1920,
            // height=1080,
            // // framerate=30/1 ! jpegdec ! videoconvert ! appsink
            //
            // // String caps = "video/x-raw, width=640, height=480" + ", pixel-aspect-ratio=1/1, "
            // // + (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "format=BGRx" : "format=xRGB");
            // //
            // // pipeline = (Pipeline) Gst
            // // .parseLaunch("autovideosrc ! videoconvert ! videoscale ! " + caps + " ! appsink name=pippo");
            //
            // pipeline = (Pipeline) Gst.parseLaunch(
            // "v4l2src device=/dev/video0 io-mode=0 ! image/jpeg, format=mjpg, width=640, height=480, framerate=30/1 !"
            // + "jpegdec ! videoconvert ! appsink name=pippo");
            // // pipeline = (Pipeline) Gst.parseLaunch("videotestsrc ! " + "videoscale ! videoconvert ! "
            // // + "capsfilter caps=video/x-raw,width=640,height=480 ! appsink name=pippo");
            // AppSink as = (AppSink) pipeline.getElementByName("pippo");
            // // pipeline.getBus().connect((Bus.ERROR) ((source, code, message) -> {
            // // System.out.println(message);
            // // Gst.quit();
            // // }));
            // // pipeline.getBus().connect((Bus.EOS) (source) -> Gst.quit());
            // pipeline.play();

            /**
             * Set up a Caps string with the width, height and buffer format
             * required for reading and writing into the BufferedImage.
             */
            String caps = "video/x-raw, width=" + WIDTH + ", height=" + HEIGHT + ", pixel-aspect-ratio=1/1, "
                    + (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "format=BGRx" : "format=xRGB");

            /**
             * Use Gst.parseLaunch() to create a pipeline from a GStreamer string
             * definition. This method returns Pipeline when more than one element
             * is specified.
             *
             * The named Identity element can be acquired from the pipeline by name
             * and the probe attached to its sink pad.
             */
            // pipeline = (Pipeline) Gst.parseLaunch("autovideosrc ! videoconvert ! videoscale ! " + caps
            // + " ! identity name=identity ! videoconvert ! autovideosink");
            pipeline = (Pipeline) Gst.parseLaunch(
                    "v4l2src device=/dev/video0 io-mode=0 ! image/jpeg, format=mjpg, width=640, height=480, framerate=30/1 !"
                            + "jpegdec ! videoconvert ! identity name=pippo");
            Element identity = pipeline.getElementByName("pippo");
            identity.getStaticPad("sink").addProbe(PadProbeType.BUFFER, new Renderer(WIDTH, HEIGHT));

            pipeline.getBus().connect((Bus.ERROR) ((source, code, message) -> {
                System.out.println(message);
                Gst.quit();
            }));
            pipeline.getBus().connect((Bus.EOS) (source) -> Gst.quit());
            pipeline.play();

            // Runnable frameGrabber = new Runnable() {
            //
            // @Override
            // public void run() {
            // Sample sample = as.pullSample();
            // Buffer buffer = sample.getBuffer();
            // ByteBuffer bb = buffer.map(false);
            // bb.get(byteImage);
            // System.arraycopy(byteImage, 0, ExampleFramegrabber.this.buff, 0,
            // ExampleFramegrabber.this.buff.length);
            // logger.info("Grabbed frame: {}", ExampleFramegrabber.this.byteImage);
            // buffer.unmap();
            // sample.close();
            //
            // // AppSink as = new AppSink("fg");
            // //
            // // Bin bin = Gst.parseBinFromDescription("videotestsrc ! " + "videoscale ! videoconvert ! "
            // // + "capsfilter caps=video/x-raw,width=640,height=480 ! appsink name=pippo", true);
            // // // Bin bin = Gst.parseBinFromDescription("videotestsrc ! videoscale ! videoconvert ! appsink",
            // // // true);
            // // // gst-launch-1.0 v4l2src device=/dev/video0 io-mode=0 ! image/jpeg, format=mjpg, width=1920,
            // // // height=1080, framerate=30/1 ! jpegdec ! videoconvert ! appsink
            // // pipeline = new Pipeline();
            // // pipeline.addMany(bin, as);
            // // // Pipeline.linkMany(bin, as.getElement());
            // //
            // // // JFrame f = new JFrame("Camera Test");
            // // // f.add(vc);
            // // // vc.setPreferredSize(new Dimension(640, 480));
            // // // f.pack();
            // // // f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // // //
            // // pipeline.play();
            // // Sample sample = as.pullSample();
            // // Buffer buffer = sample.getBuffer();
            // // ByteBuffer bb = buffer.map(false);
            // // logger.info(bb.toString());
            // // f.setVisible(true);
            //
            // // long start = System.currentTimeMillis();
            // // // effectively grab and process a single frame
            // // grabFrame();
            // // long stop = System.currentTimeMillis();
            // // logger.info("Grabbed frame {} {} {}", ExampleFramegrabber.this.frame.cols(),
            // // ExampleFramegrabber.this.frame.rows(), ExampleFramegrabber.this.frame.channels());
            // // logger.info("Frame time " + (stop - start) + " ms");
            // // logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
            // // ExampleFramegrabber.this.frame.get(0, 0, byteImage);
            // // System.arraycopy(byteImage, 0, ExampleFramegrabber.this.buff, 0,
            // // ExampleFramegrabber.this.buff.length);
            // // logger.info("Grabbed frame: {}", ExampleFramegrabber.this.buff);
            // }
            // };
            //
            // this.worker = Executors.newSingleThreadScheduledExecutor();
            // this.handle = this.worker.submit(frameGrabber);
            // // this.handle = this.worker.scheduleAtFixedRate(frameGrabber, 0,
            // // this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
            // // this.handle = this.worker.scheduleWithFixedDelay(frameGrabber, 0,
            // // this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
        } else {
            // log the error
            logger.error("Impossible to open the camera connection...");
        }
        // } else {
        // this.stopAcquisition();
        // }
    }

    // private void grabFrame() {
    // // init everything
    // // Mat frame = new Mat();
    //
    // // check if the capture is open
    // if (this.capture.isOpened()) {
    // try {
    // // read the current frame
    // this.capture.read(this.frame);
    //
    // // if the frame is not empty, process it
    // // if (!this.frame.empty()) {
    // // // Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
    // // Imgproc.cvtColor(this.frame, this.frame, Imgproc.COLOR_BGR2RGB);
    // // }
    //
    // } catch (Exception e) {
    // // log the error
    // System.err.println("Exception during the image elaboration: " + e);
    // }
    // }
    //
    // // return frame;
    // }

    /**
     * A Pad.PROBE implementation that acquires the Buffer, reads it into the
     * data array of a BufferedImage, renders an animation on top, and writes
     * back into the Buffer.
     */
    static class Renderer implements Pad.PROBE {

        private final BufferedImage image;
        private final int[] data;
        // private final Point[] points;
        // private final Paint fill;
        private int[] buff;
        private byte[] byteImage = new byte[460800];

        private Renderer(int width, int height) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            data = ((DataBufferInt) (image.getRaster().getDataBuffer())).getData();
            // points = new Point[18];
            // for (int i = 0; i < points.length; i++) {
            // points[i] = new Point();
            // }
            // fill = new GradientPaint(0, 0, new Color(1.0f, 0.3f, 0.5f, 0.9f), 60, 20, new Color(0.3f, 1.0f, 0.7f,
            // 0.8f),
            // true);
        }

        @Override
        public PadProbeReturn probeCallback(Pad pad, PadProbeInfo info) {
            Buffer buffer = info.getBuffer();
            // if (buffer.isWritable()) {
            IntBuffer ib = buffer.map(true).asIntBuffer();
            buff = new int[10];
            ib.get(buff);
            // render();
            // ib.rewind();
            // ib.put(data);
            // ByteBuffer bb = buffer.map(false);
            // byteImage = new byte[bb.remaining() - 1];
            // bb.get(byteImage);
            // System.arraycopy(data, 0, this.buff, 0, this.buff.length);
            logger.info("Grabbed frame: {}", this.buff);
            buffer.unmap();
            // }
            return PadProbeReturn.OK;
        }

        // private void render() {
        // Graphics2D g2d = image.createGraphics();
        // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // for (Point point : points) {
        // point.tick();
        // }
        // GeneralPath path = new GeneralPath();
        // path.moveTo(points[0].x, points[0].y);
        // for (int i = 2; i < points.length; i += 2) {
        // path.quadTo(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y);
        // }
        // path.closePath();
        // path.transform(AffineTransform.getScaleInstance(image.getWidth(), image.getHeight()));
        // g2d.setPaint(fill);
        // g2d.fill(path);
        // g2d.setColor(Color.BLACK);
        // g2d.draw(path);
        // }

    }
    //
    // static class Point {
    //
    // private double x, y, dx, dy;
    //
    // private Point() {
    // this.x = Math.random();
    // this.y = Math.random();
    // this.dx = 0.02 * Math.random();
    // this.dy = 0.02 * Math.random();
    // }
    //
    // private void tick() {
    // x += dx;
    // y += dy;
    // if (x < 0 || x > 1) {
    // dx = -dx;
    // }
    // if (y < 0 || y > 1) {
    // dy = -dy;
    // }
    // }
    //
    // }

}
