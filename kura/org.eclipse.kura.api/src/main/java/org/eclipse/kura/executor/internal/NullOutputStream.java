package org.eclipse.kura.executor.internal;

import java.io.IOException;
import java.io.OutputStream;

//to avoid import to common.io for simple classes
//when JDK 11 use `java.io.OutputStream.nullOutputStream();`
public class NullOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {

    }

    @Override
    public void write(byte[] b) throws IOException {

    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {

    }

}
