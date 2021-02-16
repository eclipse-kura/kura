package org.eclipse.kura.system;

public class SystemPackageInfo {

    private String name;
    private String version;

    public SystemPackageInfo(String name) {
        super();
        this.name = name;
    }

    public SystemPackageInfo(String name, String version) {
        this(name);
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
