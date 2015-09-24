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
package org.eclipse.kura.linux.net.modem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionModemDriver extends UsbModemDriver {
	
	private static final Logger s_logger = LoggerFactory.getLogger(OptionModemDriver.class);

	public OptionModemDriver(String vendor, String product) {
		super("option", vendor, product);
	}
	
	public int install() throws Exception {	
		int status = super.install();
		if (status == 0) {
			s_logger.info("submiting {}:{} information to option driver ...", getVendor(), getProduct());
			File newIdFile = new File("/sys/bus/usb-serial/drivers/option1/new_id");
			if (newIdFile.exists()) {
				StringBuffer sb = new StringBuffer();
				sb.append(getVendor());
				sb.append(' ');
				sb.append(getProduct());
				
				FileOutputStream fos = new FileOutputStream(newIdFile);
				PrintWriter pw = new PrintWriter(fos);
				pw.write(sb.toString());
				pw.flush();
				pw.close();
				fos.close();
			}
		}
		return status;
	}
}
