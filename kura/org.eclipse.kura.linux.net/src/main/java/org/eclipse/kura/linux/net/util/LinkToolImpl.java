package org.eclipse.kura.linux.net.util;

public abstract class LinkToolImpl {

    private String interfaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s
    private String duplex = null;
    private int signal = 0;

    public String getIfaceName() {
        return interfaceName;
    }

    public void setIfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public boolean isLinkDetected() {
        return linkDetected;
    }

    public void setLinkDetected(boolean linkDetected) {
        this.linkDetected = linkDetected;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getDuplex() {
        return duplex;
    }

    public void setDuplex(String duplex) {
        this.duplex = duplex;
    }

    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

}
