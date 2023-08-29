/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

public class IptablesConfigConstants {

    protected static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    protected static final String FIREWALL_TMP_CONFIG_FILE_NAME = "/tmp/iptables";
    protected static final String FILTER = "filter";
    protected static final String NAT = "nat";
    protected static final String MANGLE = "mangle";
    protected static final String STAR_NAT = "*" + NAT;
    protected static final String STAR_FILTER = "*" + FILTER;
    protected static final String STAR_MANGLE = "*" + MANGLE;
    protected static final String COMMIT = "COMMIT";
    protected static final String IPTABLES_COMMAND = "iptables";
    protected static final String FORWARD = "FORWARD";
    protected static final String INPUT = "INPUT";
    protected static final String OUTPUT = "OUTPUT";
    protected static final String PREROUTING = "PREROUTING";
    protected static final String POSTROUTING = "POSTROUTING";
    protected static final String RETURN = "RETURN";
    protected static final String INPUT_KURA_CHAIN = "input-kura";
    protected static final String OUTPUT_KURA_CHAIN = "output-kura";
    protected static final String FORWARD_KURA_CHAIN = "forward-kura";
    protected static final String FORWARD_KURA_PF_CHAIN = "forward-kura-pf";
    protected static final String FORWARD_KURA_IPF_CHAIN = "forward-kura-ipf";
    protected static final String PREROUTING_KURA_CHAIN = "prerouting-kura";
    protected static final String PREROUTING_KURA_PF_CHAIN = "prerouting-kura-pf";
    protected static final String POSTROUTING_KURA_CHAIN = "postrouting-kura";
    protected static final String POSTROUTING_KURA_PF_CHAIN = "postrouting-kura-pf";
    protected static final String POSTROUTING_KURA_IPF_CHAIN = "postrouting-kura-ipf";
    protected static final String ADD_INPUT_KURA_CHAIN = "-I INPUT -j input-kura";
    protected static final String ADD_OUTPUT_KURA_CHAIN = "-I OUTPUT -j output-kura";
    protected static final String ADD_FORWARD_KURA_CHAIN = "-I FORWARD -j forward-kura";
    protected static final String ADD_FORWARD_KURA_PF_CHAIN = "-I forward-kura -j forward-kura-pf";
    protected static final String ADD_FORWARD_KURA_IPF_CHAIN = "-I forward-kura -j forward-kura-ipf";
    protected static final String ADD_PREROUTING_KURA_CHAIN = "-I PREROUTING -j prerouting-kura";
    protected static final String ADD_PREROUTING_KURA_PF_CHAIN = "-I prerouting-kura -j prerouting-kura-pf";
    protected static final String ADD_POSTROUTING_KURA_CHAIN = "-I POSTROUTING -j postrouting-kura";
    protected static final String ADD_POSTROUTING_KURA_PF_CHAIN = "-I postrouting-kura -j postrouting-kura-pf";
    protected static final String ADD_POSTROUTING_KURA_IPF_CHAIN = "-I postrouting-kura -j postrouting-kura-ipf";
    protected static final String RETURN_PREROUTING_KURA_CHAIN = "-A prerouting-kura -j RETURN";
    protected static final String RETURN_PREROUTING_KURA_PF_CHAIN = "-A prerouting-kura-pf -j RETURN";
    protected static final String RETURN_POSTROUTING_KURA_CHAIN = "-A postrouting-kura -j RETURN";
    protected static final String RETURN_POSTROUTING_KURA_PF_CHAIN = "-A postrouting-kura-pf -j RETURN";
    protected static final String RETURN_POSTROUTING_KURA_IPF_CHAIN = "-A postrouting-kura-ipf -j RETURN";
    protected static final String RETURN_INPUT_KURA_CHAIN = "-A input-kura -j RETURN";
    protected static final String RETURN_OUTPUT_KURA_CHAIN = "-A output-kura -j RETURN";
    protected static final String RETURN_FORWARD_KURA_CHAIN = "-A forward-kura -j RETURN";
    protected static final String RETURN_FORWARD_KURA_PF_CHAIN = "-A forward-kura-pf -j RETURN";
    protected static final String RETURN_FORWARD_KURA_IPF_CHAIN = "-A forward-kura-ipf -j RETURN";
    protected static final String ALLOW_ALL_TRAFFIC_TO_LOOPBACK = "-A input-kura -i lo -j ACCEPT";
    protected static final String ALLOW_ONLY_INCOMING_TO_OUTGOING = "-A input-kura -m state --state RELATED,ESTABLISHED -j ACCEPT";
    protected static final String POSTROUTING_KURA_POLICY = ":postrouting-kura - [0:0]";
    protected static final String POSTROUTING_KURA_PF_POLICY = ":postrouting-kura-pf - [0:0]";
    protected static final String POSTROUTING_KURA_IPF_POLICY = ":postrouting-kura-ipf - [0:0]";
    protected static final String PREROUTING_KURA_POLICY = ":prerouting-kura - [0:0]";
    protected static final String PREROUTING_KURA_PF_POLICY = ":prerouting-kura-pf - [0:0]";
    protected static final String FORWARD_KURA_POLICY = ":forward-kura - [0:0]";
    protected static final String FORWARD_KURA_PF_POLICY = ":forward-kura-pf - [0:0]";
    protected static final String FORWARD_KURA_IPF_POLICY = ":forward-kura-ipf - [0:0]";
    protected static final String OUTPUT_KURA_POLICY = ":output-kura - [0:0]";
    protected static final String INPUT_KURA_POLICY = ":input-kura - [0:0]";
    protected static final String POSTROUTING_ACCEPT_POLICY = ":POSTROUTING ACCEPT [0:0]";
    protected static final String PREROUTING_ACCEPT_POLICY = ":PREROUTING ACCEPT [0:0]";
    protected static final String INPUT_ACCEPT_POLICY = ":INPUT ACCEPT [0:0]";
    protected static final String OUTPUT_ACCEPT_POLICY = ":OUTPUT ACCEPT [0:0]";
    protected static final String FORWARD_ACCEPT_POLICY = ":FORWARD ACCEPT [0:0]";
    protected static final String FORWARD_DROP_POLICY = ":FORWARD DROP [0:0]";
    protected static final String INPUT_DROP_POLICY = ":INPUT DROP [0:0]";
    protected static final String[] IPTABLES_CREATE_INPUT_KURA_CHAIN = { "-N", INPUT_KURA_CHAIN, "-t", FILTER };
    protected static final String[] IPTABLES_CREATE_FORWARD_KURA_CHAIN = { "-N", FORWARD_KURA_CHAIN, "-t", FILTER };
    protected static final String[] IPTABLES_CREATE_FORWARD_KURA_PF_CHAIN = { "-N", FORWARD_KURA_PF_CHAIN, "-t",
            FILTER };
    protected static final String[] IPTABLES_CREATE_FORWARD_KURA_IPF_CHAIN = { "-N", FORWARD_KURA_IPF_CHAIN, "-t",
            FILTER };
    protected static final String[] IPTABLES_CREATE_OUTPUT_KURA_CHAIN = { "-N", OUTPUT_KURA_CHAIN, "-t", FILTER };
    protected static final String[] IPTABLES_INPUT_DROP_POLICY = { "-P", INPUT, "DROP" };
    protected static final String[] IPTABLES_FORWARD_DROP_POLICY = { "-P", FORWARD, "DROP" };
    protected static final String[] IPTABLES_CHECK_INPUT_KURA_CHAIN = { "-C", INPUT, "-j", INPUT_KURA_CHAIN, "-t",
            FILTER };
    protected static final String[] IPTABLES_CHECK_OUTPUT_KURA_CHAIN = { "-C", OUTPUT, "-j", OUTPUT_KURA_CHAIN, "-t",
            FILTER };
    protected static final String[] IPTABLES_CHECK_FORWARD_KURA_CHAIN = { "-C", FORWARD, "-j", FORWARD_KURA_CHAIN, "-t",
            FILTER };
    protected static final String[] IPTABLES_CHECK_FORWARD_KURA_PF_CHAIN = { "-C", FORWARD_KURA_CHAIN, "-j",
            FORWARD_KURA_PF_CHAIN, "-t", FILTER };
    protected static final String[] IPTABLES_CHECK_FORWARD_KURA_IPF_CHAIN = { "-C", FORWARD_KURA_CHAIN, "-j",
            FORWARD_KURA_IPF_CHAIN, "-t", FILTER };
    protected static final String[] IPTABLES_CREATE_INPUT_KURA_CHAIN_NAT = { "-N", INPUT_KURA_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CREATE_OUTPUT_KURA_CHAIN_NAT = { "-N", OUTPUT_KURA_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CREATE_PREROUTING_KURA_CHAIN = { "-N", PREROUTING_KURA_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CREATE_PREROUTING_KURA_PF_CHAIN = { "-N", PREROUTING_KURA_PF_CHAIN, "-t",
            NAT };
    protected static final String[] IPTABLES_CREATE_POSTROUTING_KURA_CHAIN = { "-N", POSTROUTING_KURA_CHAIN, "-t",
            NAT };
    protected static final String[] IPTABLES_CREATE_POSTROUTING_KURA_PF_CHAIN = { "-N", POSTROUTING_KURA_PF_CHAIN, "-t",
            NAT };
    protected static final String[] IPTABLES_CREATE_POSTROUTING_KURA_IPF_CHAIN = { "-N", POSTROUTING_KURA_IPF_CHAIN,
            "-t", NAT };
    protected static final String[] IPTABLES_CHECK_INPUT_KURA_CHAIN_NAT = { "-C", INPUT, "-j", INPUT_KURA_CHAIN, "-t",
            NAT };
    protected static final String[] IPTABLES_CHECK_OUTPUT_KURA_CHAIN_NAT = { "-C", OUTPUT, "-j", OUTPUT_KURA_CHAIN,
            "-t", NAT };
    protected static final String[] IPTABLES_CHECK_PREROUTING_KURA_CHAIN = { "-C", PREROUTING, "-j",
            PREROUTING_KURA_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CHECK_PREROUTING_KURA_PF_CHAIN = { "-C", PREROUTING_KURA_CHAIN, "-j",
            PREROUTING_KURA_PF_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CHECK_POSTROUTING_KURA_CHAIN = { "-C", POSTROUTING, "-j",
            POSTROUTING_KURA_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CHECK_POSTROUTING_KURA_PF_CHAIN = { "-C", POSTROUTING_KURA_CHAIN, "-j",
            POSTROUTING_KURA_PF_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CHECK_POSTROUTING_KURA_IPF_CHAIN = { "-C", POSTROUTING_KURA_CHAIN, "-j",
            POSTROUTING_KURA_IPF_CHAIN, "-t", NAT };
    protected static final String[] IPTABLES_CREATE_INPUT_KURA_CHAIN_MANGLE = { "-N", INPUT_KURA_CHAIN, "-t", MANGLE };
    protected static final String[] IPTABLES_CREATE_OUTPUT_KURA_CHAIN_MANGLE = { "-N", OUTPUT_KURA_CHAIN, "-t",
            MANGLE };
    protected static final String[] IPTABLES_CHECK_INPUT_KURA_CHAIN_MANGLE = { "-C", INPUT, "-j", INPUT_KURA_CHAIN,
            "-t", MANGLE };
    protected static final String[] IPTABLES_CHECK_OUTPUT_KURA_CHAIN_MANGLE = { "-C", OUTPUT, "-j", OUTPUT_KURA_CHAIN,
            "-t", MANGLE };
    protected static final String[] IPTABLES_CREATE_PREROUTING_KURA_CHAIN_MANGLE = { "-N", PREROUTING_KURA_CHAIN, "-t",
            MANGLE };
    protected static final String[] IPTABLES_CHECK_PREROUTING_KURA_CHAIN_MANGLE = { "-C", PREROUTING, "-j",
            PREROUTING_KURA_CHAIN, "-t", MANGLE };
    protected static final String[] IPTABLES_CREATE_POSTROUTING_KURA_CHAIN_MANGLE = { "-N", POSTROUTING_KURA_CHAIN,
            "-t", MANGLE };
    protected static final String[] IPTABLES_CHECK_POSTROUTING_KURA_CHAIN_MANGLE = { "-C", POSTROUTING, "-j",
            POSTROUTING_KURA_CHAIN, "-t", MANGLE };
    protected static final String[] IPTABLES_CREATE_FORWARD_KURA_CHAIN_MANGLE = { "-N", FORWARD_KURA_CHAIN, "-t",
            MANGLE };
    protected static final String[] IPTABLES_CHECK_FORWARD_KURA_CHAIN_MANGLE = { "-C", FORWARD, "-j",
            FORWARD_KURA_CHAIN, "-t", MANGLE };

    protected static final String COMMAND_EXECUTOR_SERVICE_MESSAGE = "CommandExecutorService not set.";
    protected static final String CHAIN_CREATION_FAILED_MESSAGE = "Failed to create chain";
    protected static final String CHAIN_RETURN_RULE_FAILED_MESSAGE = "Failed to add return rule";

    protected static final String[] ALLOW_ICMP = {
            "-A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT",
            "-A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT" };

    protected static final String[] DO_NOT_ALLOW_ICMP = {
            "-A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP",
            "-A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP" };

    protected IptablesConfigConstants() {
        // Empty constructor
    }
}