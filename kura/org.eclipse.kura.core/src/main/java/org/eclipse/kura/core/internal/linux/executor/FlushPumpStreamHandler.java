package org.eclipse.kura.core.internal.linux.executor;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.exec.PumpStreamHandler;

public class FlushPumpStreamHandler extends PumpStreamHandler {

    public FlushPumpStreamHandler(final OutputStream out, final OutputStream err, final InputStream input) {
        super(out, err, input);
    }

    @Override
    protected Thread createPump(final InputStream is, final OutputStream os, final boolean closeWhenExhausted) {
        final Thread result = new Thread(new FlushStreamPumper(is, os, closeWhenExhausted), "Exec Stream Pumper");
        result.setDaemon(true);
        return result;
    }

}
