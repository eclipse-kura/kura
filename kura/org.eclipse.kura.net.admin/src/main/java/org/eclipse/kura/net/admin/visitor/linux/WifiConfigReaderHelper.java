package org.eclipse.kura.net.admin.visitor.linux;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WifiConfigReaderHelper {

    private static final Logger logger = LoggerFactory.getLogger(WifiConfigReaderHelper.class);

    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
    }

    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                getConfig((WifiInterfaceConfigImpl) netInterfaceConfig);
            }
        }
    }

    private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        logger.debug("Getting config for {}", interfaceName);

        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();

        if (wifiInterfaceAddressConfigs == null || wifiInterfaceAddressConfigs.isEmpty()) {
            wifiInterfaceAddressConfigs = new ArrayList<>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        }

        WifiInterfaceAddressConfig wifiInterfaceAddressConfig = wifiInterfaceAddressConfigs.get(0);
        if (wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            List<NetConfig> netConfigs = wifiInterfaceAddressConfig.getConfigs();

            if (netConfigs == null) {
                netConfigs = new ArrayList<>();
                ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig).setNetConfigs(netConfigs);
            }

            // Get infrastructure config
            netConfigs.add(getWifiConfig(interfaceName));
        }
    }

    public WifiConfig getWifiConfig(String ifaceName) throws KuraException {
        return new WifiConfig();
    }
}
