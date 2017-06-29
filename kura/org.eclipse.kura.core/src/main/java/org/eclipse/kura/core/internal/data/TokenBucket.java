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
    private final long refillPeriod;
    private int remainingTokens;
    private long lastRefillTime;

    public TokenBucket(int capacity, long refillPeriod) {
        this.capacity = capacity;
        this.remainingTokens = capacity;
        this.refillPeriod = refillPeriod;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public boolean getToken() {
        boolean result = false;
        refill();
        if (isTokenAvailable()) {
            this.remainingTokens--;
            result = true;
        }
        return result;
    }

    private boolean isTokenAvailable() {
        return this.remainingTokens != 0;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefillTime >= this.refillPeriod) {
            this.remainingTokens = (int) Math.min(this.capacity,
                    this.remainingTokens + (now - this.lastRefillTime) / this.refillPeriod);
            this.remainingTokens = Math.max(1, this.remainingTokens);
            this.lastRefillTime += ((now - lastRefillTime) / refillPeriod) * refillPeriod;
        }
    }

    public long getTokenWaitTime() {
        long now = System.currentTimeMillis();
        long timeToRefill = (this.lastRefillTime + this.refillPeriod) - now;
        return Math.max(0, timeToRefill);
    }
}
