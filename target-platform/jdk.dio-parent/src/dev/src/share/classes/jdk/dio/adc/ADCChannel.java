/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dio.adc;

import jdk.dio.BufferAccess;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.*;

/**
 * The {@code ADCChannel} interface provides methods for controlling an ADC (Analog to Digital
 * Converter) channel.
 * <p />
 * One ADC device can have several channels. Analog input are sampled and converted according to the
 * ADC device resolution to raw digital values between {@link #getMinValue getMinValue} and
 * {@link #getMaxValue getMaxValue}. Actual input voltage values can be calculated from raw digital
 * values and the <em>Reference Voltage</em> value as returned by {@link #getVRefValue getVRefValue}.
 * <p />
 * An ADC channel may be identified by the numeric ID and by the name (if any defined) that
 * correspond to its registered configuration. An {@code ADCChannel} instance can be opened by a
 * call to one of the {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods
 * using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code ADCChannel} instance is
 * opened with an ad-hoc {@link ADCChannelConfig} configuration (which includes its hardware
 * addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig)
 * DeviceManager.open(config,...)} it is not assigned any ID nor name.
 * <p />
 * Once opened, an application can read the current sampled input value of an ADC channel by calling
 * the {@link #acquire() acquire} method or can acquire the input values sampled over a period of time by
 * calling the {@link #acquire(IntBuffer)} method.
 * <p />
 * An application can also asynchronously acquire the input values sampled at a certain rate by
 * calling the {@link #startAcquisition(IntBuffer, AcquisitionRoundListener) startAcquisition}
 * methods with a {@link AcquisitionRoundListener} instance which will get cyclicly and
 * asynchronously notified when the desired number of samples have been acquired. The analog input
 * acquisition can be stopped by calling the {@link #stopAcquisition() stopAcquisition} method.
 * <p />
 * An application can monitor the input value by calling the
 * {@link #startMonitoring(int, int, MonitoringListener) startMonitoring} method with a low and a
 * high threshold value and {@link MonitoringListener} instance which will get asynchronously
 * notified when the input value gets out of or back in the defined range. The monitoring can be
 * stopped by calling the {@link #stopMonitoring() stopMonitoring} method.
 * <p />
 * At most one acquisition (synchronous or asynchronous) and/or (depending on the platform) at most
 * one monitoring can be going on at any time. If an acquisition and a monitoring can be performed
 * concurrently, they will be performed at the same sampling rate (see
 * {@link #getSamplingInterval() getSamplingInterval}). They therefore respectively acquire and
 * monitor the same sampled input values.
 * <p />
 * When an application is no longer using an ADC channel it should call the {@link #close() close}
 * method to close the ADC channel. Any further attempt to set or get the value of a ADC channel
 * which has been closed will result in a {@link ClosedDeviceException} been thrown.
 * <p />
 * Asynchronous notification of range conditions or input acquisition is only loosely tied
 * to hardware-level interrupt requests. The platform does not guarantee notification in a
 * deterministic/timely manner.
 *
 * @see AcquisitionRoundListener
 * @see MonitoringListener
 * @see ADCPermission
 * @since 1.0
 */
public interface ADCChannel extends Device<ADCChannel>, BufferAccess<IntBuffer> {

    /**
     * Returns the maximum raw value this channel can convert. If the ADC device resolution is
     * {@code n} then the {@code min} value returned by {@link #getMinValue() getMinValue} and the
     * {@code max} value returned by {@link #getMaxValue() getMaxValue} are such that: <blockquote>
     *
     * <pre>
     * {@code (max - min) == (2^n - 1)}.
     * </pre>
     *
     * </blockquote>
     *
     * @return the maximum raw value this channel can convert.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMaxValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the minimum input sampling interval (in microseconds) that can be set using
     * {@link #setSamplingInterval setSamplingInterval}.
     *
     * @return the minimum input sampling interval (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMinSamplingInterval() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the minimum raw value this channel can convert. If the ADC device resolution is
     * {@code n} then the {@code min} value returned by {@link #getMinValue getMinValue} and the
     * {@code max} value returned by {@link #getMaxValue getMaxValue} are such that: <blockquote>
     *
     * <pre>
     * {@code (max - min) == (2^n - 1)}.
     * </pre>
     *
     * </blockquote>
     *
     * @return the minimum raw value this channel can convert.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMinValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the input sampling interval (in microseconds). If the sampling interval was not set
     * previously using {@link #setSamplingInterval setSamplingInterval} the device
     * configuration-specific default value is returned.
     *
     * @return the input sampling interval (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getSamplingInterval() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads the current raw sampled input value of this channel.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous
     * input acquisition upon this channel, however, then an invocation of this method will block
     * until the first operation is complete.
     * <p />
     * Only one acquisition (synchronous or asynchronous) can be going on at any time.
     *
     * @return this channel's current raw sampled input value.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another asynchronous acquisition is already active.
     */
    int acquire() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads a sequence of raw sampled input values from this channel and copies them into the
     * provided buffer.
     * <p />
     * The input will be sampled according to the current input sampling interval as returned by
     * {@link #getSamplingInterval getSamplingInterval}.
     * <p />
     * <i>r</i> {@code int} integers will be read from this channel, where <i>r</i> is the number of
     * integers remaining in the buffer, that is, {@code dst.remaining()}, at the moment this method
     * is invoked.
     * <p />
     * Suppose that an integer sequence of length <i>n</i> is read, where <i>{@code 0 <= n <= r}
     * </i>. This integer sequence will be transferred into the buffer so that the first integer in
     * the sequence is at index <i>p</i> and the last integer is at index <i>{@code p + n - 1}</i>,
     * where <i>p</i> is the buffer's position at the moment this method is invoked. Upon return the
     * buffer's position will be equal to <i>{@code p + n}</i>; its limit will not have changed.
     * <p />
     * This operation will block until the requested <i>r</i> integers are read or an error occurs.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous
     * input acquisition upon this channel, however, then an invocation of this method will block
     * until the first operation is complete.
     * <p />
     * Only one acquisition (synchronous or asynchronous) can be going on at any time.
     *
     * @param dst
     *            The buffer into which integer raw sampled input values are to be transferred
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalStateException
     *             if another asynchronous acquisition is already active.
     * @throws UnsupportedOperationException
     *             if an asynchronous monitoring is already active and acquisition and monitoring
     *             cannot be performed concurrently.
     */
    void acquire(IntBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the <em>Reference Voltage</em> value of this ADC channel. If the reference voltage is
     * {@code vRef} and the ADC device resolution is {@code n} then the actual input voltage value
     * corresponding to a raw sampled value {@code value} read on this channel can be calculated as
     * follows: <blockquote>
     *
     * <pre>
     * {@code vInput = (value * vRef) / (2^n)}
     * </pre>
     *
     * </blockquote>
     *
     * @return the <em>Reference Voltage</em> value of this channel.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    double getVRefValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the input sampling interval (in microseconds). Whether changing the sampling interval
     * has an immediate effect or not on an active (synchronous or asynchronous) acquisition is
     * device- as well as platform-dependent.
     *
     * @param interval
     *            the input sampling interval (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws InvalidInputSamplingRateException
     *             if the resulting sampling rate (i.e.: <i>(1 / {@code interval}</i>)) is higher
     *             than the maximum supported sampling rate (i.e.: <i>(1 /
     *             {@link #getMinSamplingInterval getMinSamplingInterval} )</i>).
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code interval} is negative or zero.
     */
    void setSamplingInterval(int interval) throws IOException, UnavailableDeviceException,
            ClosedDeviceException;

    /**
     * Starts asynchronous analog input acquisition on this channel and reads a series of raw
     * sampled input values. The provided {@link AcquisitionRoundListener} instance is cyclicly
     * notified when the provided buffer has been filled with raw sampled input values. The read
     * values are copied into the provided buffer. Once the requested number of raw sampled input
     * values has been read, reading will be suspended and in the event of continuous sampling,
     * subsequent sampled input values may be lost. Reading into the buffer and notification will
     * only resume once the event has been handled. Reading and notification will immediately start
     * and will repeat until it is stopped by a call to {@link #stopAcquisition() stopAcquisition}.
     * <p />
     * <i>r</i> {@code int} integers will be read from this channel, where <i>r</i> is the number of
     * integers remaining in the buffer (possibly {@code 0}), that is, {@code dst.remaining()}, at
     * the moment this method is initially invoked and then subsequently when the listener is
     * returning.
     * <p />
     * Suppose that an integer sequence of length <i>n</i> is read, where <i>{@code 0 <= n <= r}
     * </i>. This integer sequence will be transferred into the buffer so that the first integer in
     * the sequence is at index <i>p</i> and the last integer is at index <i>{@code p + n - 1}</i>,
     * where <i>p</i> is the buffer's position at the moment this method is invoked and then
     * subsequently when the listener is returning. Upon invocation of the listener to fetch more
     * values to convert the buffer's position will be equal to <i>{@code p + n}</i>; its limit will
     * not have changed. <br />
     * The buffer's position upon stopping this asynchronous operation by a call to
     * {@link #stopAcquisition stopAcquisition} is not predictable unless called from within the
     * listener.
     * <p />
     * A buffer with {@code 0} integers remaining to be read (that is a buffer already
     * full) at the moment this method is initially invoked or then subsequently when the listener
     * is returning will not stop the asynchronous operation; the listener is guaranteed to be
     * called back again at the latest as soon as all other events pending at the time of
     * notification have been dispatched.
     * <p />
     * Interfering with the asynchronous operation by accessing and modifying the provided buffer
     * concurrently may yield unpredictable results.
     * <p />
     * The input will be sampled according to the current input sampling interval as returned by
     * {@link #getSamplingInterval getSamplingInterval}.
     * <p />
     * Only one acquisition (synchronous or asynchronous) can be going on at any time. Buffers are
     * not safe for use by multiple concurrent threads so care should be taken to not access the
     * working buffer until the operation (or a round thereof) has completed.
     *
     * @param dst
     *            The buffer into which integer raw sampled input values are to be transferred.
     * @param listener
     *            the {@link AcquisitionRoundListener} instance to be notified when all the sampled
     *            input values have been read.
     * @throws NullPointerException
     *             If {@code dst} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous acquisition is already active.
     * @throws UnsupportedOperationException
     *             if an asynchronous monitoring is already active and acquisition and monitoring
     *             cannot be performed concurrently.
     */
    void startAcquisition(IntBuffer dst, AcquisitionRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous analog input acquisition on this channel and reads a series of raw
     * sampled input values.
     * <p />
     * This method behaves identically to
     * {@link #startAcquisition(IntBuffer, AcquisitionRoundListener)} excepts that it uses
     * double-buffering. Notification will happen when the current working buffer (initially
     * {@code dst1}) has been filled with raw sampled input values and reading will asynchronously
     * proceed with the alternate buffer (which becomes the current working buffer). Reading will
     * only be suspended if the previous event has not yet been handled (this may result in the case
     * of continuous sampling in subsequent sampled input values to be lost).
     * <p />
     * A working buffer with {@code 0} integers remaining to be read (that is a buffer
     * already full) at the moment this method is initially invoked or then subsequently when the
     * listener is returning will not stop the asynchronous operation; the listener is guaranteed to
     * be called back again at the latest as soon as all other events pending at the time of
     * notification have been dispatched.
     * <p />
     * Only one acquisition (synchronous or asynchronous) can be going on at any time. Buffers are
     * not safe for use by multiple concurrent threads so care should be taken to not access the
     * working buffer until the operation (or a round thereof) has completed.
     *
     * @param dst1
     *            The first buffer into which integer raw sampled input values are to be
     *            transferred.
     * @param dst2
     *            The second buffer into which integer raw sampled input values are to be
     *            transferred.
     * @param listener
     *            the {@link AcquisitionRoundListener} instance to be notified when all the sampled
     *            input values have been read.
     * @throws NullPointerException
     *             If {@code dst1}, {@code dst2} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous acquisition is already active.
     * @throws UnsupportedOperationException
     *             if an asynchronous monitoring is already active and acquisition and monitoring
     *             cannot be performed concurrently.
     */
    void startAcquisition(IntBuffer dst1, IntBuffer dst2, AcquisitionRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts monitoring this channel analog input and asynchronously notifies the provided
     * {@link MonitoringListener} instance when this channel's raw sampled input value gets out of
     * or back in the specified range (as defined by a low and a high threshold value). Monitoring
     * and notification will immediately start and will repeat until it is stopped by a call to
     * {@link #stopMonitoring stopMonitoring}. Range notification operates in toggle mode: once
     * notified of an out-of-range condition the application will next only get notified of a
     * back-in-range condition and so on...
     * <p />
     * The sampled input value will be monitored according to the current input sampling interval as
     * returned by {@link #getSamplingInterval getSamplingInterval}.
     * <p />
     * To only get notified when the input value gets over some threshold one may call this method
     * with the {@code low} parameter set to the value of {@link #getMinValue getMinValue}.
     * Conversely, to only get notified when the input value gets under some threshold one may call
     * this method with the {@code high} parameter set to the value of {@link #getMaxValue
     * getMaxValue}.
     * <p />
     * If {@code low} is lower than the minimum value returned by {@link #getMinValue getMinValue}
     * then the minimum value is assumed. If {@code high} is higher the maximum value returned by
     * {@link #getMaxValue getMaxValue}, then the maximum value is assumed.
     * <p />
     * Only one monitoring can be going on at any time.
     *
     * @param listener
     *            the {@link MonitoringListener} instance to be notified when a range condition
     *            occurs.
     * @param low
     *            the low raw threshold value.
     * @param high
     *            the high raw threshold value.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code low} is greater than {@code high}.
     * @throws NullPointerException
     *             if {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if monitoring is already active.
     * @throws UnsupportedOperationException
     *             if an asynchronous acquisition is already active and monitoring and acquisition
     *             cannot be performed concurrently.
     */
    void startMonitoring(int low, int high, MonitoringListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops the asynchronous analog input acquisition on this channel as started by a call to one
     * of the {@link #startAcquisition startAcquisition} methods.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void stopAcquisition() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops the range monitoring of this channel analog input as started by a call to the
     * {@link #startMonitoring startMonitoring} method.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void stopMonitoring() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
