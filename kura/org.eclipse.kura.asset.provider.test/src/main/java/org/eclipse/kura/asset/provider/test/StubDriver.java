package org.eclipse.kura.asset.provider.test;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.ChannelDescriptor;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.DriverListener;
import org.eclipse.kura.asset.DriverRecord;

public class StubDriver implements Driver {

	@Override
	public void connect() throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public ChannelDescriptor getChannelDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void read(List<DriverRecord> records) throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerDriverListener(Map<String, Object> channelConfig, DriverListener listener)
			throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterDriverListener(DriverListener listener) throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(List<DriverRecord> records) throws KuraException {
		// TODO Auto-generated method stub

	}

}
