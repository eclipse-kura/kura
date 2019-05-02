package org.eclipse.kura.driver.block.task;

import java.io.IOException;

import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;

public class ChannelListenerBlockTask extends ChannelBlockTaskWrapper {

    private final ChannelListener listener;

    public ChannelListenerBlockTask(final ChannelBlockTask wrapped, final ChannelListener listener) {
        super(wrapped);

        this.listener = listener;
    }

    public ChannelListener getListener() {
        return listener;
    }

    @Override
    public void run() throws IOException {
        final ChannelBlockTask wrapped = getWrappedTask();

        wrapped.run();

        listener.onChannelEvent(new ChannelEvent(wrapped.getRecord()));
    }
}
