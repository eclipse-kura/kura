package org.eclipse.kura.core.internal.linux.executor;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.executor.ExecutorFactory;

public class DefaultExecutorFactory implements ExecutorFactory {

    @Override
    public Executor getExecutor() {
        return new DefaultExecutor();
    }

}
