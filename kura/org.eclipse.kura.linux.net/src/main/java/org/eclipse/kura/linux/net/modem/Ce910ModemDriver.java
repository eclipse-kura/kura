package org.eclipse.kura.linux.net.modem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ce910ModemDriver extends OptionModemDriver {
	private static final Logger s_logger = LoggerFactory.getLogger(Le910ModemDriver.class);
	private static final String s_vendor = "1bc7";
	private static final String s_product = "1011";
	
	public Ce910ModemDriver() {
		super(s_vendor, s_product);
	}
	
	public int install() throws Exception {	
		s_logger.info("Installing {} driver for Telit CE910 modem", getName());
		int status = super.install();
		return status;
	}
}
