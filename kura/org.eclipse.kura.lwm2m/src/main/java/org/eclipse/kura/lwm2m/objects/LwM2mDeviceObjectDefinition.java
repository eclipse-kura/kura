package org.eclipse.kura.lwm2m.objects;

import org.eclipse.kura.system.SystemService;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.response.ValueResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mDeviceObjectDefinition extends BaseInstanceEnabler {
	private static final Logger s_logger = LoggerFactory.getLogger(LwM2mDeviceObjectDefinition.class);
	
	private SystemService m_systemService;
	
	public LwM2mDeviceObjectDefinition() {
	}
	
	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}
	
	@Override
    public ValueResponse read(int resourceid) {
        s_logger.info("Read on Device Resource " + resourceid);
        switch (resourceid) {
        case 0:
            return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                    Value.newStringValue(m_systemService.getDeviceName())));
        case 1:
            return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                    Value.newStringValue(m_systemService.getModelName())));
        case 2:
            return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                    Value.newStringValue(m_systemService.getSerialNumber())));
        case 3:
            return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                    Value.newStringValue(m_systemService.getFirmwareVersion())));
        default:
            return super.read(resourceid);
        }
    }
}
