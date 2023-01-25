package org.eclipse.kura.net2.admin.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DhcpServerMonitor {

    private List<String> interfaceNames;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    public DhcpServerMonitor() {
        this.interfaceNames = new ArrayList<>();
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.handle = this.worker.scheduleAtFixedRate(this::monitor, 0, 30, TimeUnit.SECONDS);
    }

    public void addInterface(String interfaceName) {
        this.interfaceNames.add(interfaceName);
    }

    public void removeInterface(String interfaceName) {
        this.interfaceNames.remove(interfaceName);
    }

    private void monitor() {
        this.interfaceNames.forEach(interfaceName -> {

        });
    }

}
