package org.eclipse.kura.web.client.ui.wires;

import java.util.List;
import java.util.Optional;

public class SnapshotDownloadOptions {

    private String format;
    private Optional<List<String>> selectedPids;

    public SnapshotDownloadOptions(String format, Optional<List<String>> selectedPids) {
        super();
        this.format = format;
        this.selectedPids = selectedPids;
    }

    public SnapshotDownloadOptions(String format) {
        super();
        this.format = format;
        this.selectedPids = Optional.empty();
    }

    public String getFormat() {
        return this.format;
    }

    public Optional<List<String>> getSelectedPids() {
        return selectedPids;
    }
}
