package org.eclipse.kura.web.client.ui.Network;

import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;

public interface Tab {
	
	public void setDirty(boolean flag);
	public boolean isDirty();
	public boolean isValid();
	public void setNetInterface(GwtNetInterfaceConfig config);
	public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf);
	public void refresh();
}
