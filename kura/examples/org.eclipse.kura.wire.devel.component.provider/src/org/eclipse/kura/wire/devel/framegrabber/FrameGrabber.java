package org.eclipse.kura.wire.devel.framegrabber;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameGrabber implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(FrameGrabber.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;
    private VideoCapture capture;
    private FrameGrabberOptions options;
    private static int cameraId = 0;
    private byte[] buff = new byte[10];
    // private byte[] byteImage;
    private Mat frame;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (wireHelperService == this.wireHelperService) {
            this.wireHelperService = null;
        }
    }

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        logger.debug("Activating FrameGrabber...");

        nu.pattern.OpenCV.loadLocally();
        this.capture = new VideoCapture();

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) ctx.getServiceReference());
        this.options = new FrameGrabberOptions(properties);
        this.frame = new Mat();

        doUpdate();

        logger.debug("Activating FrameGrabber... Done");
    }

    protected void deactivate() {
        logger.debug("Dectivating FrameGrabber...");

        stopAcquisition();

        logger.debug("Dectivating FrameGrabber... Done");
    }

    protected void updated(final Map<String, Object> properties) {
        logger.debug("Updating FrameGrabber...");

        this.options = new FrameGrabberOptions(properties);

        doUpdate();

        logger.debug("Updating FrameGrabber... Done");
    }

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
                this.worker.awaitTermination(this.options.getFramePeriod(), TimeUnit.MILLISECONDS);
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
        boolean isEnabled = this.options.isEnabled();
        String mode = this.options.getAcquisitionMode();
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
                    Videoio.CAP_PROP_FRAME_WIDTH, this.options.getWidth(), Videoio.CAP_PROP_FRAME_HEIGHT,
                    this.options.getHeight(), Videoio.CAP_PROP_FPS, this.options.getNativeFrameRate());
            this.capture.open(cameraId, Videoio.CAP_V4L2, config);

            if ("listen".equals(mode)) {
                logger.info("Start acquisition: {} {} {}", capture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                        capture.get(Videoio.CAP_PROP_FRAME_HEIGHT), capture.get(Videoio.CAP_PROP_FPS));

                byte[] byteImage = new byte[(int) (this.options.getWidth() * this.options.getHeight() * 3)];

                // is the video stream available?
                if (this.capture.isOpened()) {

                    Runnable frameGrabber = new Runnable() {

                        @Override
                        public void run() {
                            long start = System.currentTimeMillis();
                            // effectively grab and process a single frame
                            grabFrame();
                            long stop = System.currentTimeMillis();
                            logger.info("Grabbed frame {} {} {}", FrameGrabber.this.frame.cols(),
                                    FrameGrabber.this.frame.rows(), FrameGrabber.this.frame.channels());
                            logger.info("Frame time " + (stop - start) + " ms");
                            logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
                            FrameGrabber.this.frame.get(0, 0, byteImage);
                            System.arraycopy(byteImage, 0, FrameGrabber.this.buff, 0, FrameGrabber.this.buff.length);
                            logger.info("Grabbed frame: {}", FrameGrabber.this.buff);
                            final TypedValue<byte[]> message = TypedValues.newByteArrayValue(byteImage);
                            final WireRecord messageWireRecord = new WireRecord(
                                    Collections.singletonMap("INPUT0", message));
                            wireSupport.emit(Collections.singletonList(messageWireRecord));
                        }
                    };

                    this.worker = Executors.newSingleThreadScheduledExecutor();
                    this.handle = this.worker.scheduleAtFixedRate(frameGrabber, 0, this.options.getFramePeriod(),
                            TimeUnit.MILLISECONDS);
                    // this.handle = this.worker.scheduleWithFixedDelay(frameGrabber, 0,
                    // this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
                } else {
                    // log the error
                    logger.error("Impossible to open the camera connection...");
                }
            }
        } else {
            this.stopAcquisition();
        }
    }

    // private void doUpdate(final FrameGrabberOptions properties) {
    // stopAcquisition();
    // if (this.capture.isOpened()) {
    // this.capture.release();
    // }
    //
    // this.options = properties;
    // this.framePeriod = (int) ((double) (1.0 / this.options.getFrameRate()) * 1000);
    // if (this.options.isEnabled()) {
    // this.timer = Executors.newSingleThreadScheduledExecutor();
    // this.capture.open(0);
    // boolean wset = capture.set(3, 1920);
    // boolean hset = capture.set(4, 1080);
    // logger.info(Double.toString(this.capture.get(5)));
    // logger.info("Set width " + wset + " Set height " + hset);
    // this.byteImage = new byte[(int) (1920 * 1080 * 3)];
    // Runnable frameGrabber = new Runnable() {
    //
    // @Override
    // public void run() {
    // // byte[] buff = new byte[10];
    // long start = System.currentTimeMillis();
    // Mat frame = grabFrame();
    // long stop = System.currentTimeMillis();
    // // byte[] byteImage = new byte[(int) (frame.total() * frame.channels())];
    // frame.get(0, 0, byteImage);
    // // System.arraycopy(byteImage, 0, buff, 0, buff.length);
    // // final TypedValue<String> message = TypedValues.newStringValue(frame.dump());
    // final TypedValue<byte[]> message = TypedValues.newByteArrayValue(byteImage);
    // // final TypedValue<byte[]> message = TypedValues.newByteArrayValue(buff);
    // final WireRecord messageWireRecord = new WireRecord(Collections.singletonMap("FRAME", message));
    // wireSupport.emit(Collections.singletonList(messageWireRecord));
    // // long stop = System.currentTimeMillis();
    // logger.info("Start: " + start + " Stop: " + stop);
    // logger.info("Frame time " + (stop - start) + " ms");
    // logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
    // }
    // };
    // // This is for stream acquisition.
    // startAcquisition(frameGrabber);
    // }
    // }

    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    // static void emit(final WireSupport wireSupport) {
    //
    // // final TypedValue<Long> timestamp = TypedValues.newLongValue(System.currentTimeMillis());
    // //
    // // final WireRecord timerWireRecord = new WireRecord(Collections.singletonMap("TIMER", timestamp));
    // //
    // // wireSupport.emit(Collections.singletonList(timerWireRecord));
    // }

    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        boolean isEnabled = this.options.isEnabled();
        String mode = this.options.getAcquisitionMode();
        if (isEnabled && "polling".equals(mode)) {
            logger.info("Acquire: {} {} {}", capture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                    capture.get(Videoio.CAP_PROP_FRAME_HEIGHT), capture.get(Videoio.CAP_PROP_FPS));

            byte[] byteImage = new byte[(int) (this.options.getWidth() * this.options.getHeight() * 3)];

            // is the video stream available?
            if (this.capture.isOpened()) {

                // Runnable frameGrabber = new Runnable() {

                // @Override
                // public void run() {
                long start = System.currentTimeMillis();
                // effectively grab and process a single frame
                grabFrame();
                long stop = System.currentTimeMillis();
                logger.info("Grabbed frame {} {} {}", FrameGrabber.this.frame.cols(), FrameGrabber.this.frame.rows(),
                        FrameGrabber.this.frame.channels());
                logger.info("Frame time " + (stop - start) + " ms");
                logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
                FrameGrabber.this.frame.get(0, 0, byteImage);
                System.arraycopy(byteImage, 0, FrameGrabber.this.buff, 0, FrameGrabber.this.buff.length);
                logger.info("Grabbed frame: {}", FrameGrabber.this.buff);
                final TypedValue<byte[]> message = TypedValues.newByteArrayValue(byteImage);
                final WireRecord messageWireRecord = new WireRecord(Collections.singletonMap("INPUT0", message));
                wireSupport.emit(Collections.singletonList(messageWireRecord));
                // }
                // };

                // this.worker = Executors.newSingleThreadScheduledExecutor();
                // this.handle = this.worker.scheduleAtFixedRate(frameGrabber, 0, this.options.getFramePeriod(),
                // TimeUnit.MILLISECONDS);
                // this.handle = this.worker.scheduleWithFixedDelay(frameGrabber, 0,
                // this.examplePublisherOptions.getFramePeriod(), TimeUnit.MILLISECONDS);
            } else {
                // log the error
                logger.error("Impossible to open the camera connection...");
            }
        }
        // if (this.options.isEnabled()) {
        // if (this.capture.isOpened()) {
        // long start = System.currentTimeMillis();
        // Mat frame = grabFrame();
        // byte[] byteImage = new byte[(int) (frame.total() * frame.channels())];
        // frame.get(0, 0, byteImage);
        // // final TypedValue<String> message = TypedValues.newStringValue(frame.dump());
        // final TypedValue<byte[]> message = TypedValues.newByteArrayValue(byteImage);
        // final WireRecord messageWireRecord = new WireRecord(Collections.singletonMap("FRAME", message));
        // wireSupport.emit(Collections.singletonList(messageWireRecord));
        // long stop = System.currentTimeMillis();
        // logger.info("Start: " + start + " Stop: " + stop);
        // logger.info("Frame time " + (stop - start) + " ms");
        // logger.info("Frame rate " + ((double) (1.0 / (stop - start)) * 1000.0) + " frame/s");
        // }
        //
        // } else {
        // logger.info("Acquisition disabled.");
        // // final TypedValue<String> message = TypedValues.newStringValue("Acquisition disabled.");
        // // final WireRecord messageWireRecord = new WireRecord(Collections.singletonMap("MSG", message));
        // // wireSupport.emit(Collections.singletonList(messageWireRecord));
        // }
    }

    // private void startAcquisition(Runnable frameGrabber) {
    // logger.info("Start acquisition at " + this.framePeriod + " ms");
    // this.timer.scheduleWithFixedDelay(frameGrabber, 0, this.framePeriod, TimeUnit.MILLISECONDS);
    // }
    //
    // private void stopAcquisition() {
    // if (this.timer != null && !this.timer.isShutdown()) {
    // try {
    // this.timer.shutdown();
    // this.timer.awaitTermination(this.framePeriod, TimeUnit.MILLISECONDS);
    // } catch (InterruptedException e) {
    // logger.error("Exception in stopping the frame capture, trying to release the camera now... " + e);
    // }
    // }
    // }

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
