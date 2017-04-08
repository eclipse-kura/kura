package org.eclipse.kura.position;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface PositionListener {

    public void newNmeaSentence(String nmeaSentence);
}
