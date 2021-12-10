package org.eclipse.kura.linux.position;

import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpsdPositionProvider implements PositionProvider {

    private static final Logger logger = LoggerFactory.getLogger(GpsdPositionProvider.class);

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public Position getPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        // TODO Auto-generated method stub
        return null;
    }

}
