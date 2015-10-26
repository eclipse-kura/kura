package org.eclipse.kura.gpio;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class KuraUnavailableDeviceException extends KuraException {

	private static final long serialVersionUID = -5115093706356681148L;

	public KuraUnavailableDeviceException(Object argument) {
		super(KuraErrorCode.UNAVAILABLE_DEVICE, null, argument);
	}
	
	public KuraUnavailableDeviceException(Throwable cause, Object argument) {
		super(KuraErrorCode.UNAVAILABLE_DEVICE, cause, argument);
	}
}
