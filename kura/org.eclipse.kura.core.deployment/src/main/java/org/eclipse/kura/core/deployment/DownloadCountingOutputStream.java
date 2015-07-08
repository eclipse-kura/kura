package org.eclipse.kura.core.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class DownloadCountingOutputStream extends CountingOutputStream {

	private static int PROP_RESOLUTION = 1024 * 4;
	private static int PROP_BUFFER_SIZE = 1024 * 4;
	private static int PROP_CONNECT_TIMEOUT = 5000;
	private static int PROP_READ_TIMEOUT = 6000;

	private final DeploymentPackageDownloadOptions options;
	// https://s3.amazonaws.com/kura-resources/dps/heater.dp

	private long totalBytes;

	private final SslManagerService m_sslManagerService;

	private final ProgressListener pl;

	InputStream is = null;

	private long m_currentStep = 0;
	
	private ExecutorService executor;
	private Future<Void> future;

	public DownloadCountingOutputStream(OutputStream out, DeploymentPackageDownloadOptions options, ProgressListener callback,
			SslManagerService m_sslManagerService) {
		super(out);
		this.options = options;
		this.m_sslManagerService = m_sslManagerService;
		this.pl = callback;
		PROP_BUFFER_SIZE = options.getBlockSize();
		PROP_RESOLUTION = options.getNotifyBlockSize();
	}

	public void setResolution(int resolution) {
		PROP_RESOLUTION = resolution;
	}

	public void setBufferSize(int size) {
		PROP_BUFFER_SIZE = size;
	}

	public void setConnectTimeout(int timeout) {
		PROP_CONNECT_TIMEOUT = timeout;
	}

	public void setReadTimeout(int timeout) {
		PROP_READ_TIMEOUT = timeout;
	}

	public void cancelDownload() throws Exception{
		if(executor != null){
			if(future != null){
				future.cancel(true);
				executor.shutdownNow();
				
				postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED);
			}
		}
	}
	
	public void startWork() throws KuraException {
		
		executor = Executors.newSingleThreadExecutor();
		
		future = executor.submit(new Callable<Void>(){

			@Override
			public Void call() throws Exception {
				URL localUrl = null;
				boolean shouldAuthenticate = false;
				try {

					postProgressEvent(options.getClientId(), 0, totalBytes, DOWNLOAD_STATUS.PROGRESS);

					shouldAuthenticate = (options.getUsername() != null) && (options.getPassword() != null)
							&& !(options.getUsername().trim().isEmpty() && !(options.getPassword().trim().isEmpty()));

					if (shouldAuthenticate) {
						Authenticator.setDefault(new Authenticator() {
							protected PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(options.getUsername(), options.getPassword().toCharArray());
							}
						});
					}

					localUrl = new URL(options.getDeployUrl());
					URLConnection urlConnection = localUrl.openConnection();
					urlConnection.setConnectTimeout(PROP_CONNECT_TIMEOUT);
					urlConnection.setReadTimeout(PROP_READ_TIMEOUT);

					try {
						if (urlConnection instanceof HttpsURLConnection) {
							((HttpsURLConnection) urlConnection).setSSLSocketFactory(m_sslManagerService.getSSLSocketFactory());
						} else if (!(urlConnection instanceof HttpURLConnection)) {
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

					long numBytes = IOUtils.copyLarge(is, DownloadCountingOutputStream.this, new byte[PROP_BUFFER_SIZE]);

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
						DownloadCountingOutputStream.this.close();
					} catch (IOException e) {
					}
					localUrl = null;
					if (shouldAuthenticate) {
						Authenticator.setDefault(null);
					}
				}
				
				return null;
			}
			
		});
		

		try{
			future.get();
		}catch(ExecutionException ex){
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
		}catch(InterruptedException ex){
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
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
		Long perc = Math.round((((Long) progress).doubleValue() / ((Long) total).doubleValue()) * 100);
		pl.progressChanged(new ProgressEvent(this, options.getRequestClientId(), clientId, ((Long) total).intValue(), ((Long) perc).intValue(), status
				.getStatusString()));
	}

}
