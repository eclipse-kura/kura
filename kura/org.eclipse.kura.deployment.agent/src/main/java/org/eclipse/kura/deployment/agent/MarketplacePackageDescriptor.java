package org.eclipse.kura.deployment.agent;

import java.io.Serializable;

public class MarketplacePackageDescriptor implements Serializable {

    private String nodeId;
    private String url;
    private String dpUrl;
    private String minKuraVersion;
    private String maxKuraVersion;
    private String currentKuraVersion;
    private boolean isCompatible;

    private MarketplacePackageDescriptor(MarketplacePackageDescriptorBuilder builder) {
        this.nodeId = builder.nodeId;
        this.url = builder.url;
        this.dpUrl = builder.dpUrl;
        this.minKuraVersion = builder.minKuraVersion;
        this.maxKuraVersion = builder.maxKuraVersion;
        this.currentKuraVersion = builder.currentKuraVersion;
        this.isCompatible = builder.isCompatible;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getUrl() {
        return url;
    }

    public String getDpUrl() {
        return dpUrl;
    }

    public String getMinKuraVersion() {
        return minKuraVersion;
    }

    public String getMaxKuraVersion() {
        return maxKuraVersion;
    }

    public String getCurrentKuraVersion() {
        return currentKuraVersion;
    }

    public boolean isCompatible() {
        return isCompatible;
    }

    public static MarketplacePackageDescriptorBuilder builder() {
        return new MarketplacePackageDescriptorBuilder();
    }

    // Builder
    public static class MarketplacePackageDescriptorBuilder {

        private String nodeId;
        private String url;
        private String dpUrl;
        private String minKuraVersion;
        private String maxKuraVersion;
        private String currentKuraVersion;
        private boolean isCompatible = false;

        public MarketplacePackageDescriptorBuilder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public MarketplacePackageDescriptorBuilder url(String url) {
            this.url = url;
            return this;
        }

        public MarketplacePackageDescriptorBuilder dpUrl(String dpUrl) {
            this.dpUrl = dpUrl;
            return this;
        }

        public MarketplacePackageDescriptorBuilder minKuraVersion(String minKuraVersion) {
            this.minKuraVersion = minKuraVersion;
            return this;
        }

        public MarketplacePackageDescriptorBuilder maxKuraVersion(String maxKuraVersion) {
            this.maxKuraVersion = maxKuraVersion;
            return this;
        }

        public MarketplacePackageDescriptorBuilder currentKuraVersion(String currentKuraVersion) {
            this.currentKuraVersion = currentKuraVersion;
            return this;
        }

        public MarketplacePackageDescriptorBuilder isCompatible(boolean isCompatible) {
            this.isCompatible = isCompatible;
            return this;
        }

        public MarketplacePackageDescriptor build() {
            return new MarketplacePackageDescriptor(this);
        }
    }
}
