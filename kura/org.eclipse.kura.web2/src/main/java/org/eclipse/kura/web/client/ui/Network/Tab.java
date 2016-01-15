package org.eclipse.kura.web.client.bootstrap.ui.Network;

import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;

public interface Tab {
	
	public void setDirty(boolean flag);
	public boolean isDirty();
	public boolean isValid();
	public void setNetInterface(GwtBSNetInterfaceConfig config);
	public void getUpdatedNetInterface(GwtBSNetInterfaceConfig updatedNetIf);
	public void refresh();
}
