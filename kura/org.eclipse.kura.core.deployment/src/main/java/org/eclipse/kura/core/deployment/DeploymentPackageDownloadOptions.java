package org.eclipse.kura.core.deployment;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;

public class DeploymentPackageDownloadOptions {

	// Metrics in RESOURCE_DOWNLOAD
	public static final String METRIC_DEPLOY_URL = "dp.url";
	public static final String METRIC_DP_NAME = "dp.name";
	public static final String METRIC_DP_VERSION = "dp.version";
	public static final String METRIC_BLOCK_SIZE = "dp.http.block.size";
	public static final String METRIC_BLOCK_DELAY = "dp.http.block.delay";
	public static final String METRIC_HTTP_TIMEOUT = "dp.http.timeout";
	public static final String METRIC_HTTP_RESUME = "dp.http.resume";
	public static final String METRIC_HTTP_USER = "dp.http.username";
	public static final String METRIC_HTTP_PASSWORD = "dp.http.password";
	public static final String METRIC_DP_INSTALL = "dp.install";
	public static final String METRIC_DP_POSTINSTALL = "dp.postinst";
	public static final String METRIC_DP_DELETE = "dp.delete";
	public static final String METRIC_DP_REBOOT = "dp.reboot";
	public static final String METRIC_DP_REBOOT_DELAY = "dp.reboot.delay";
	public static final String METRIC_DP_CLIENT_ID = "client.id";
	public static final String METRIC_DP_NOTIFY_BLOCK_SIZE = "dp.http.notify.block.size";
	public static final String METRIC_JOB_ID = "job.id";
	public static final String METRIC_DP_HTTP_FORCE_DOWNLOAD = "dp.http.force.download";

	private final String deployUrl;
	private final String dpName;
	private final String dpVersion;
	private int blockSize = 1024 * 4;
	private int notifyBlockSize = 1024 * 256;
	private int blockDelay = 0;
	private int timeout = 4000;
	private boolean resume = false;
	private boolean install = true;
	private boolean postInst = false;
	private boolean delete = false;
	private boolean reboot = false;
	private int rebootDelay = 0;
	private String username = null;
	private String password = null;
	private boolean forceDownload = false;

	private String clientId = "";
	private String requestClientId = "";
	private Long jobId = null;

	public DeploymentPackageDownloadOptions(String deployUrl, String dpName, String dpVersion) {
		super();
		this.deployUrl = deployUrl;
		this.dpName = dpName;
		this.dpVersion = dpVersion;
	}

	public DeploymentPackageDownloadOptions(KuraPayload request) throws KuraException {

		deployUrl = (String) request.getMetric(METRIC_DEPLOY_URL);
		if (deployUrl == null) {
			throw new KuraInvalidMessageException("Missing deployment package URL!");
		}

		dpName = (String) request.getMetric(METRIC_DP_NAME);
		if (dpName == null) {
			throw new KuraInvalidMessageException("Missing deployment package name!");
		}

		dpVersion = (String) request.getMetric(METRIC_DP_VERSION);
		if (dpVersion == null) {
			throw new KuraInvalidMessageException("Missing deployment package version!");
		}
		
		jobId = (Long) request.getMetric(METRIC_JOB_ID);
		if (jobId == null) {
			throw new KuraInvalidMessageException("Missing jobId!");
		}

		try {
			Object metric = request.getMetric(METRIC_BLOCK_SIZE);
			if (metric != null) {
				blockSize = (Integer) metric;
			}
			metric = request.getMetric(METRIC_BLOCK_DELAY);
			if (metric != null) {
				blockDelay = (Integer) metric;
			}
			metric = request.getMetric(METRIC_HTTP_TIMEOUT);
			if (metric != null) {
				timeout = (Integer) metric;
			}
			metric = request.getMetric(METRIC_HTTP_RESUME);
			if (metric != null) {
				resume = (Boolean) metric;
			}
			metric = request.getMetric(METRIC_HTTP_USER);
			if (metric != null) {
				username = (String) metric;
			}
			metric = request.getMetric(METRIC_HTTP_PASSWORD);
			if (metric != null) {
				password = (String) metric;
			}
			metric = request.getMetric(METRIC_DP_INSTALL);
			if (metric != null) {
				install = (Boolean) metric;
			}
			metric = request.getMetric(METRIC_DP_POSTINSTALL);
			if (metric != null) {
				postInst = (Boolean) metric;
			}
			metric = request.getMetric(METRIC_DP_DELETE);
			if (metric != null) {
				delete = (Boolean) metric;
			}
			metric = request.getMetric(METRIC_DP_REBOOT);
			if (metric != null) {
				reboot = (Boolean) metric;
			}
			metric = request.getMetric(METRIC_DP_REBOOT_DELAY);
			if (metric != null) {
				rebootDelay = (Integer) metric;
			}
			metric = request.getMetric(METRIC_DP_HTTP_FORCE_DOWNLOAD);
			if (metric != null) {
				forceDownload = (Boolean) metric;
			}

			metric = request.getMetric(METRIC_DP_NOTIFY_BLOCK_SIZE);
			if (metric != null) {
				notifyBlockSize = (Integer) metric;
			}

			metric = request.getMetric(KuraRequestPayload.REQUESTER_CLIENT_ID);
			if (metric != null) {
				clientId = (String) metric;
			}

		} catch (Exception ex) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
		}
	}

	public int getNotifyBlockSize() {
		return notifyBlockSize;
	}

	public void setNotifyBlockSize(int notifyBlockSize) {
		this.notifyBlockSize = notifyBlockSize;
	}

	public String getDeployUrl() {
		return deployUrl;
	}

	public String getDpName() {
		return dpName;
	}

	public String getDpVersion() {
		return dpVersion;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public int getBlockDelay() {
		return blockDelay;
	}

	public void setBlockDelay(int blockDelay) {
		this.blockDelay = blockDelay;
	}

	public int getTimeout() {
		return timeout;
	}
	
	public long getJobId() {
		return jobId;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isResume() {
		return resume;
	}

	public void setResume(boolean resume) {
		this.resume = resume;
	}

	public boolean isInstall() {
		return install;
	}

	public void setInstall(boolean install) {
		this.install = install;
	}

	public boolean isPostInst() {
		return postInst;
	}

	public void setPostInst(boolean postInst) {
		this.postInst = postInst;
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public boolean isReboot() {
		return reboot;
	}

	public void setReboot(boolean reboot) {
		this.reboot = reboot;
	}

	public int getRebootDelay() {
		return rebootDelay;
	}
	
	public boolean isDownloadForced() {
		return forceDownload;
	}
	
	public void setDownloadForced(boolean forceDownload) {
		this.forceDownload = forceDownload;
	}

	public void setRebootDelay(int rebootDelay) {
		this.rebootDelay = rebootDelay;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRequestClientId() {
		return requestClientId;
	}

	public void setRequestClientId(String requestClientId) {
		this.requestClientId = requestClientId;
	}

}
