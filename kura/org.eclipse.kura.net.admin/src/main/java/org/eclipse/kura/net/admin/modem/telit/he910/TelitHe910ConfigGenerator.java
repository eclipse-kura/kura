/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.telit.he910;

import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangePair;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;

public class TelitHe910ConfigGenerator implements ModemPppConfigGenerator {

    @Override
    public PppPeer getPppPeer(String deviceId, ModemConfig modemConfig, String logFile, String connectScript,
            String disconnectScript) {

        PppPeer pppPeer = new PppPeer();

        // default values
        pppPeer.setBaudRate(115200);
        pppPeer.setEnableDebug(true);
        pppPeer.setUseModemControlLines(true);
        pppPeer.setUseRtsCtsFlowControl(false);
        pppPeer.setLockSerialDevice(true);
        pppPeer.setPeerMustAuthenticateItself(false);
        pppPeer.setPeerToSupplyLocalIP(true);
        pppPeer.setAddDefaultRoute(true);
        pppPeer.setUsePeerDns(true);
        pppPeer.setAllowProxyArps(false);
        pppPeer.setAllowVanJacobsonTcpIpHdrCompression(false);
        pppPeer.setAllowVanJacobsonConnectionIDCompression(false);
        pppPeer.setAllowBsdCompression(false);
        pppPeer.setAllowDeflateCompression(false);
        pppPeer.setAllowMagic(false);
        pppPeer.setConnectDelay(1000);
        pppPeer.setLcpEchoInterval(0);

        // other config
        pppPeer.setLogfile(logFile);
        pppPeer.setProvider(deviceId);
        pppPeer.setPppUnitNumber(modemConfig.getPppNumber());
        pppPeer.setConnectScript(connectScript);
        pppPeer.setDisconnectScript(disconnectScript);
        pppPeer.setApn(modemConfig.getApn());
        pppPeer.setAuthType(modemConfig.getAuthType());
        pppPeer.setUsername(modemConfig.getUsername());
        pppPeer.setPassword(modemConfig.getPasswordAsPassword());
        pppPeer.setDialString(modemConfig.getDialString());
        pppPeer.setPersist(modemConfig.isPersist());
        pppPeer.setMaxFail(modemConfig.getMaxFail());
        pppPeer.setIdleTime(modemConfig.getIdle());
        pppPeer.setActiveFilter(modemConfig.getActiveFilter());
        pppPeer.setLcpEchoInterval(modemConfig.getLcpEchoInterval());
        pppPeer.setLcpEchoFailure(modemConfig.getLcpEchoFailure());

        return pppPeer;
    }

    @Override
    public ModemXchangeScript getConnectScript(ModemConfig modemConfig) {
        int pdpPid = 1;
        String apn = "";
        String dialString = "";

        if (modemConfig != null) {
            apn = modemConfig.getApn();
            dialString = modemConfig.getDialString();
            pdpPid = getPdpContextNumber(dialString);
        }

        ModemXchangeScript modemXchange = new ModemXchangeScript();
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"BUSY\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"VOICE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO CARRIER\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO DIALTONE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO DIAL TONE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"ERROR\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"+++ath\"", "\"\""));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"AT\"", "OK"));
        modemXchange.addmodemXchangePair(new ModemXchangePair(formPDPcontext(pdpPid, PdpType.IP, apn), "OK"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"\\d\\d\\d\"", "OK"));
        modemXchange.addmodemXchangePair(new ModemXchangePair(formDialString(dialString), "\"\""));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"\\c\"", "CONNECT"));

        return modemXchange;
    }

    @Override
    public ModemXchangeScript getDisconnectScript(ModemConfig modemConfig) {

        ModemXchangeScript modemXchange = new ModemXchangeScript();
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"BUSY\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"VOICE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO CARRIER\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO DIALTONE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"NO DIAL TONE\"", "ABORT"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("BREAK", "\"\""));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"+++ATH\"", "\"\""));

        return modemXchange;
    }

    private int getPdpContextNumber(String dialString) {
        return Integer.parseInt(dialString.substring("atd*99***".length(), dialString.length() - 1));
    }

    /*
     * This method forms dial string
     */
    private String formDialString(String dialString) {
        StringBuilder buf = new StringBuilder();
        buf.append('"');
        if (dialString != null) {
            buf.append(dialString);
        }
        buf.append('"');
        return buf.toString();
    }

    /*
     * This method forms PDP context
     * (e.g. AT+CGDCONT=<pid>,<pdp_type>,<apn>)
     */
    private String formPDPcontext(int pdpPid, PdpType pdpType, String apn) {

        StringBuilder pdpcontext = new StringBuilder(TelitHe910AtCommands.PDP_CONTEXT.getCommand());
        pdpcontext.append('=');
        pdpcontext.append(pdpPid);
        pdpcontext.append(',');
        pdpcontext.append('"');
        pdpcontext.append(pdpType.toString());
        pdpcontext.append('"');
        pdpcontext.append(',');
        pdpcontext.append('"');
        if (apn != null) {
            pdpcontext.append(apn);
        }
        pdpcontext.append('"');

        return pdpcontext.toString();
    }

}
