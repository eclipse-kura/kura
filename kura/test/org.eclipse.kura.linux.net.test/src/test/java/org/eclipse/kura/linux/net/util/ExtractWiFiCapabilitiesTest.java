package org.eclipse.kura.linux.net.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.junit.Test;

public class ExtractWiFiCapabilitiesTest {

    private String iwInfoOutput;
    private String iwListOutputPath;
    private int phyIndex;
    private String interfaceName;

    @Test
    public void checkDFSAndVHT() throws IOException, KuraException {

        givenIwInfoOutput("iw-wlan0-info.txt");
        givenIwOutput("iw-output-vht-dsf.txt");

        whenInterfaceNameIs("wlan0");
        whenPhyIndexIs(0);

        thenCapabilitiesAreDetected(Capability.DFS, Capability.VHT);

    }

    @Test
    public void checkCCMPAndWEP10AndWEP104() throws IOException, KuraException {

        givenIwInfoOutput("iw-wlan0-info.txt");
        givenIwOutput("iw-output-vht-dsf.txt");

        whenInterfaceNameIs("wlan0");
        whenPhyIndexIs(0);

        thenCapabilitiesAreDetected(Capability.CIPHER_CCMP, Capability.CIPHER_WEP104, Capability.CIPHER_WEP104);

    }

    private void givenIwInfoOutput(String iwInfoOutput) {
        this.iwInfoOutput = iwInfoOutput;
    }

    private void whenPhyIndexIs(int phyIndex) {
        this.phyIndex = phyIndex;
    }

    private void whenInterfaceNameIs(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    private void givenIwOutput(String iwListOutputPath) throws IOException {
        this.iwListOutputPath = iwListOutputPath;
    }

    private void thenCapabilitiesAreDetected(Capability... capabilities) throws KuraException, IOException {
        Set<Capability> foundCapabilities = IwCapabilityTool.probeCapabilities(this.interfaceName,
                commandExecutorMock());

        foundCapabilities.forEach(c -> assertTrue(foundCapabilities.contains(c)));

    }

    private CommandExecutorService commandExecutorMock() throws IOException {
        CommandStatus interfaceInfoStatus = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        interfaceInfoStatus.setOutputStream(loadFileToOutPutStream(iwInfoOutput));

        CommandStatus phyInfoStatus = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        phyInfoStatus.setOutputStream(loadFileToOutPutStream(iwListOutputPath));

        CommandExecutorService commandExecutorService = mock(CommandExecutorService.class);

        when(commandExecutorService.execute(new Command(new String[] { "iw", this.interfaceName, "info" })))
                .thenReturn(interfaceInfoStatus);
        when(commandExecutorService.execute(new Command(new String[] { "iw", "phy" + this.phyIndex, "info" })))
                .thenReturn(phyInfoStatus);

        return commandExecutorService;
    }

    private OutputStream loadFileToOutPutStream(String filename) throws IOException {
        InputStream is = new ByteArrayInputStream(IOUtil.readResource(filename).getBytes(StandardCharsets.UTF_8));
        OutputStream os = new ByteArrayOutputStream();

        IOUtils.copy(is, os);

        is.close();

        return os;
    }

}
