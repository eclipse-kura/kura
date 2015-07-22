package org.eclipse.kura.gpio;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class KuraClosedDeviceException extends KuraException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1750311704822256084L;

	public KuraClosedDeviceException(Object argument) {
		super(KuraErrorCode.CLOSED_DEVICE, null, argument);
	}
	
	public KuraClosedDeviceException(Throwable cause, Object argument) {
		super(KuraErrorCode.CLOSED_DEVICE, cause, argument);
	}
}
