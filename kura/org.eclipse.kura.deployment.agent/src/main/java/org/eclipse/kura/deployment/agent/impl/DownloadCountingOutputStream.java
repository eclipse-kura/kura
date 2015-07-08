package org.eclipse.kura.deployment.agent.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.deployment.agent.DeploymentAgentService.DOWNLOAD_STATUS;
import org.eclipse.kura.deployment.agent.DeploymentPackageDownloadOptions;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;

public class DownloadCountingOutputStream extends CountingOutputStream {

	private static int PROP_RESOLUTION = 1024 * 4;
	private static int PROP_BUFFER_SIZE = 1024 * 4;
	private static int PROP_CONNECT_TIMEOUT = 5000;
	private static int PROP_READ_TIMEOUT = 6000;
	
	private final DeploymentPackageDownloadOptions options;
	// https://s3.amazonaws.com/kura-resources/dps/heater.dp

	private long totalBytes;
	private final EventAdmin m_eventAdmin;
	private final SslManagerService m_sslManagerService;

	InputStream is = null;

	private long m_currentStep = 0;

	public DownloadCountingOutputStream(OutputStream out, DeploymentPackageDownloadOptions options, EventAdmin m_eventAdmin, SslManagerService m_sslManagerService) {
		super(out);
		this.options = options;
		this.m_eventAdmin = m_eventAdmin;
		this.m_sslManagerService = m_sslManagerService;
		PROP_BUFFER_SIZE = options.getBlockSize();
		PROP_RESOLUTION = options.getNotifyBlockSize();
	}

	public void setResolution(int resolution) {
		PROP_RESOLUTION = resolution;
	}

	public void setBufferSize(int size) {
		PROP_BUFFER_SIZE = size;
	}
	
	public void setConnectTimeout(int timeout){
		PROP_CONNECT_TIMEOUT = timeout;
	}
	
	public void setReadTimeout(int timeout){
		PROP_READ_TIMEOUT = timeout;
	}

	public void startWork() throws KuraException {
		URL localUrl = null;
		try {

			postProgressEvent(options.getClientId(), 0, totalBytes, DOWNLOAD_STATUS.PROGRESS);
			
			localUrl = new URL(options.getDeployUrl());
			URLConnection urlConnection = localUrl.openConnection();
			urlConnection.setConnectTimeout(PROP_CONNECT_TIMEOUT);
			urlConnection.setReadTimeout(PROP_READ_TIMEOUT);

			try {
				if (urlConnection instanceof HttpsURLConnection) {
					((HttpsURLConnection) urlConnection).setSSLSocketFactory(m_sslManagerService.getSSLSocketFactory());
				} else if (!(urlConnection instanceof HttpURLConnection)){
					postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED);
					throw new KuraConnectException("Unsupported protocol!");
				}
			} catch (GeneralSecurityException e) {
				postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED);
				throw new KuraConnectException(e, "Unsupported protocol!");
			}

			is = localUrl.openStream();

			String s = urlConnection.getHeaderField("Content-Length");

			totalBytes = s != null ? Integer.parseInt(s) : -1;

			long numBytes = IOUtils.copyLarge(is, this, new byte[PROP_BUFFER_SIZE]);
			
			postProgressEvent(options.getClientId(), numBytes, totalBytes, DOWNLOAD_STATUS.COMPLETE);
			
		} catch (IOException e) {
			postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED);
			throw new KuraConnectException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			try {
				this.close();
			} catch (IOException e) {
			}
			localUrl = null;
		}

	}

	@Override
	protected void afterWrite(int n) throws IOException {
		super.afterWrite(n);
		if (getByteCount() > m_currentStep * PROP_RESOLUTION) {
			m_currentStep++;
			postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.PROGRESS);
		}
	}

	private void postProgressEvent(String clientId, long progress, long total, DOWNLOAD_STATUS status) {
		Map<String, Object> props = new HashMap<String, Object>();

		props.put(DeploymentAgentService.EVENT_CLIENT_ID, clientId);
		props.put(DeploymentAgentService.EVENT_TOTAL_SIZE, total);
		Long perc = Math.round((((Long)progress).doubleValue() / ((Long)total).doubleValue()) * 100 ); 
		props.put(DeploymentAgentService.EVENT_CURRENT_PROGRESS, perc.intValue());
		props.put(DeploymentAgentService.EVENT_PROGRESS_STATUS, status.getStatusString());
		

		EventProperties eventProps = new EventProperties(props);
		m_eventAdmin.postEvent(new Event(DeploymentAgentService.EVENT_PROGRESS_TOPIC, eventProps));
	}
}
