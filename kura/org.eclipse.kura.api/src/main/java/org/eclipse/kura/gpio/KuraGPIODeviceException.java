package org.eclipse.kura.gpio;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class KuraGPIODeviceException extends KuraException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1750311704822256084L;

	public KuraGPIODeviceException(Object argument) {
		super(KuraErrorCode.GPIO_EXCEPTION, null, argument);
	}
	
	public KuraGPIODeviceException(Throwable cause, Object argument) {
		super(KuraErrorCode.GPIO_EXCEPTION, cause, argument);
	}
}
