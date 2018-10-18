package org.eclipse.kura.driver.block.task;

public abstract class ChannelBlockTaskWrapper extends ChannelBlockTask {

    private final ChannelBlockTask wrapped;

    public ChannelBlockTaskWrapper(final ChannelBlockTask wrapped) {
        super(wrapped.getRecord(), wrapped.getStart(), wrapped.getEnd(), wrapped.getMode());
        this.wrapped = wrapped;
    }

    public ChannelBlockTask getWrappedTask() {
        return wrapped;
    }

    @Override
    public void setParent(final ToplevelBlockTask parent) {
        super.setParent(parent);
        wrapped.setParent(parent);
    }

    @Override
    public int getStart() {
        return wrapped.getStart();
    }

    @Override
    public int getEnd() {
        return wrapped.getEnd();
    }

    @Override
    public void setEnd(int end) {
        super.setEnd(end);
        wrapped.setEnd(end);
    }

    @Override
    public void setStart(int start) {
        super.setStart(start);
        wrapped.setStart(start);
    }

    @Override
    public void onSuccess() {
        wrapped.onSuccess();
    }

    @Override
    public void onFailure(final Exception exception) {
        wrapped.onFailure(exception);
    }
}
