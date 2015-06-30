package com.eurotech.denali.comm;

import java.util.List;
import java.util.regex.Pattern;

import org.testng.Reporter;

import com.eurotech.denali.common.Application;
import com.eurotech.denali.util.Constants;
import com.eurotech.denali.util.JvmUtil;

public class ConsoleFactory extends ConsoleSession {

	private static ConsoleFactory factory;
	private static final int TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 60000;
	private static final int OSGI_READ_TIMEOUT = 90000;
	private static final String REBOOT_COMMAND = "sudo reboot";
	private static final String OSGI_CONSOLE_CONNECT_COMMAND = "telnet localhost 5002";
	private static final String OSGI_LIST_BUNDLES_COMMAND = "ss";
	private static final String OSGI_CONSOLE_DISCONNECT_COMMAND = "disconnect";
	private static final String IF_CONFIG_ETH0_COMMAND = "ifconfig eth0";
	private static final String IF_CONFIG_DOWN_COMMAND = "sudo ifdown eth0";
	private static final String IF_CONFIG_ETH1_COMMAND = "ifconfig eth1";
	private static final String IF_CONFIG_WLAN0_COMMAND = "ifconfig wlan0";
	private static final String ROUTE_COMMAND = "route";
	private static final String DNS_SERVER_COMMAND = "cat /etc/resolv.conf";
	private static final String PING_COMMAND = "ping google.com -c 3";
	private static final String NET_ETH0_SHELL_SCRIPT_COMMAND = "sh /tmp/net_disconnect_eth0_reboot.sh";
	private static final String IF_CONFIG_LOG_COMMAND = "cat ifconfig.txt";
	private static final String ROUTE_LOG_COMMAND = "cat route.txt";
	private static final String SERVICE_STATUS_COMMAND = "ps -ef";
	private static final Pattern OSGI_PROMPT_PATTERN = Pattern.compile("osgi>",
			Pattern.MULTILINE);
	private static final Pattern IPADDRESS_PATTERN = Pattern.compile(
			"(?<=inet\\saddr:)(.*?)(?=Bcast)", Pattern.DOTALL);
	private static final Pattern ETH0_SUBNET_MASK_PATTERN = Pattern
			.compile("(?<=Mask\\:)(.*)");
	private static final Pattern GATEWAY_ETH0_PATTERN = Pattern.compile(
			"(?<=default)(\\s+)((\\d{1,3}\\.?){4})(?=(.*)eth0)",
			Pattern.MULTILINE);
	private static final Pattern GATEWAY_ETH1_PATTERN = Pattern.compile(
			"(?<=default)(\\s+)((\\d{1,3}\\.?){4})(?=(.*)eth1)",
			Pattern.MULTILINE);
	private static final Pattern GATEWAY_WLAN0_PATTERN = Pattern.compile(
			"(?<=default)(\\s+)((\\d{1,3}\\.?){4})(?=(.*)wlan0)",
			Pattern.MULTILINE);
	private static final Pattern NETWORK_ETH0_PATTERN = Pattern.compile(
			"^((\\d{1,3}\\.?){3})(?=(.*)eth0)", Pattern.MULTILINE);
	private static final Pattern DNS_SERVERS_PATTERN = Pattern.compile(
			"(\\d{1,3}\\.?){4}", Pattern.MULTILINE);
	private static final Pattern PING_STATISTICS_PATTERN = Pattern.compile(
			"\\d+\\%\\spacket\\sloss", Pattern.DOTALL);
	private static final Pattern OSGI_WEB_BUNDLE_PATTERN = Pattern
			.compile(
					"ACTIVE\\s+(org|com)\\.(eclipse|eurotech)\\.(kura|framework)\\.web_\\d+\\.\\d+\\.\\d+",
					Pattern.MULTILINE);
	private static final Pattern DHCPD_SERVICE_PATTERN = Pattern.compile(
			"\\/etc\\/dhcpd-eth0.conf", Pattern.MULTILINE);
	private static final Pattern BIND_SERVICE_PATTERN = Pattern.compile(
			"\\/usr\\/sbin\\/named", Pattern.MULTILINE);

	private static String denaliHost = "";
	private static String denaliUsername = "";
	private static String denaliPassword = "";

	private ConsoleFactory() {
		super(denaliHost, denaliUsername, denaliPassword);
	}

	public static ConsoleFactory getInstance() {
		denaliHost = Application.getDeviceIP();
		denaliUsername = Application.getDeviceUsername();
		denaliPassword = Application.getDevicePassword();
		factory = new ConsoleFactory();
		return factory;
	}

	public static ConsoleFactory getInstance(String hostIP) {
		denaliHost = hostIP;
		denaliUsername = Application.getDeviceUsername();
		denaliPassword = Application.getDevicePassword();
		factory = new ConsoleFactory();
		return factory;
	}

	public void sendEth0DownCommand() {
		send(IF_CONFIG_DOWN_COMMAND);
	}

	public void rebootDevice() {
		send(REBOOT_COMMAND);
		readString(TIMEOUT);
	}

	public String getIfConfigLog() {
		send(IF_CONFIG_LOG_COMMAND);
		return readString(TIMEOUT);
	}

	public String getRouteLog() {
		send(ROUTE_LOG_COMMAND);
		return readString(TIMEOUT);
	}

	private String sendIfConfigEth0Command() {
		send(IF_CONFIG_ETH0_COMMAND);
		return readString(TIMEOUT);
	}

	private String sendIfConfigEth1Command() {
		send(IF_CONFIG_ETH1_COMMAND);
		return readString(TIMEOUT);
	}

	private String sendIfConfigWlan0Command() {
		send(IF_CONFIG_WLAN0_COMMAND);
		return readString(TIMEOUT);
	}

	private String sendRouteCommand() {
		send(ROUTE_COMMAND);
		return readUntilPrompt(READ_TIMEOUT);
	}

	private String sendDNSServerCommand() {
		send(DNS_SERVER_COMMAND);
		return readUntilPrompt(READ_TIMEOUT);
	}

	private String sendDHCPDServiceCommand() {
		send(SERVICE_STATUS_COMMAND);
		return readUntilPrompt(READ_TIMEOUT);
	}

	private String sendBindServiceCommand() {
		send(SERVICE_STATUS_COMMAND);
		return readUntilPrompt(READ_TIMEOUT);
	}

	public boolean isDHCPDServiceRunning() {
		String result = sendDHCPDServiceCommand();
		System.out.println(result);
		return JvmUtil.isPatternTextPresent(DHCPD_SERVICE_PATTERN, result);
	}

	public boolean isBindServiceRunning() {
		String result = sendBindServiceCommand();
		System.out.println(result);
		return JvmUtil.isPatternTextPresent(BIND_SERVICE_PATTERN, result);
	}

	public String getEth0IPAddress() {
		String result = sendIfConfigEth0Command();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(IPADDRESS_PATTERN, result);
	}

	public String getEth1IPAddress() {
		String result = sendIfConfigEth1Command();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(IPADDRESS_PATTERN, result);
	}

	public String getWlan0IPAddress() {
		String result = sendIfConfigWlan0Command();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(IPADDRESS_PATTERN, result);
	}

	public String getEth0SubnetMask() {
		String result = sendIfConfigEth0Command();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(ETH0_SUBNET_MASK_PATTERN, result);
	}

	public String getEth0GatewayEntry() {
		String result = sendRouteCommand();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(GATEWAY_ETH0_PATTERN, result);
	}

	public String getEth1GatewayEntry() {
		String result = sendRouteCommand();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(GATEWAY_ETH1_PATTERN, result);
	}

	public String getWlan0GatewayEntry() {
		String result = sendRouteCommand();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(GATEWAY_WLAN0_PATTERN, result);
	}

	public String getEth0NetworkEntry() {
		String result = sendRouteCommand();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternText(NETWORK_ETH0_PATTERN, result);
	}

	public List<String> getDNSServers() {
		String result = sendDNSServerCommand();
		Reporter.log(result, Constants.ENV_OUTPUT);
		return JvmUtil.getPatternList(DNS_SERVERS_PATTERN, result);
	}

	public boolean verifyPingExternalAddress() {
		send(PING_COMMAND);
		String result = readUntilPrompt(READ_TIMEOUT);
		Reporter.log(result, Constants.ENV_OUTPUT);
		return (JvmUtil.getPatternText(PING_STATISTICS_PATTERN, result)
				.contains("0")) ? true : false;
	}

	public boolean verifyConnectionStatus() {
		boolean status = true;
		try {
			Reporter.log("Trying to connect " + denaliHost + " through SSH...",
					Constants.ENV_OUTPUT);
			connectViaSSH();
			Reporter.log("SSH connection successful... ", Constants.ENV_OUTPUT);
		} catch (Exception e) {
			status = false;
		}
		return status;
	}

	public boolean verfiyKuraWebBundleStatus() {
		boolean status = true;
		try {
			Reporter.log("Trying to connect " + denaliHost + " through SSH...",
					Constants.ENV_OUTPUT);
			connectViaSSH();
			Reporter.log("SSH connection successful... ", Constants.ENV_OUTPUT);
			if (!isOSGIServiceActive()) {
				Reporter.log("Not connected to OSGI console...",
						Constants.ENV_OUTPUT);
				return false;
			}
			status = isWebBundleActive();
			disconnectOSGIConsole();
		} catch (Exception e) {
			status = false;
			Reporter.log("Connecting to " + denaliHost
					+ " through SSH failed...", Constants.ENV_OUTPUT);
		} finally {
			close();
		}
		return status;
	}

	public void executeDisconnectEth0AndRebootShellScript() {
		send(NET_ETH0_SHELL_SCRIPT_COMMAND);
		JvmUtil.idle(10);
	}

	private boolean isOSGIServiceActive() {
		send(OSGI_CONSOLE_CONNECT_COMMAND);
		return waitUntilPrompt(OSGI_PROMPT_PATTERN, OSGI_READ_TIMEOUT);
	}

	private boolean isWebBundleActive() {
		boolean result = false;
		send(OSGI_LIST_BUNDLES_COMMAND);
		String bundleList = readString(TIMEOUT);
		Reporter.log(bundleList, Constants.ENV_OUTPUT);
		result = JvmUtil.isPatternTextPresent(OSGI_WEB_BUNDLE_PATTERN,
				bundleList);
		return result;
	}

	private void disconnectOSGIConsole() {
		send(OSGI_CONSOLE_DISCONNECT_COMMAND);
		readString(TIMEOUT);
		send(Constants.EMPTY);
		readString(TIMEOUT);
	}
}
