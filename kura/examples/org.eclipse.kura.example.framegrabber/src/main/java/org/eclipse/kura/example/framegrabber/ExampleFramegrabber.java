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
package org.eclipse.kura.example.framegrabber;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleFramegrabber implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleFramegrabber.class);

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;
    private VideoCapture capture;
    private static int cameraId = 0;
    private Mat frame;
    private byte[] buff = new byte[10];
    private byte[] byteImage;

    private ExampleFramegrabberOptions examplePublisherOptions;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating ExampleFramegrabber...");

        nu.pattern.OpenCV.loadLocally();
        this.capture = new VideoCapture();

        // start worker
        // this.worker = Executors.newSingleThreadScheduledExecutor();
        this.examplePublisherOptions = new ExampleFramegrabberOptions(properties);
        this.frame = new Mat();

        doUpdate();

        logger.info("Activating ExamplePublisher... Done.");
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

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
    }

    private void startAcquisition() {
        // schedule a new worker based on the properties of the service
        boolean isEnabled = this.examplePublisherOptions.isEnabled();
        if (isEnabled) {
            // start the video capture
            // this.capture.open(cameraId);
            // boolean wset = capture.set(Videoio.CAP_PROP_FRAME_WIDTH, (double)
            // this.examplePublisherOptions.getWidth());
            // boolean hset = capture.set(Videoio.CAP_PROP_FRAME_HEIGHT,
            // (double) this.examplePublisherOptions.getHeight());
            // boolean fset = capture.set(Videoio.CAP_PROP_FPS,
            // (double) this.examplePublisherOptions.getNativeFrameRate());
            //
            // logger.info("Width configuration accepted? {}", wset);
            // logger.info("Height configuration accepted? {}", hset);
            // logger.info("FPS configuration accepted? {}", fset);

            logger.info("OpenCV infos: {}", Core.getBuildInformation());
            // // It seems that opencv on my RPi hasn't the support for gstreamer.
            // // With GStreamer backend:
            // StringBuilder gstreamerPipeline = new StringBuilder();
            // // gstreamerPipeline.append("v4l2src device=/dev/video").append(cameraId)
            // // .append(" io-mode=0 ! image/jpeg, format=mjpg, width=")
            // // .append(this.examplePublisherOptions.getWidth()).append(", height=")
            // // .append(this.examplePublisherOptions.getHeight()).append(", framerate=")
            // // .append(this.examplePublisherOptions.getNativeFrameRate())
            // // .append("/1 ! jpegdec ! video/x-raw ! videoconvert ! appsink");
            // gstreamerPipeline.append("videotestsrc ! videoconvert ! appsink");
            // this.capture.open(gstreamerPipeline.toString(), Videoio.CAP_GSTREAMER);

            // With Video4Linux backend:
            // https://stackoverflow.com/questions/61046673/opencv-how-to-read-autodetected-api-and-format-of-videocapture
            // https://stackoverflow.com/questions/36593145/java-setting-videocapture-setcap-prop-fourcc-codec-value
            // Set the encoder (fourcc) BEFORE setting resolution and framerate!!!
            MatOfInt config = new MatOfInt(Videoio.CAP_PROP_FOURCC, VideoWriter.fourcc('M', 'J', 'P', 'G'),
                    Videoio.CAP_PROP_FRAME_WIDTH, this.examplePublisherOptions.getWidth(),
                    Videoio.CAP_PROP_FRAME_HEIGHT, this.examplePublisherOptions.getHeight(), Videoio.CAP_PROP_FPS,
                    this.examplePublisherOptions.getNativeFrameRate());
            this.capture.open(cameraId, Videoio.CAP_V4L2, config);

            logger.info("Start acquisition: {} {} {}", capture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                    capture.get(Videoio.CAP_PROP_FRAME_HEIGHT), capture.get(Videoio.CAP_PROP_FPS));

            this.byteImage = new byte[(int) (ExampleFramegrabber.this.examplePublisherOptions.getWidth()
                    * ExampleFramegrabber.this.examplePublisherOptions.getHeight() * 3)];

            // is the video stream available?
            if (this.capture.isOpened()) {

                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run() {
                        long start = System.currentTimeMillis();
                        // effectively grab and process a single frame
                        grabFrame();
                        long stop = System.currentTimeMillis();
                        logger.info("Grabbed frame {} {} {}", ExampleFramegrabber.this.frame.cols(),
                                ExampleFramegrabber.this.frame.rows(), ExampleFramegrabber.this.frame.channels());
                        logger.info("Frame time " + (stop - start) + " ms");
                        logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
                        ExampleFramegrabber.this.frame.get(0, 0, byteImage);
                        System.arraycopy(byteImage, 0, ExampleFramegrabber.this.buff, 0,
                                ExampleFramegrabber.this.buff.length);
                        logger.info("Grabbed frame: {}", ExampleFramegrabber.this.buff);
                    }
                };

                this.worker = Executors.newSingleThreadScheduledExecutor();
                this.handle = this.worker.scheduleAtFixedRate(frameGrabber, 0,
                        this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
                // this.handle = this.worker.scheduleWithFixedDelay(frameGrabber, 0,
                // this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
            } else {
                // log the error
                logger.error("Impossible to open the camera connection...");
            }
        } else {
            this.stopAcquisition();
        }
    }

    private void grabFrame() {
        // init everything
        // Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(this.frame);

                // if the frame is not empty, process it
                // if (!this.frame.empty()) {
                // // Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                // Imgproc.cvtColor(this.frame, this.frame, Imgproc.COLOR_BGR2RGB);
                // }

            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        // return frame;
    }

}
