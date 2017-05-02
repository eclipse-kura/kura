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

package jdk.dio.dac;

import jdk.dio.BufferAccess;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.IntBuffer;

/**
 * The {@code DACChannel} interface provides methods for controlling a DAC (Digital to Analog
 * Converter) channel.
 * <p />
 * One DAC device can have several channels. Raw digital output values (samples) are converted to
 * analog output values according to the DAC channel resolution. The raw digital output values may
 * range from {@link #getMinValue getMinValue} to {@link #getMaxValue getMaxValue}. Actual output
 * voltage values can be calculated from the raw digital output values and the
 * <em>Reference Voltage</em> value as returned by {@link #getVRefValue getVRefValue}.
 * <p />
 * A DAC channel may be identified by the numeric ID and by the name (if any defined) that
 * correspond to its registered configuration. A {@code DACChannel} instance can be opened by a call
 * to one of the {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using
 * its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code DACChannel} instance is
 * opened with an ad-hoc {@link DACChannelConfig} configuration (which includes its hardware
 * addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig)
 * DeviceManager.open(config,...)} it is not assigned any ID nor name.
 * <p />
 * Once opened, an application can write an output value to a DAC channel by calling the
 * {@link #generate(int) generate(int)} method or can write a series of output values to be
 * converted over a period of time by calling the {@link #generate(IntBuffer) generate(IntBuffer)}
 * method.
 * <p />
 * An application can also asynchronously write a series of output values (samples) to be converted
 * at a certain rate by calling the {@link #startGeneration(IntBuffer, GenerationRoundListener)
 * startGeneration} method with a {@link GenerationRoundListener} instance which will get cyclicly
 * and asynchronously invoked to fetch more values to be converted. Analog output generation can be
 * stopped by calling the {@link #stopGeneration stopGeneration} method.
 * <p />
 * Only one output generation (synchronous or asynchronous) can be going on at any time.
 * <p />
 * When an application is no longer using an DAC channel it should call the {@link #close close}
 * method to close the DAC channel. Any further attempt to set or get the value of a DAC channel
 * which has been closed will result in a {@link ClosedDeviceException} been thrown.
 * <p />
 * Asynchronous notification of output generation completion is only loosely tied to
 * hardware-level interrupt requests. The platform does not guarantee notification in a
 * deterministic/timely manner.
 *
 * @see GenerationRoundListener
 * @since 1.0
 */
public interface DACChannel extends Device<DACChannel>, BufferAccess<IntBuffer> {

    /**
     * Returns the maximum raw value this channel can convert. If the DAC device resolution is
     * {@code n} then the {@code min} value returned by {@link #getMinValue getMinValue} and the
     * {@code max} value returned by {@link #getMaxValue getMaxValue} are such that: <blockquote>
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
     * Gets the minimum output sampling interval (in microseconds) that can be set using
     * {@link #setSamplingInterval setSamplingInterval}.
     *
     * @return the minimum output sampling interval (in microseconds).
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
     * Returns the minimum raw value this channel can convert. If the DAC device resolution is
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
     * Gets the output sampling interval (in microseconds). If the output sampling interval was not
     * set previously using {@link #setSamplingInterval setSamplingInterval} the device
     * configuration-specific default value is returned.
     *
     * @return the output sampling interval (in microseconds).
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
     * Returns the <em>Reference Voltage</em> value of this DAC channel. If the reference voltage is
     * {@code vRef} and the DAC device resolution is {@code n} then the actual output voltage value
     * corresponding to a raw value {@code value} written to this channel can be calculated as
     * follows: <blockquote>
     *
     * <pre>
     * {@code vOutput = (value * vRef) / (2^n)}
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
     * Sets the output sampling interval (in microseconds). Whether changing the sampling interval
     * has an immediate effect or not on an active (synchronous or asynchronous) generation is
     * device- as well as platform-dependent.
     *
     * @param interval
     *            the output sampling interval (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws InvalidOutputSamplingRateException
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
     * Writes the provided raw output value to this channel. The corresponding converted analog
     * output value will be held until it is overwritten by another output generation.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous
     * output generation upon this channel then an invocation of this method will block until the
     * first operation is complete.
     * <p />
     * Only one output generation (synchronous or asynchronous) can be going on at any time.
     *
     * @param value
     *            the raw value to be output.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if the provided raw value is not within the range defined by
     *             {@link #getMinValue getMinValue} and {@link #getMaxValue getMaxValue}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if an asynchronous output generation is already active.
     */
    void generate(int value) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Writes {@code count} raw output values from the provided buffer to this channel for
     * conversion.
     * <p />
     * The raw output values will be converted according to the current output sampling interval as
     * returned by {@link #getSamplingInterval getSamplingInterval}.
     * <p />
     * <i>r</i> integers will be written to this channel, where <i>r</i> is the number of integers
     * remaining in the buffer, that is, {@code src.remaining()}, at the moment this method is
     * invoked.
     * <p />
     * <p />
     * Suppose that an integer sequence of length <i>n</i> is written, where <i>{@code 0 <= n <= r}
     * </i>. This integer sequence will be transferred from the buffer starting at index <i>p</i>,
     * where <i>p</i> is the buffer's position at the moment this method is invoked; the index of
     * the last integer written will be <i>{@code p + n - 1}</i>. Upon return the buffer's position
     * will be equal to <i>{@code p + n}</i>; its limit will not have changed.
     * <p />
     * The operation will return only after writing all of the <i>r</i> requested integers.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous
     * output generation upon this channel, however, then an invocation of this method will block
     * until the first operation is complete.
     * <p />
     * Only one output generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the
     * designated raw values is not within the range defined by {@link #getMinValue getMinValue}
     * and {@link #getMaxValue getMaxValue}. If a value is not within range the actual analog value
     * output by the DAC device is hardware- or driver-specific: the output value may for example be
     * equal to the maximum output value or it may correspond to the raw value where the most
     * significant bits beyond the {@code n} bits of the DAC device resolution have been truncated.
     *
     * @param src
     *            the buffer from which the integer raw values can be retrieved.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if an asynchronous output generation is already active.
     */
    void generate(IntBuffer src) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous analog output generation on this channel from a series of raw output
     * values (samples). More values to be converted are asynchronously fetched by notifying the
     * provided {@link GenerationRoundListener} instance once {@code count} raw output values have
     * been converted. The raw output values to be converted are read from the provided buffer.
     * <p />
     * Analog output generation can be stopped by a call to {@link #stopGeneration stopGeneration}.
     * <p />
     * <i>r</i> integers will be written to this channel, where <i>r</i> is the number of integers
     * remaining in the buffer (possibly {@code 0}), that is, {@code src.remaining()}, at the moment
     * this method is initially invoked and then subsequently when the listener is returning.
     * <p />
     * Suppose that an integer sequence of length <i>n</i> is written, where <i>{@code 0 <= n <= r}
     * </i>. This integer sequence will be transferred from the buffer starting at index <i>p</i>,
     * where <i>p</i> is the buffer's position at the moment this method is invoked and then
     * subsequently when the listener is returning; the index of the last integer written will be
     * <i>{@code p + n - 1}</i>. Upon invocation of the listener to fetch more values to convert the
     * buffer's position will be equal to <i>{@code p + n}</i>; its limit will not have changed. <br />
     * The buffer's position upon stopping this asynchronous operation by a call to
     * {@link #stopGeneration stopGeneration} is not predictable unless called from within the
     * listener..
     * <p />
     * The raw output values (samples) will be converted according to the current output sampling
     * interval as returned by {@link #getSamplingInterval getSamplingInterval}.
     * <p />
     * A buffer with {@code 0} integers remaining to be written (that is a buffer already
     * empty) at the moment this method is initially invoked or then subsequently when the listener
     * is returning will not stop the asynchronous operation; the listener is guaranteed to
     * be called back again at the latest as soon as all other events pending at the time of
     * notification have been dispatched.
     * <p />
     * Interfering with the asynchronous operation by accessing and modifying the provided buffer
     * concurrently may yield unpredictable results.
     * <p />
     * Only one output generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation (or a round thereof) has completed.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the
     * designated raw values is not within the range defined by {@link #getMinValue getMinValue}
     * and {@link #getMaxValue getMaxValue}. If a value is not within range the actual analog value
     * output by the DAC device is hardware- or driver-specific: the output value may for example be
     * equal to the maximum output value or it may correspond to the raw value where the most
     * significant bits beyond the {@code n} bits of the DAC device resolution have been truncated.
     *
     * @param src
     *            the buffer from which the integer raw sampled input values are to be retrieved.
     * @param listener
     *            the {@link GenerationRoundListener} instance to be notified when all the output
     *            values have been converted.
     * @throws NullPointerException
     *             If {@code src} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output generation is already active.
     */
    void startGeneration(IntBuffer src, GenerationRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous analog output generation on this channel from a series of raw output
     * values (samples).
     * <p />
     * This method behaves identically to
     * {@link #startGeneration(IntBuffer, GenerationRoundListener)} excepts that it uses
     * double-buffering. Notification will happen when all the raw output values remaining in the
     * current working buffer (initially {@code src1}) have been converted and conversion will
     * proceed with the alternate buffer (which will become the current working buffer). Conversion
     * will only be suspended if the previous event has not yet been handled.
     * <p />
     * A working buffer with {@code 0} integers remaining to be written (that is a buffer
     * already empty) at the moment this method is initially invoked or then subsequently when the
     * listener is returning will not stop the asynchronous operation; the listener is guaranteed to
     * be called back again at the latest as soon as all other events pending at the time of
     * notification have been dispatched.
     * <p />
     * Only one output generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the working buffer until the operation (or a round thereof) has completed.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the
     * designated raw values is not within the range defined by {@link #getMinValue getMinValue}
     * and {@link #getMaxValue getMaxValue}. If a value is not within range the actual analog value
     * output by the DAC device is hardware- or driver-specific: the output value may for example be
     * equal to the maximum output value or it may correspond to the raw value where the most
     * significant bits beyond the {@code n} bits of the DAC device resolution have been truncated.
     *
     * @param src1
     *            the first buffer from which the integer raw sampled input values are to be
     *            retrieved.
     * @param src2
     *            the second buffer from which the integer raw sampled input values are to be
     *            retrieved.
     * @param listener
     *            the {@link GenerationRoundListener} instance to be notified when all the output
     *            values have been converted.
     * @throws NullPointerException
     *             If {@code src1}, {@code src2} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output generation is already active.
     */
    void startGeneration(IntBuffer src1, IntBuffer src2, GenerationRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops the asynchronous analog output generation on this channel as started by a call to one
     * of the {@link #startGeneration startGeneration} methods.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void stopGeneration() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
