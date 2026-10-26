/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.watchdog;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A snapshot of the supervision state of the hardware watchdog, as returned by
 * {@link WatchdogService#getWatchdogStatus()}. All the values refer to the same instant.
 *
 * @since 2.9
 */
@ProviderType
public final class WatchdogStatus {

    private final WatchdogState state;
    private final boolean deviceOpen;
    private final Optional<String> devicePath;
    private final Optional<Instant> lastRefreshTime;
    private final long refreshIntervalMs;
    private final Optional<String> failureReason;

    private WatchdogStatus(Builder builder) {
        this.state = builder.state;
        this.deviceOpen = builder.deviceOpen;
        this.devicePath = builder.devicePath;
        this.lastRefreshTime = builder.lastRefreshTime;
        this.refreshIntervalMs = builder.refreshIntervalMs;
        this.failureReason = builder.failureReason;
    }

    /**
     * Returns the supervision state of the watchdog. The system is under hardware watchdog supervision only if the
     * state is {@link WatchdogState#SUPERVISING}.
     *
     * @return the supervision state
     */
    public WatchdogState getState() {
        return this.state;
    }

    /**
     * Returns whether the watchdog device is currently open.
     *
     * @return {@code true} if the watchdog device is open, {@code false} otherwise
     */
    public boolean isDeviceOpen() {
        return this.deviceOpen;
    }

    /**
     * Returns the path of the configured watchdog device.
     *
     * @return the watchdog device path, or an empty {@link Optional} if no device is configured
     */
    public Optional<String> getDevicePath() {
        return this.devicePath;
    }

    /**
     * Returns the time of the last successful refresh of the watchdog device since the device has been opened.
     *
     * @return the time of the last successful refresh, or an empty {@link Optional} if the device has not been
     *         refreshed
     */
    public Optional<Instant> getLastRefreshTime() {
        return this.lastRefreshTime;
    }

    /**
     * Returns the configured interval between two refreshes of the watchdog device, in milliseconds.
     *
     * @return the refresh interval in milliseconds
     */
    public long getRefreshIntervalMs() {
        return this.refreshIntervalMs;
    }

    /**
     * Returns a description of the reason why the system is not under hardware watchdog supervision or a reboot has
     * been requested.
     *
     * @return the failure reason, or an empty {@link Optional} if there is no failure
     */
    public Optional<String> getFailureReason() {
        return this.failureReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state, this.deviceOpen, this.devicePath, this.lastRefreshTime,
                this.refreshIntervalMs, this.failureReason);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        WatchdogStatus other = (WatchdogStatus) obj;
        return this.state == other.state && this.deviceOpen == other.deviceOpen
                && Objects.equals(this.devicePath, other.devicePath)
                && Objects.equals(this.lastRefreshTime, other.lastRefreshTime)
                && this.refreshIntervalMs == other.refreshIntervalMs
                && Objects.equals(this.failureReason, other.failureReason);
    }

    @Override
    public String toString() {
        return "WatchdogStatus [state=" + this.state + ", deviceOpen=" + this.deviceOpen + ", devicePath="
                + this.devicePath + ", lastRefreshTime=" + this.lastRefreshTime + ", refreshIntervalMs="
                + this.refreshIntervalMs + ", failureReason=" + this.failureReason + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private WatchdogState state = WatchdogState.DISABLED;
        private boolean deviceOpen = false;
        private Optional<String> devicePath = Optional.empty();
        private Optional<Instant> lastRefreshTime = Optional.empty();
        private long refreshIntervalMs = 0;
        private Optional<String> failureReason = Optional.empty();

        private Builder() {
        }

        public Builder withState(WatchdogState state) {
            this.state = Objects.requireNonNull(state, "state cannot be null");
            return this;
        }

        public Builder withDeviceOpen(boolean deviceOpen) {
            this.deviceOpen = deviceOpen;
            return this;
        }

        public Builder withDevicePath(Optional<String> devicePath) {
            this.devicePath = Objects.requireNonNull(devicePath, "devicePath cannot be null");
            return this;
        }

        public Builder withLastRefreshTime(Optional<Instant> lastRefreshTime) {
            this.lastRefreshTime = Objects.requireNonNull(lastRefreshTime, "lastRefreshTime cannot be null");
            return this;
        }

        public Builder withRefreshIntervalMs(long refreshIntervalMs) {
            this.refreshIntervalMs = refreshIntervalMs;
            return this;
        }

        public Builder withFailureReason(Optional<String> failureReason) {
            this.failureReason = Objects.requireNonNull(failureReason, "failureReason cannot be null");
            return this;
        }

        public WatchdogStatus build() {
            return new WatchdogStatus(this);
        }
    }
}
