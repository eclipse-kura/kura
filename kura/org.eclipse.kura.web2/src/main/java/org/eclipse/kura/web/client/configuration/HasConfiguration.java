package org.eclipse.kura.web.client.configuration;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;

public interface HasConfiguration {

    public GwtConfigComponent getConfiguration();

    public void clearDirtyState();

    public boolean isValid();

    public boolean isDirty();

    public void markAsDirty();

    public void setListener(Listener listener);

    public interface Listener {

        public void onConfigurationChanged(HasConfiguration hasConfiguration);

        public void onDirtyStateChanged(HasConfiguration hasConfiguration);
    }
}
