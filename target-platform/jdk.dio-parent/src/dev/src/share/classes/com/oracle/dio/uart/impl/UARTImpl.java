/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.dio.uart.impl;

import java.util.StringTokenizer;

import jdk.dio.ClosedDeviceException;
import jdk.dio.DeviceNotFoundException;

import jdk.dio.InputRoundListener;
import jdk.dio.OutputRoundListener;
import jdk.dio.RoundCompletionEvent;

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceConfig;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnsupportedAccessModeException;

import java.io.*;
import java.nio.ByteBuffer;

import jdk.dio.uart.*;
import jdk.dio.modem.*;

import com.oracle.dio.power.impl.PowerManagedBase;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.Configuration;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.utils.Logging;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import com.oracle.dio.utils.PrivilegeController;
import com.oracle.dio.utils.PrivilegedAction;

//import java.security.AccessController;
import jdk.dio.DevicePermission;

class UARTImpl extends PowerManagedBase<UART> implements ModemUART,
        ModemSignalDispatcher.SerialSignalListener {
    private boolean isWriting;

    private Object synchReadLock = new Object();

    private ByteBuffer writeBuffers[] = new ByteBuffer[2];
    private int writeBuffersPositions[] = new int[2];

    private ByteBuffer readBuffers[] = new ByteBuffer[2];
    private int readBuffersPositions[] = new int[2];

    private int readBufferIdx = 0;
    private int writeBufferIdx = 0;

    private InputRoundListener<UART, ByteBuffer> inRoundListener;
    private OutputRoundListener<UART, ByteBuffer> outRoundListener;

    private Hashtable<Integer, UARTEventListener> eventListeners;

    private int receiveTriggerLevel;
    private int inputTimeout;
    private Timer receiveTimer;

    private ModemSignalListener modemListener;
    private InputRoundListener<UART, ByteBuffer> iRL;

    UARTImpl(DeviceDescriptor<UART> dscr, int mode)
                                throws DeviceNotFoundException, InvalidDeviceConfigException, UnsupportedAccessModeException{
        super(dscr, mode);

        String deviceName;
        byte[] devName; // UTF-8 device name

        if( mode != DeviceManager.EXCLUSIVE){
            throw new UnsupportedAccessModeException();
        }

        UARTConfig cfg = dscr.getConfiguration();

        deviceName = getSecurityName();

        if (deviceName == null){
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.UART_CANT_GET_PORT_NAME)
            );
        }

        //UARTPermission permission = new UARTPermission(deviceName);
        //AccessController.getContext().checkPermission(permission);

        try{
            devName = deviceName.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.UART_UTF8_UNCONVERTIBLE_DEVNAME)
            );
        }

        inputTimeout = Configuration.getNonNegativeIntProperty("jdk.dio.uart.inputTimeout", 2000);

        openUARTByConfig0(devName, cfg.getBaudRate(), cfg.getStopBits(), cfg.getFlowControlMode(),
                                   cfg.getDataBits(), cfg.getParity(), mode == DeviceManager.EXCLUSIVE);

        isWriting = false;
        eventListeners = new Hashtable<Integer, UARTEventListener>();
        initPowerManagement();
    }

    private InputRoundListener getLocalInputRoundListener(){
        if(iRL == null){
            iRL = new InputRoundListener<UART, ByteBuffer>() {
                @Override
                public void inputRoundCompleted(RoundCompletionEvent<UART, ByteBuffer> event) {
                    synchronized(synchReadLock){
                        synchReadLock.notifyAll();
                    }
                }

                @Override
                public void failed(Throwable ex, UART arg1) {
                    synchronized(synchReadLock){
                        synchReadLock.notifyAll();
                    }
                }
            };
        }
        return iRL;
    }

    private void startReceiveTimer(){

        if(receiveTimer != null){
            receiveTimer.cancel();
        }
        receiveTimer = new Timer();
        receiveTimer.schedule(new TimerTask(){
                                            @Override
                                            public void run() {
                                            try{
                                                UARTEventHandler.getInstance().sendTimeoutEvent(getHandle().getNativeHandle());
                                            }catch(Exception e){
                                                //do nothing
                                            }
                                        }
                                   },inputTimeout);
    }

    private void stopReceiveTimer(){
        if(receiveTimer != null){
            receiveTimer.cancel();
        }
    }

    private boolean isAlphaNumerical(char ch) {
        if ((('a' <= ch && ch <= 'z') ||
             ('A' <= ch && ch <= 'Z') ||
             ('0' <= ch && ch <= '9'))) {
            return true;
        }
        return false;
    }

    private String getSecurityName(){
        UARTConfig cfg = dscr.getConfiguration();
        int devNum = cfg.getControllerNumber();
        String securityName = null;

        if (null != cfg.getControllerName()) {
            securityName = cfg.getControllerName();
            for (int i = 0; i < securityName.length(); i++) {
                if(!isAlphaNumerical(securityName.charAt(i))) {
                    // for security reason to prohibit usage of "../"
                    // and to align with MEEP spec
                    Logging.reportError("Unacceptable device name:", securityName);
                    return null;
                }
            }

        } else {
            if (devNum == DeviceConfig.DEFAULT) {
                devNum = 0;
            }
            // first port in list is DEFAULT port
            String ports = Configuration.getProperty("microedition.commports");
            if (ports == null) {
                ports = Configuration.getProperty("jdk.dio.uart.ports");
            }
            if (null != ports) {
                StringTokenizer t = new StringTokenizer(ports, ",");
                while(devNum-- > 0 && t.hasMoreTokens()) {
                    t.nextToken();
                }
                // if no more tokens - returns null
                if (t.hasMoreTokens()) {
                    securityName = t.nextToken();
                }
            }
        }

        return securityName;
    }

    protected void checkPowerPermission(){
        //AccessController.getContext().checkPermission(new UARTPermission(getSecurityName(), DevicePermission.POWER_MANAGE));
    }

    protected synchronized void processEvent(int event, int bytesProcessed){
        UARTEventListener listener = eventListeners.get(event);
        if (listener != null){
            try{
                if(event == UARTEvent.INPUT_DATA_AVAILABLE){
                    stopReceiveTimer();
                }
                UARTEvent uartEvent = new UARTEvent(this, event);
                listener.eventDispatched(uartEvent);
                if(event == UARTEvent.INPUT_DATA_AVAILABLE){
                    startReceiveTimer();
                }
            }
            catch(Exception e){
                //do nothing
            }
        }

        switch(event){
        case UARTEvent.INPUT_DATA_AVAILABLE:
            if (inRoundListener != null){
                ByteBuffer buffer = readBuffers[readBufferIdx];
                if (null == buffer) {
                    try{
                        inRoundListener.failed(new Exception("Event processing error. Read buffer is null"), this);
                    }catch(Exception e){
                        //do nothing
                    }
                    return;
                }
                /*
                 *  read0 is designed to copy available data from the javacall buffer to java buffer,
                 *  because of that no slice() call is necessary, the following is necessary:
                 *
                 *  int bytesReaden = read0(buffer.slice());
                 */
                int bytesRead = read0(buffer);
                try {
                    buffer.position(buffer.position() + bytesRead);
                } catch (IllegalArgumentException e) {
                    //buffer.position() + bytesRead < 0 (not expected) or buffer.position() + bytesRead > limit
                    buffer.position(buffer.limit());
                }

                if(!buffer.hasRemaining() || ( receiveTriggerLevel !=0 && buffer.position() > receiveTriggerLevel) || (-1 == bytesProcessed)) {
                    RoundCompletionEvent<UART,ByteBuffer> rcEvent =
                        new RoundCompletionEvent(this, buffer, buffer.position() - readBuffersPositions[readBufferIdx]);

                    if (null != readBuffers[1]) {
                        //2 buffers schema
                        //switch buffers, than notify user
                        readBufferIdx = readBufferIdx == 0 ? 1 : 0;
                        buffer = readBuffers[readBufferIdx];
                        readBuffersPositions[readBufferIdx] = buffer.position();

                        //notify user
                        try{
                            inRoundListener.inputRoundCompleted(rcEvent);
                        }catch(Exception e){
                            //do nothing, listener should not throw an exception
                        }
                    }else{
                        //1 buffer
                        //notify the user first, then keep reading
                        try{
                            inRoundListener.inputRoundCompleted(rcEvent);
                            readBuffersPositions[readBufferIdx] = buffer.position();
                        }catch(Exception e){
                            //do nothing, listener should not throw an exception
                        }
                    }//end of else 1 buffer
                }
            }
            break;

        case UARTEvent.INPUT_BUFFER_OVERRUN:
            break;
        case UARTEvent.OUTPUT_BUFFER_EMPTY:
            if (outRoundListener != null){
                ByteBuffer buffer = writeBuffers[writeBufferIdx];

                if (null == buffer) {
                    try{
                        outRoundListener.failed(new Exception("Event processing error. Write buffer is null"), this);
                    }catch(Exception e){
                        //do nothing, listener should not throw an exception
                    }
                    return;
                }
                buffer.position(buffer.position() + bytesProcessed);

                if (!buffer.hasRemaining()) {
                    RoundCompletionEvent<UART,ByteBuffer> rcEvent = new RoundCompletionEvent(this, buffer, buffer.position() - writeBuffersPositions[writeBufferIdx]);

                    if (null != writeBuffers[1]) {
                        //2 byffers
                        //switch buffers, than notify user
                        writeBufferIdx = writeBufferIdx == 0 ? 1 : 0;
                        buffer = writeBuffers[writeBufferIdx];
                        //keep writing from the second buffer before user notice
                        if (isWriting) {
                            if (buffer.hasRemaining()) {
                                /* since write0 needs to be called by the following manner:
                                 * int bytesWritten = write0(buffer.slice());
                                 * buffer.position(buffer.position() + bytesWritten);
                                 *
                                 * but because write0 is calling inside processEvent as part of asynchronous write,
                                 * bytesWritten = 0
                                 * write0 returns immediatelly after copy data to javacall buffer.
                                 */
                                write0(buffer);
                            }
                        }
                        //notify user
                        try{
                            outRoundListener.outputRoundCompleted(rcEvent);
                        }catch(Exception e){
                            //do nothing, listener should not throw an exception
                        }
                    }else{
                        //1 buffer
                        //notify user first, then keep writing
                        try{
                            outRoundListener.outputRoundCompleted(rcEvent);
                        }catch(Exception e){
                            //do nothing, listener should not throw an exception
                        }
                        if(isWriting){
                            if (buffer.hasRemaining()){
                                write0(buffer);
                            }
                        }
                    }//end of else 1 buffer
                }else{ //buffer has remaining, keep writing
                    if(isWriting){
                        write0(buffer);
                    }
                }
            }//if (outRoundListener != null)
            break;
        }//switch(event)
    }

    /**
     * Gets the current baud rate. If the baud rate was not set previously using {@link #setBaudRate(int)} the
     * peripheral configuration-specific default value is returned.
     *
     * @return the current baud rate.
     */
    @Override
    public synchronized int getBaudRate() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        return getBaudRate0();

    }

    /**
     * Gets the current number of bits per character.
     *
     */
    @Override
    public synchronized int getDataBits() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        return getDataBits0();
    }

    /**
     * Gets the current parity.
     *
     */
    @Override
    public synchronized int getParity() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        return getParity0();
    }

    /**
     * Gets the current number of stop bits per character.
     *
     */
    @Override
    public synchronized int getStopBits() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        return getStopBits0();
    }

    /**
     * Sets the baud rate.
     *
     */
    @Override
    public synchronized void setBaudRate(int baudRate) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        if(baudRate <= 0){
            throw new java.lang.UnsupportedOperationException();
        }
        setBaudRate0( baudRate);
    }

    /**
     * Sets the number of bits per character.
     *
     */
    @Override
    public synchronized void setDataBits(int dataBits) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        setDataBits0( dataBits);
    }

    /**
     * Registers a {@link UARTEventListener} instance to monitor input data availability, input buffer overrun or
     * empty output buffer conditions. While the listener can be triggered by hardware interrupts, there are no
     * real-time guarantees of when the listener will be called.
     */
    @Override
    public synchronized void setEventListener(int eventId, UARTEventListener listener) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkOpen();

        if (eventId != UARTEvent.INPUT_BUFFER_OVERRUN && eventId != UARTEvent.INPUT_DATA_AVAILABLE &&
            eventId != UARTEvent.OUTPUT_BUFFER_EMPTY && eventId != UARTEvent.BREAK_INTERRUPT &&
            eventId != UARTEvent.PARITY_ERROR &&eventId != UARTEvent.FRAMING_ERROR){
                throw new IllegalArgumentException();
        }
        UARTEventListener registeredListener = eventListeners.get(eventId);

        if (listener != null && registeredListener != null){
            //got listener for the eventId
            throw new IllegalStateException();
        }

        if (listener == null){
            //remove listener for the eventId
             eventListeners.remove(eventId);
             unsubscribe(eventId);
             // remove handlers
        }else{
             eventListeners.put(eventId, listener);
             subscribe(eventId);
             if(eventId == UARTEvent.INPUT_DATA_AVAILABLE){
                startReceiveTimer();
             }
        }
    }

    private void subscribe(int eventId){
        UARTEventHandler.getInstance().addEventListener(eventId, this);
        setEventListener0(eventId);
    }

    private void unsubscribe(int eventId){
        UARTEventHandler.getInstance().removeEventListener(eventId, this);
        removeEventListener0(eventId);
    }
    /**
     * Sets the parity.
     */
    @Override
    public synchronized void setParity(int parity) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        setParity0( parity);
    }

    /**
     * Sets the number of stop bits per character.
     *
     */
    @Override
    public synchronized void setStopBits(int stopBits) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        setStopBits0( stopBits);
    }

    /**
     * Starts asynchronous writing in sucessive rounds - initially writing the data remaining in the provided
     * buffer. Additional rounds are asynchronously fetched by notifying the provided {@link OutputRoundListener}
     * instance once the initial data have been written. The initial data to be written
     * is retrieved from the provided buffer; the data to write during the subsequent rounds is retrieved
     * from that very same buffer upon invocation od the provided {@link OutputRoundListener} instance.
     */
    @Override
    public synchronized void startWriting(ByteBuffer src, OutputRoundListener<UART, ByteBuffer> listener) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        checkWrite();
        writeBuffers[0] = src;
        writeBuffersPositions[0] = src.position();
        writeBufferIdx = 0;
        outRoundListener = listener;
        subscribe(UARTEvent.OUTPUT_BUFFER_EMPTY);
        write0(src);
        isWriting = true;
    }

    /**
     * Starts asynchronous writing in successive rounds.
     */
    @Override
    public synchronized void startWriting(ByteBuffer src1, ByteBuffer src2, OutputRoundListener<UART, ByteBuffer> listener) throws IOException,
        UnavailableDeviceException, ClosedDeviceException{
        writeBuffers[1] = src2;
        writeBuffersPositions[1] = src2.position();
        startWriting(src1, listener);
    }

    /**
     * Stops (cancels) the currently active writing session.
     */
    @Override
    public synchronized void stopWriting() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        if (isWriting){
            unsubscribe(UARTEvent.OUTPUT_BUFFER_EMPTY);

            outRoundListener = null;
            writeBuffers[0] = writeBuffers[1] = null;
            stopWriting0();
            isWriting = false;
        }
    }

    /**
     * Starts asynchronous reading in sucessive rounds - reading data into the provided
     * buffer.
     */
    @Override
    public synchronized void startReading(ByteBuffer src, InputRoundListener<UART, ByteBuffer> listener) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        checkRead();
        if(src == null || listener == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.UART_NULL_SRC_OR_LISTENER)
            );
        }

        inRoundListener = listener;
        readBuffers[0] = src;
        readBuffersPositions[0] = src.position();
        readBufferIdx = 0;
        /*
                        subscribe calls set_event_listener, in case of INPUT_DATA_AVAILABLE
                        the native function checks if data available in the internal
                        buffer and generates INPUT_DATA_AVAILABLE event if so.
                */
        subscribe(UARTEvent.INPUT_DATA_AVAILABLE);
    }

    /**
     * Starts asynchronous reading in sucessive rounds.
     */
    @Override
    public synchronized void startReading(ByteBuffer src1, ByteBuffer src2, InputRoundListener<UART, ByteBuffer> listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException{
        if(src1 == null || src2 == null || listener == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.UART_NULL_SRC1_OR_SRC2_OR_LISTENER)
            );
        }
        readBuffers[1] = src2;
        readBuffersPositions[1] = src2.position();
        startReading(src1, listener);
    }

    /**
     * Stops (cancels) the currently active reading session.
      */
    @Override
    public synchronized void stopReading() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        if (inRoundListener != null){
            unsubscribe(UARTEvent.INPUT_DATA_AVAILABLE);
            inRoundListener = null;
            readBuffers[0] = readBuffers[1] = null;
            stopReading0();
        }
    }

    /**
     * Generates a break condition for the specified duration.
     */
    @Override
    public synchronized void generateBreak(int duration) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the receive trigger level
     */
    @Override
    public synchronized void setReceiveTriggerLevel(int level) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        if(level < 0){
            throw new IllegalArgumentException();
        }
        receiveTriggerLevel = level;
    }

    /**
     * Gets the current receive trigger level.
     *
     */
    @Override
    public synchronized int getReceiveTriggerLevel() throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        return receiveTriggerLevel;
    }

    /**
     * Reads a sequence of bytes from this UART into the given buffer.
     */
    @Override
    public int read(ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException{
        if (dst == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.UART_NULL_DST)
            );
        }

        int ret = dst.position();
        if(!dst.hasRemaining()){
            return 0;
        }else{
            startReceiveTimer();
            startReading(dst, getLocalInputRoundListener());
            synchronized(synchReadLock){
                try{
                    synchReadLock.wait();
                }catch(InterruptedException iE){
                    throw new IOException();
                }finally{
                    stopReceiveTimer();
                }
            }
            stopReading();
            ret = dst.position() - ret;
        }
        return ret==0?-1:ret ;
    }

    /**
     * Writes a sequence of bytes to this UART from the given buffer.
     */
    @Override
    public int write(ByteBuffer src) throws IOException,
            UnavailableDeviceException, ClosedDeviceException{
        if (src == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.UART_NULL_SRC)
            );
        }

        synchronized (this) {
            checkPowerState();
            checkWrite();
        }

        int ret = 0;
        try {
            isWriting = true;
            /*
             * synchronous write0 returns number of written bytes
             * slice is needed to avoid memory corruption because src buffer modification
             * might happen during write0
             */
            ret = write0(src.slice());
            try{
                src.position(src.position() + ret);
            } catch (IllegalArgumentException e) {
                //IAE happens if src.position() + ret < 0 (not expected) or src.position() + ret > limit
                src.position(src.limit());
            }
        } finally {
            isWriting = false;
            return ret;
        }
    }

    @Override
    public synchronized void close() throws IOException{
        if (isOpen()) {
            if (modemListener != null){
                ModemSignalDispatcher.getInstance().removeListener(getHandle().getNativeHandle(), this);
                modemListener = null;
            }
            synchronized(synchReadLock){
                synchReadLock.notifyAll();
            }
            stopWriting();
            stopReading();
            super.close();
        }
    }

    @Override
    public synchronized int getReceiveTimeout() throws IOException, UnavailableDeviceException, ClosedDeviceException {
        checkPowerState();
        return inputTimeout;
    }

    @Override
    public synchronized void setReceiveTimeout(int timeout) throws IOException, UnavailableDeviceException, ClosedDeviceException {
        checkPowerState();
        if(timeout < 0 ){
            throw new UnsupportedOperationException(
                    ExceptionMessage.format(ExceptionMessage.UART_NEGATIVE_TIMEOUT)
            );
        }
        inputTimeout = timeout;
    }


    private void checkRead(){
        if (inRoundListener != null){
            throw new IllegalStateException(
                ExceptionMessage.format(ExceptionMessage.UART_ACTIVE_READ_OPERATION)
            );
        }
    }

    private void checkWrite(){
        if (isWriting){
            throw new IllegalStateException(
                ExceptionMessage.format(ExceptionMessage.UART_ACTIVE_WRITE_OPERATION)
            );
        }
    }

//ModemUART stuff
    /**
     * Sets or clears the designated signal.
     */
    public synchronized void setSignalState(int signalID, boolean state) throws IOException,
                                UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();
        setDTESignalState0( signalID, state);
    }
    /**
     * Gets the state of the designated signal.
     *
     */
    public synchronized boolean getSignalState(int signalID) throws IOException,
                                UnavailableDeviceException, ClosedDeviceException{

        if (signalID != ModemSignalsControl.CTS_SIGNAL && signalID != ModemSignalsControl.DCD_SIGNAL &&
            signalID != ModemSignalsControl.DSR_SIGNAL && signalID != ModemSignalsControl.DTR_SIGNAL &&
            signalID != ModemSignalsControl.RI_SIGNAL  && signalID != ModemSignalsControl.RTS_SIGNAL){

            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.UART_UNKNOWN_SIGNAL_ID)
            );
        }
        checkPowerState();
        return getDCESignalState0( signalID);
    };

    /**
     * Registers a {@link ModemSignalListener} instance which will get asynchronously notified when one of the
     * designated signals changes. Notification will automatically begin after registration completes.
     */
    public synchronized void setSignalChangeListener(ModemSignalListener<ModemUART> listener, int signals)
            throws IOException, UnavailableDeviceException, ClosedDeviceException{
        checkPowerState();

        int tmpS = ~(ModemSignalsControl.CTS_SIGNAL|ModemSignalsControl.DCD_SIGNAL|ModemSignalsControl.DSR_SIGNAL
                  |ModemSignalsControl.DTR_SIGNAL|ModemSignalsControl.RI_SIGNAL|ModemSignalsControl.RTS_SIGNAL);

        if ( ( signals & tmpS ) > 0 ){

            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.UART_SIGNALS_NOT_BITWISE_COMBINATION)
            );
        }

        if ( (listener != null) && (modemListener != null) ){
            throw new IllegalStateException(
                ExceptionMessage.format(ExceptionMessage.UART_LISTENER_ALREADY_REGISTERED)
            );
        }

        if (listener != null) {
            if (modemListener == null) {
                ModemSignalDispatcher.getInstance().addListener(getHandle().getNativeHandle(), this);
            }
            modemListener = listener;
        } else {
            ModemSignalDispatcher.getInstance().removeListener(getHandle().getNativeHandle(), this);
            modemListener = null;
        }
    }

    public void signalChanged(int signalLine, boolean state) {
        ModemSignalListener l = modemListener;
        if (null != l) {
            ModemSignalEvent sce = new ModemSignalEvent(this, signalLine, state);
            try{
                l.signalStateChanged(sce);
            }catch(Exception e){
                //do nothing
            }
        }
    }
//end ModemUART stuff

//ModemUART native
    private native void setDTESignalState0( int signalID, boolean state);
    private native boolean getDCESignalState0( int signalID);
//end ModemUART native

    protected synchronized int getGrpID() {
        return getUartId0();
    }

    public synchronized ByteBuffer getInputBuffer() throws ClosedDeviceException,
            IOException {
        throw new java.lang.UnsupportedOperationException();
    }

    public synchronized ByteBuffer getOutputBuffer() throws ClosedDeviceException,
            IOException {
        throw new java.lang.UnsupportedOperationException();
    }

    private native void removeEventListener0(int eventId);
    private native void setEventListener0(int eventId);

    private native void openUARTByConfig0(byte[] devName, int baudrate, int stopBits, int flowControl, int bitsPerChar, int parity, boolean exclusive);

    private native int write0(ByteBuffer src);
    private native int read0(ByteBuffer src);

    private native int getBaudRate0();
    private native void setBaudRate0(int baudRate);

    private native int getDataBits0();
    private native void setDataBits0(int dataBits);

    private native int getParity0();
    private native void setParity0(int parity);

    private native int getStopBits0();
    private native void setStopBits0(int stopBits);

    private native void stopWriting0();
    private native void stopReading0();

    private native int getUartId0();
}
