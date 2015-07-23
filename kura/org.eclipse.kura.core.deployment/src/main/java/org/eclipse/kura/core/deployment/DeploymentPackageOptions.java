/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.core.deployment;

public abstract class DeploymentPackageOptions {

	// Metrics
	
	public static final String METRIC_DP_NAME = "dp.name";
	public static final String METRIC_DP_VERSION = "dp.version";
	public static final String METRIC_DP_POSTINSTALL = "dp.postinst";
	public static final String METRIC_DP_DELETE = "dp.delete";
	public static final String METRIC_DP_REBOOT = "dp.reboot";
	public static final String METRIC_DP_REBOOT_DELAY = "dp.reboot.delay";
	public static final String METRIC_DP_CLIENT_ID = "client.id";
	public static final String METRIC_JOB_ID = "job.id";
	public static final String METRIC_DP_INSTALL_SYSTEM_UPDATE = "dp.install.system.update";

	
	private String dpName;
	private String dpVersion;
	private boolean resume = false;
	private boolean install = true;
	private boolean postInst = false;
	private boolean delete = false;
	private boolean reboot = false;
	private int rebootDelay = 0;
	

	private String clientId = "";
	private String requestClientId = "";
	private Long jobId = null;
	private Boolean systemUpdate = false;

	public DeploymentPackageOptions(String dpName, String dpVersion) {
		this.dpName = dpName;
		this.dpVersion = dpVersion;
	}

	public String getDpName() {
		return dpName;
	}
	
	public void setDpName(String dpName) {
		this.dpName = dpName;
	}

	public String getDpVersion() {
		return dpVersion;
	}
	
	public void setDpVersion(String dpVersion) {
		this.dpVersion = dpVersion;
	}
	
	public Long getJobId() {
		return jobId;
	}
	
	public void setJobId(long jobId) {
		this.jobId = jobId;
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

	public void setRebootDelay(int rebootDelay) {
		this.rebootDelay = rebootDelay;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getRequestClientId() {
		return requestClientId;
	}

	public void setRequestClientId(String requestClientId) {
		this.requestClientId = requestClientId;
	}
	
	public void setSystemUpdate(Boolean systemUpdate) {
		this.systemUpdate = systemUpdate;
	}

	public Boolean getSystemUpdate() {
		return systemUpdate;
	}

}
