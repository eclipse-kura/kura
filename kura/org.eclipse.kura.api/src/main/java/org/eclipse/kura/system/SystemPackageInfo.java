package org.eclipse.kura.system;

public class SystemPackageInfo {

    private String name;
    private String version;
    private String type;

    public SystemPackageInfo(String name) {
        super();
        this.name = name;
    }

    public SystemPackageInfo(String name, String version, String type) {
        this(name);
        this.version = version;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
