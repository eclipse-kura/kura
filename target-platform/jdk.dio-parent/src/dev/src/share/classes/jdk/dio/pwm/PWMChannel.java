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

package jdk.dio.pwm;

import jdk.dio.BufferAccess;
import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.IntBuffer;

/**
 * The {@code PWMChannel} interface provides methods for controlling a PWM (Pulse Width Modulation) signal generator
 * channel.
 * <p />
 * One PWM generator/controller can have several channels. A PWM channel can generate pulses on a digital output line
 * (possibly a GPIO pin).
 * <p />
 * A PWM channel may be identified by the numeric ID and by the name (if any defined) that correspond to its
 * registered configuration. A {@code PWMChannel} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code PWMChannel} instance is opened with an ad-hoc
 * {@link PWMChannelConfig} configuration (which includes its hardware addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig) DeviceManager.open(config,...)} it is not
 * assigned any ID nor name.
 * <p />
 * Once opened, an application can set the pulse period using the {@link #setPulsePeriod} and then generate a certain
 * number of pulses of a specified width by calling one of the {@link #generate} methods.
 * <p />
 * An application can also asynchronously generate a train of pulses either of a specified width up to a specified
 * maximum count or from widths specified in a buffer by calling one of the
 * {@link #startGeneration(int, int, jdk.dio.pwm.GenerationListener) startGeneration} methods with a
 * {@link GenerationListener} instance which will get notified upon completion. Such an asynchronous pulse generation
 * can be stopped or canceled by calling the {@link #stopGeneration stopGeneration} method.
 * <p />
 * Only one output/generation operation (synchronous or asynchronous) can be going on at any time.
 * <p />
 * When an application is no longer using a PWM channel it should call the {@link #close PWMChannel.close} method to
 * close the PWM channel. Any further attempt to use a PWM channel which has been closed will result in a
 * {@link ClosedDeviceException} been thrown.
 * <p />
 * Upon opening a PWM channel the default pulse width and duty cycle are always {@code 0}. The idle
 * state is platform or configuration-specific.
 *
 * @see GenerationListener
 * @see PWMPermission
 * @since 1.0
 */
public interface PWMChannel extends Device<PWMChannel>, BufferAccess<IntBuffer> {

    /**
     * Sets the pulse period of this PWM channel.
     *
     * @param period
     *            the pulse period as a period in microseconds.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws InvalidPulseRateException
     *             if the resulting pulse rate/frequency (i.e.: <i>1/period</i>) is either higher than the maximum supported
     *             pulse rate/frequency (i.e.: <i>1/{@link #getMinPulsePeriod getMinPulsePeriod}</i>) or lower than the minimum supported
     *             pulse rate/frequency (i.e.: <i>1/{@link #getMaxPulsePeriod getMaxPulsePeriod}</i>)
     * @throws IllegalArgumentException
     *             if {@code period} is negative or zero.
     */
    void setPulsePeriod(int period) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the pulse period of this PWM channel (in microseconds). If the pulse period was not set previously using
     * {@link #setPulsePeriod setPulsePeriod} the device configuration-specific default value is returned.
     *
     * @return the pulse period (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getPulsePeriod() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the maximum pulse period of this PWM channel (in microseconds) that can bet set by a call to
     * {@link #setPulsePeriod setPulsePeriod}.
     *
     * @return the maximum pulse period (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMaxPulsePeriod() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the minimum pulse period of this PWM channel (in microseconds) that can bet set by a call to
     * {@link #setPulsePeriod setPulsePeriod}.
     *
     * @return the minimum pulse period (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMinPulsePeriod() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Generates a pulse train containing the specified count of pulses of the specified width.
     * <p />
     * To generate pulses of a specific duty cycle {@code dutyCycle}, this method may be called as follows: <br />
     *
     * <pre>
     * float dutyCycle = 0.5f;
     * pwmChannel.generate((pwmChannel.getPulsePeriod() * dutyCycle), count);
     * </pre>
     * <p />
     * The operation will return only after generating all of the {@code count} requested pulses.
     * <p />
     * The pulses will be generated according to the current pulse period as returned by {@link #getPulsePeriod getPulsePeriod}.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous output operation
     * upon this channel then an invocation of this method will block until the first operation is complete.
     * <p />
     * Only one output operation (synchronous or asynchronous) can be going on at any time.
     *
     * @param width
     *            the pulse width (in microseconds).
     * @param count
     *            the maximum number of pulses to generate.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code width} or {@code count} is equal to or less than {@code 0} or if {@code width} is greater
     *             than the currently set period.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if an asynchronous pulse generation session is already active.
     */
    void generate(int width, int count) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Generates a pulse train containing {@code count} pulses of the specified widths. The provded buffer contains the
     * widths of the pulses to generate.
     * <p />
     * <i>r</i> pulses will be generated by this channel, where <i>r</i> is the number of integers (pulse widths)
     * remaining in the buffer, that is, {@code src.remaining()}, at the moment this method is invoked.
     * <p />
     * Suppose that a pulse width integer value sequence of length <i>n</i> is provided, where <i>{@code 0 <= n <= r}
     * </i>. The sequence starts at index <i>p</i>, where <i>p</i> is the buffer's position at the moment this method is
     * invoked; the index of the last pulse width integer value written will be <i>{@code p + n - 1}</i>. Upon return
     * the buffer's position will be equal to <i>{@code p + n}</i>; its limit will not have changed.
     * <p />
     * The operation will return only after generating all of the <i>r</i> requested pulses.
     * <p />
     * The pulses will be generated according to the current pulse period as returned by {@link #getPulsePeriod getPulsePeriod}.
     * <p />
     * This method may be invoked at any time. If another thread has already initiated a synchronous pulse generation
     * upon this channel, however, then an invocation of this method will block until the first operation is complete.
     * <p />
     * Only one pulse generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the designated pulse width values
     * is greater than the currently set period. If a pulse width value is not within range
     * the actual width of the pulse generated by the PWM device is hardware- or driver-specific: the pulse width
     * may for example be equal to the set period, corresponding to a 100% duty cycle.
     *
     * @param src
     *            the buffer from which the pulse width integer values can be retrieved.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if an asynchronous pulse generation is already active.
     */
    void generate(IntBuffer src) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts an asynchronous pulse train generation session - continuously generating pulses of the specified width
     * until explicitly stopped.
     * <p />
     * The pulses will be generated according to the current pulse period as returned by {@link #getPulsePeriod getPulsePeriod}.
     * <p/>
     * Pulse generation will immediately start and proceed asynchronously. It may be stopped prior to completion by a
     * call to {@link #stopGeneration stopGeneration}.
     * <p />
     * Only one pulse generation session can be going on at any time.
     *
     * @param width
     *            the pulse width (in microseconds).
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code width} is equal to or less than
     *             {@code 0} or if {@code width} is greater than the currently set period.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous generation is already active.
     */
    void startGeneration(int width) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts an asynchronous pulse train generation session - generating pulses of the specified width up to the
     * specified count. The provided {@link GenerationListener} instance will be invoked upon completion, that is when
     * the count of generated pulses reaches the specified count value.
     * <p />
     * The pulses will be generated according to the current pulse period as returned by {@link #getPulsePeriod getPulsePeriod}.
     * <p/>
     * Pulse generation will immediately start and proceed asynchronously. It may be stopped prior to completion by a
     * call to {@link #stopGeneration stopGeneration}.
     * <p />
     * Only one pulse generation session can be going on at any time.
     *
     * @param width
     *            the pulse width (in microseconds).
     * @param count
     *            the maximum number of pulses to generate.
     * @param listener
     *            the {@link GenerationListener} instance to be notified when the count of generated pulses reaches the
     *            specified count value.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code count} is equal to or less than {@code 0} or if {@code width} is equal to or less than
     *             {@code 0} or if {@code width} is greater than the currently set period.
     * @throws NullPointerException
     *             if {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous generation is already active.
     */
    void startGeneration(int width, int count, GenerationListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous pulse train generation in successive rounds - initially generating pulses of the specified
     * widths up to the specified initial count - as indicated by the number of remaining elements in the provided
     * buffer. Additional rounds are asynchronously fetched by notifying the provided {@link GenerationRoundListener}
     * instance once the initial count of pulses have been generated. The widths of the initial pulses to be generated
     * are read from the provided buffer; the widths of the pulses to generate during the subsequent rounds are read
     * from that very same buffer upon invocation of the provided {@link GenerationRoundListener} instance.
     * <p />
     * Pulse generation can be stopped by a call to {@link #stopGeneration stopGeneration}.
     * <p />
     * <i>r</i> integers will be written to this channel, where <i>r</i> is the number of integers remaining in the
     * buffer (possibly {@code 0}), that is, {@code src.remaining()}, at the moment this method is initially invoked and then subsequently when the listener is returning.
     * <p />
     * Suppose that an integer sequence of length <i>n</i> is written, where <i>{@code 0 <= n <= r}</i>. This integer
     * sequence will be transferred from the buffer starting at index <i>p</i>, where <i>p</i> is the buffer's position
     * at the moment this method is invoked and then subsequently when the listener is returning; the index of the last integer written will be <i>{@code p + n - 1}</i>.
     * Upon invocation of the listener to fetch the widths of more pulses to generate the buffer's position will be equal to <i>{@code p + n}</i>; its limit will not have changed.
     * <br />
     * The buffer's position upon stopping this asynchronous operation by a call to {@link #stopGeneration stopGeneration}
     * is not predictable unless called from within the listener.
     * <p />
     * The pulses will be generated according to the current pulse period as returned by {@link #getPulsePeriod getPulsePeriod}. The
     * pulse period can be changed by the provided {@link GenerationRoundListener} instance upon notification of each
     * pulse train subsequence.
     * <p />
     * A buffer with {@code 0} integers remaining to be written (that is a buffer already empty) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Interfering with the asynchronous operation by accessing and modifying the provided buffer concurrently
     * may yield unpredictable results.
     * <p />
     * Only one pulse generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation (or a round thereof) has completed.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the designated pulse width values
     * is greater than the currently set period. If a pulse width value is not within range
     * the actual width of the pulse generated by the PWM device is hardware- or driver-specific: the pulse width
     * may for example be equal to the set period, corresponding to a 100% duty cycle.
     *
     * @param src
     *            the buffer for the widths (in microseconds) of the pulses to generate.
     * @param listener
     *            the {@link GenerationRoundListener} instance to be notified when pulses have been geneerated for all
     *            the width values remaining in the buffer.
     * @throws NullPointerException
     *             If {@code src} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output generation is already active.
     */
    void startGeneration(IntBuffer src, GenerationRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous pulse train generation in successive rounds.
     * <p />
     * This method behaves identically to {@link #startGeneration(IntBuffer, GenerationRoundListener)} excepts that it
     * uses double-buffering. Notification will happen when pulses have been generated for all the width values
     * remaining in the current working buffer (initially {@code src1}) and generation will proceed with the alternate buffer (which will become the
     * current working buffer). Generation will only be suspended if the previous event has not yet been handled.
     * <p />
     * A working buffer with {@code 0} integers remaining to be written (that is a buffer already empty) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Only one pulse generation (synchronous or asynchronous) can be going on at any time.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the working buffer until the operation (or a round thereof) has completed.
     * <p />
     * This method does not throw an {@link IllegalArgumentException} if any of the designated pulse width values
     * is greater than the currently set period. If a pulse width value is not within range
     * the actual width of the pulse generated by the PWM device is hardware- or driver-specific: the pulse width
     * may for example be equal to the set period, corresponding to a 100% duty cycle.
     *
     * @param src1
     *            the first buffer for the widths (in microseconds) of the pulses to generate.
     * @param src2
     *            the second buffer for the widths (in microseconds) of the pulses to generate.
     * @param listener
     *            the {@link GenerationRoundListener} instance to be notified when pulses have been geneerated for all
     *            the width values remaining in the working buffer.
     * @throws NullPointerException
     *             If {@code src1}, {@code src2} or {@code listener} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output generation is already active.
     */
    void startGeneration(IntBuffer src1, IntBuffer src2, GenerationRoundListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops (cancels) the currently active pulse generation session.
     * <p />
     * This method return silently if no pulse generation session is currently active.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void stopGeneration() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
