package org.eclipse.kura.net2.admin.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// to be fixed...
// what happen to the thread when the ncs is updated?
public class DhcpServerMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerMonitor.class);

    private Map<String, Boolean> dhcpServerEnableForInterface;
    private DhcpServerManager dhcpServerManager;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    public DhcpServerMonitor(CommandExecutorService commandExecutorService) {
        this.dhcpServerEnableForInterface = new HashMap<>();
        this.dhcpServerManager = new DhcpServerManager(commandExecutorService);
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.handle = this.worker.scheduleAtFixedRate(this::monitor, 0, 30, TimeUnit.SECONDS);
    }

    public void putDhcpServerEnableForInterface(String interfaceName, boolean enable) {
        this.dhcpServerEnableForInterface.put(interfaceName, enable);
    }

    public void removeInterface(String interfaceName) {
        this.dhcpServerEnableForInterface.remove(interfaceName);
    }

    private void monitor() {
        this.dhcpServerEnableForInterface.entrySet().forEach(entry -> {
            String interfaceName = entry.getKey();
            boolean enable = entry.getValue();
            if (enable && !this.dhcpServerManager.isRunning(interfaceName)) {
                startDhcpServer(interfaceName);
            } else if (!enable && this.dhcpServerManager.isRunning(interfaceName)) {
                stopDhcpServer(interfaceName);
            }
        });
    }

    private void startDhcpServer(String interfaceName) {
        logger.debug("Starting DHCP server for {}", interfaceName);
        try {
            this.dhcpServerManager.enable(interfaceName);
        } catch (KuraException e) {
            logger.warn("Failed to start DHCP server for the interface " + interfaceName, e);
        }
    }

    private void stopDhcpServer(String interfaceName) {
        logger.debug("Stopping DHCP server for {}", interfaceName);
        try {
            this.dhcpServerManager.disable(interfaceName);
        } catch (KuraException e) {
            logger.warn("Failed to stop DHCP server for the interface " + interfaceName, e);
        }
    }

}
