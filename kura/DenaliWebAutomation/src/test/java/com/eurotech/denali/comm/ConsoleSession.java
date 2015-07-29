package com.eurotech.denali.comm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import com.eurotech.denali.util.JvmUtil;
import com.google.common.base.Strings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class ConsoleSession {

	private static final int CHANNEL_CONNECTION_TIMEOUT_IN_MILLIS = 3000;
	private static final int SESSION_CONNECTION_TIMEOUT_IN_MILLIS = 30000;
	private static final int WAIT_TIME_IN_MILLIS = 5000;
	private static final Pattern PROMPT_PATTERN = Pattern.compile(
			"(\\:\\s?\\~\\s?(\\#|\\$)\\s?)$|(osgi>)", Pattern.MULTILINE);

	private String denaliHost;
	private String denaliUserName;
	private String denaliPassword;
	private Session session;
	private Channel channel;
	private ChannelSftp sftpChannel;
	private PrintWriter writeToConsole;
	private InputStreamReader readFromConsole;

	private StringBuilder log = new StringBuilder(2000);

	public ConsoleSession(String host, String maintenanceUserName,
			String maintenanceUserPassword) {
		this.denaliHost = host;
		this.denaliUserName = maintenanceUserName;
		this.denaliPassword = maintenanceUserPassword;
	}

	public void connectShell() {
		try {
			connectViaSSH();
		} catch (JSchException | IOException e) {
			throw new RuntimeException(String.format(
					"Unable to connect %s@%s%n", denaliUserName, denaliHost)
					+ e);
		}
	}

	public void disconnectShell() {
		if (channel != null) {
			channel.disconnect();
		}
		log.append(String.format("Shell disconnected from %s@%s%n",
				denaliUserName, denaliHost));
	}

	public void disconnectSFTP() {
		if (sftpChannel != null) {
			sftpChannel.disconnect();
		}
		log.append(String.format("SFTP disconnected from %s@%s%n",
				denaliUserName, denaliHost));
	}

	public void disconnectSession() {
		if (session != null) {
			session.disconnect();
		}
		log.append(String.format("Session disconnected from %s@%s%n",
				denaliUserName, denaliHost));
	}

	public String readString(int timeoutInMillis) {
		if (readFromConsole == null) {
			throw new IllegalStateException("No open channel");
		}
		try {
			Thread.sleep(timeoutInMillis);
		} catch (InterruptedException e1) {
			throw new RuntimeException(e1);
		}

		StringWriter writer = new StringWriter();
		char[] buffer = new char[4096];
		try {
			if (!readFromConsole.ready()) {
				return null;
			}
			while (readFromConsole.ready()) {
				int n = readFromConsole.read(buffer);
				writer.write(buffer, 0, n);
			}
			log.append(String.format("Read from %s@%s%n", denaliUserName,
					denaliHost));
			log.append(String.format("%s%n", writer.toString()));
			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void send(String command) {
		if (writeToConsole == null) {
			throw new IllegalStateException("No open channel");
		}
		writeToConsole.println(command);
		writeToConsole.flush();
		log.append(String.format("Sent to %s@%s%n", denaliUserName, denaliHost));
		log.append(String.format("%s%n", command));
		// Read the command.
		try {
			readFromConsole.read(new char[command.length() + 1]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isShellConnected() {
		if (channel == null) {
			return false;
		}
		return channel.isConnected();
	}

	public boolean isSFTPConnected() {
		if (sftpChannel == null) {
			return false;
		}
		return sftpChannel.isConnected();
	}

	public String readUntilPrompt(int maxWaitTimeInMilliSec) {
		int counter = 0;
		StringBuilder returnString = new StringBuilder();
		while (counter < maxWaitTimeInMilliSec) {
			String outputString = readString(WAIT_TIME_IN_MILLIS);
			returnString.append(Strings.nullToEmpty(outputString));
			if (PROMPT_PATTERN.matcher(Strings.nullToEmpty(outputString))
					.find()) {
				break;
			}
			counter += WAIT_TIME_IN_MILLIS;
		}
		return returnString.toString();
	}

	public boolean waitUntilPrompt(Pattern pattern, int maxWaitTimeInMilliSec) {
		String result = readUntilPrompt(maxWaitTimeInMilliSec);
		return pattern.matcher(result).find() ? true : false;
	}

	public void close() {
		while (isShellConnected()) {
			disconnectShell();
			JvmUtil.idle(2);
		}
		while (isSFTPConnected()) {
			disconnectSFTP();
			JvmUtil.idle(2);
		}
		disconnectSession();
		JvmUtil.idle(2);
	}

	public void connectViaSSH() throws JSchException, IOException {
		JSch jsch = new JSch();
		session = jsch.getSession(denaliUserName, denaliHost);
		session.setPassword(denaliPassword);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect(SESSION_CONNECTION_TIMEOUT_IN_MILLIS);
		channel = session.openChannel("shell");
		System.setProperty("line.separator", "\n");
		writeToConsole = new PrintWriter(channel.getOutputStream());
		readFromConsole = new InputStreamReader(channel.getInputStream());
		channel.connect(CHANNEL_CONNECTION_TIMEOUT_IN_MILLIS);
		log.append(String.format("Connected to %s@%s%n", denaliUserName,
				denaliHost));
	}

	public void connectViaSFTP() {
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(denaliUserName, denaliHost);
			session.setPassword(denaliPassword);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(SESSION_CONNECTION_TIMEOUT_IN_MILLIS);
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.setInputStream(System.in);
			sftpChannel.setOutputStream(System.out);
			sftpChannel.connect(SESSION_CONNECTION_TIMEOUT_IN_MILLIS);
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"Unable to connect %s@%s%n", denaliUserName, denaliHost)
					+ e);
		}
	}

	public void transferFile(String sourcePath, String destPath) {
		try {
			sftpChannel.put(sourcePath, destPath);
		} catch (SftpException e) {
			throw new RuntimeException(String.format("Unable to transfer file")
					+ e);
		}
	}
}
