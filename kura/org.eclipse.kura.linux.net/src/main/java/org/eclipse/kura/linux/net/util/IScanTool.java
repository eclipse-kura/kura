package org.eclipse.kura.linux.net.util;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.wifi.WifiAccessPoint;

public interface IScanTool {

	public List<WifiAccessPoint> scan() throws KuraException;
}
