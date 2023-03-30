package org.eclipse.kura.nm.configuration.writer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallNatConfigWriter {

    private static final Logger logger = LoggerFactory.getLogger(FirewallNatConfigWriter.class);

    private final CommandExecutorService executorService;
    private final List<String> wanInterfaceNames;
    private final List<String> natInterfaceNames;

    public FirewallNatConfigWriter(CommandExecutorService executorService, List<String> wanInterfaceNames, List<String> natInterfaceNames) {
        this.executorService = executorService;
        this.wanInterfaceNames = wanInterfaceNames;
        this.natInterfaceNames = natInterfaceNames;
    }

    public void writeConfiguration() throws KuraException {
        try {
            Set<NATRule> natRules = new HashSet<>();

            for (String source : this.natInterfaceNames) {
                for (String destination : this.wanInterfaceNames) {
                    NATRule natRule = new NATRule();
                    // new NATRule(source, destination, true, RuleType.GENERIC)
                    natRule.setSourceInterface(source);
                    natRule.setDestinationInterface(destination);
                    natRule.setMasquerade(true);

                    logger.info("Applying NAT rule {} -> {}: {}", source, destination, natRule);

                    natRules.add(natRule);
                }
            }

            LinuxFirewall firewall = LinuxFirewall.getInstance(this.executorService);
            firewall.replaceAllNatRules(natRules);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "Failed to replace all NAT rules with new ones for interfaces: " + this.natInterfaceNames, e);
        }
    }

}
