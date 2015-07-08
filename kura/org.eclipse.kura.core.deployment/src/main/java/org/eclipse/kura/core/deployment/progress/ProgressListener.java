package org.eclipse.kura.core.deployment.progress;

import java.util.EventListener;

public interface ProgressListener extends EventListener {

	public void progressChanged(ProgressEvent progress);
	
}
