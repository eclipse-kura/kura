/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.telit.le910v2;

import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangePair;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;

public class TelitLe910v2ConfigGenerator implements ModemPppConfigGenerator {

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
        /*
         * We are using PDP context 2 for this modem because Telit documentation reports
         * that setting a user defined APN on PDP context 1 might
         * cause issues with some 4G operators (see section 4.1.40.1. of
         * http://www.telit.com/fileadmin/user_upload/products/Downloads/3G/Telit_Modules_Software_User_Guide_2G3G4G_r19
         * .pdf).
         * The dial string is hardcoded to use context 2, dial string entered by the user
         * in the Web UI will be ignored.
         */
        final int pdpPid = 2;
        final String dialString = "atd*99***2#";

        String apn = "";

        if (modemConfig != null) {
            apn = modemConfig.getApn();
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

    /*
     * This method forms dial string
     */
    private String formDialString(String dialString) {
        StringBuffer buf = new StringBuffer();
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

        StringBuffer pdpcontext = new StringBuffer(TelitHe910AtCommands.pdpContext.getCommand());
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
