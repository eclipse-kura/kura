/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

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
