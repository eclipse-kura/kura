package org.eclipse.kura.web.client.ui.settings;

public enum PartialSnapshotCheckboxStatus {

    ALL_VISIBLE_ALL_SELECTED,
    ALL_VISIBLE_PARTIAL_SELECTED,
    PARTIAL_VISIBLE_ALL_SELECTED,
    PARTIAL_VISIBLE_PARTIAL_SELECTED;

    public static PartialSnapshotCheckboxStatus fromVisibleAndSelectedStatus(boolean allVisible, boolean allSelected) {
        if (allVisible) {
            if (allSelected) {
                return PartialSnapshotCheckboxStatus.ALL_VISIBLE_ALL_SELECTED;
            } else {
                return PartialSnapshotCheckboxStatus.ALL_VISIBLE_PARTIAL_SELECTED;
            }
        } else {
            if (allSelected) {
                return PartialSnapshotCheckboxStatus.PARTIAL_VISIBLE_ALL_SELECTED;
            } else {
                return PartialSnapshotCheckboxStatus.PARTIAL_VISIBLE_PARTIAL_SELECTED;
            }
        }
    }

}
