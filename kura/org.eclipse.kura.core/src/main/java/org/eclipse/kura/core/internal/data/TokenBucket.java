/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.internal.data;

public class TokenBucket {

    private final int capacity;
    // period in mills between 1 token refill
    private final long refillPeriod;
    private int remainingTokens;
    private long lastRefillTime;

    public TokenBucket(int capacity, long refillPeriod) {
        // Set the capacity to zero means that no token will be retrieved from the bucket,
        // hence no message can be published. So, set it to 1 at least.
        this.capacity = (capacity == 0) ? 1 : capacity;
        this.remainingTokens = capacity;
        this.refillPeriod = refillPeriod;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getRemainingTokens() {
        return this.remainingTokens;
    }

    public long getRefillPeriod() {
        return this.refillPeriod;
    }

    public long getLastRefill() {
        return this.lastRefillTime;
    }

    public boolean hasToken() {
        boolean success = false;
        if (this.refillPeriod == 0) {
            success = true;
        } else {
            if (refill() > 0) {
                this.remainingTokens--;
                success = true;
            }
        }
        return success;
    }

    public void waitForToken() throws InterruptedException {
        if (this.refillPeriod != 0) {
            while (refill() == 0) {
                Thread.sleep(100);
            }
            this.remainingTokens--;
        }
    }

    private int refill() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefillTime >= this.refillPeriod) {
            this.remainingTokens = (int) Math.min(this.capacity,
                    this.remainingTokens + (now - this.lastRefillTime) / this.refillPeriod);
            this.lastRefillTime = now;
        }
        return this.remainingTokens;
    }

}
