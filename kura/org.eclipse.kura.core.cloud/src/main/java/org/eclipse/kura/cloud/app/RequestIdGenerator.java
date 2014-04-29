/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.cloud.app;

import java.util.Random;

public class RequestIdGenerator {
	
	private static RequestIdGenerator s_instance = new RequestIdGenerator();
	
	private Random m_random;
	
	private RequestIdGenerator() {
		super();
		m_random = new Random();
	}
	
	public static RequestIdGenerator getInstance() {
		return s_instance;
	}
	
	public String next() {
		long timestamp = System.currentTimeMillis();
		
		long random;
		synchronized (m_random) {
			 random = m_random.nextLong();
		}
		
		return timestamp + "-" + random;
	}
}
