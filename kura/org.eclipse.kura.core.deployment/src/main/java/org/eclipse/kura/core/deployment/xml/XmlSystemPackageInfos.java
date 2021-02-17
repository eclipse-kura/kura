package org.eclipse.kura.core.deployment.xml;

import java.util.List;

import org.eclipse.kura.system.SystemPackageInfo;

public class XmlSystemPackageInfos {

    private List<SystemPackageInfo> packages;

    public XmlSystemPackageInfos(List<SystemPackageInfo> packages) {
        this.packages = packages;
    }

    public List<SystemPackageInfo> getSystemPackages() {
        return this.packages;
    }

    public void setSystemPackageInfos(List<SystemPackageInfo> packages) {
        this.packages = packages;
    }

}
