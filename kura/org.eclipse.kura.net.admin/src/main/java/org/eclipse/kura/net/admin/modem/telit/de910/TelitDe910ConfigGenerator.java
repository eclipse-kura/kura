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
package org.eclipse.kura.net.admin.modem.telit.de910;

import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangePair;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;

public class TelitDe910ConfigGenerator implements ModemPppConfigGenerator {

    @Override
    public PppPeer getPppPeer(String deviceId, ModemConfig modemConfig, String logFile, String connectScript,
            String disconnectScript) {

        PppPeer pppPeer = new PppPeer();

        // default values
        pppPeer.setBaudRate(921600);
        pppPeer.setEnableDebug(true);
        pppPeer.setUseRtsCtsFlowControl(true);
        pppPeer.setLockSerialDevice(true);
        pppPeer.setPeerMustAuthenticateItself(false);
        pppPeer.setAddDefaultRoute(true);
        pppPeer.setUsePeerDns(true);
        pppPeer.setAllowProxyArps(true);
        pppPeer.setAllowVanJacobsonTcpIpHdrCompression(true);
        pppPeer.setAllowVanJacobsonConnectionIDCompression(true);
        pppPeer.setAllowBsdCompression(true);
        pppPeer.setAllowDeflateCompression(true);
        pppPeer.setAllowMagic(true);
        pppPeer.setConnectDelay(10000);
        pppPeer.setLcpEchoFailure(4);
        pppPeer.setLcpEchoInterval(65535);

        pppPeer.setLogfile(logFile);
        pppPeer.setProvider(deviceId);
        pppPeer.setPppUnitNumber(modemConfig.getPppNumber());
        pppPeer.setConnectScript(connectScript);
        pppPeer.setDisconnectScript(disconnectScript);
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

        String dialString = "";
        if (modemConfig != null) {
            dialString = modemConfig.getDialString();
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
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"ATE1V1&F&D2&C1&C2S0=0\"", "OK"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"ATE1V1\"", "OK"));
        modemXchange.addmodemXchangePair(new ModemXchangePair("\"ATS7=60\"", "OK"));
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

    private String formDialString(String dialString) {
        StringBuilder buf = new StringBuilder();
        buf.append('"');
        if (dialString != null) {
            buf.append(dialString);
        }
        buf.append('"');
        return buf.toString();
    }
}
