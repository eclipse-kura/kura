package org.eclipse.kura.linux.position.provider;

import org.eclipse.kura.position.PositionListener;

public interface LockStatusListener extends PositionListener {

    public void onLockStatusChanged(final boolean hasLock);
}