package org.eclipse.kura.web.client.configuration;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;

public interface HasConfiguration {

    public GwtConfigComponent getConfiguration();

    public void clearDirtyState();

    public boolean isValid();

    public boolean isDirty();

    public interface Listener {

        public void onConfigurationChanged(HasConfiguration hasConfiguration);

    }
}
