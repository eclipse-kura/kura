/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundleWithLookup {

	Resources INSTANCE = GWT.create(Resources.class);
	@Source("icons/32x32/emblems/emblem-important.png")
	ImageResource information32();

	@Source("icons/16x16/emblems/emblem-important.png")
	ImageResource information();
	
	@Source("icons/32x32/devices/drive-harddisk.png")
	ImageResource router32();

	@Source("icons/16x16/devices/drive-harddisk.png")
	ImageResource router();

	@Source("icons/32x32/places/network-workgroup.png")
	ImageResource network32();

	@Source("icons/16x16/places/network-workgroup.png")
	ImageResource network();

	@Source("icons/32x32/emblems/emblem-readonly.png")
	ImageResource firewall32();

	@Source("icons/16x16/emblems/emblem-readonly.png")
	ImageResource firewall();

	@Source("icons/32x32/mimetypes/package-x-generic.png")
	ImageResource packages32();

	@Source("icons/16x16/mimetypes/package-x-generic.png")
	ImageResource packages();

	@Source("icons/32x32/categories/preferences-system.png")
	ImageResource settings32();

	@Source("icons/16x16/categories/preferences-system.png")
	ImageResource settings();
	
	@Source("icons/32x32/status/dialog-warning.png")
	ImageResource alert32();

	@Source("icons/32x32/status/weather-overcast.png")
	ImageResource cloud32();
	
	@Source("icons/32x32/actions/appointment-new.png")
	ImageResource clock32();

	@Source("icons/32x32/apps/internet-web-browser.png")
	ImageResource gps32();

	@Source("icons/32x32/apps/utilities-system-monitor.png")
	ImageResource dog32();

	@Source("icons/32x32/mimetypes/package-x-generic.png")
	ImageResource plugin32();

    @Source("icons/16x16/actions/view-refresh.png")
    ImageResource refresh();

    @Source("icons/16x16/actions/list-add.png")
    ImageResource accept();

    @Source("icons/16x16/actions/process-stop.png")
    ImageResource cancel();

    @Source("icons/16x16/actions/go-up.png")
    ImageResource moveUp();

    @Source("icons/16x16/actions/go-down.png")
    ImageResource moveDown();

    @Source("icons/16x16/actions/document-new.png")
    ImageResource add();

    @Source("icons/16x16/apps/accessories-text-editor.png")
    ImageResource edit();

    @Source("icons/16x16/actions/edit-delete.png")
    ImageResource delete();
    
    @Source("icons/16x16/actions/list-add.png")
    ImageResource packageAdd();
    
    @Source("icons/16x16/actions/list-remove.png")
    ImageResource packageDelete();
    
    @Source("icons/16x16/mimetypes/package-x-generic.png")
    ImageResource plugin();

    @Source("icons/16x16/actions/edit-find-replace.png")
    ImageResource snapshots();

    @Source("icons/16x16/actions/edit-redo.png")
    ImageResource snapshotUpload();

    @Source("icons/16x16/actions/edit-undo.png")
    ImageResource snapshotRollback();

    @Source("icons/16x16/actions/document-save.png")
    ImageResource snapshotDownload();

    @Source("icons/32x32/places/network-server.png")
    ImageResource databaseConnect32();
    
    @Source("icons/16x16/actions/system-search.png")
    ImageResource magnifier16();
    
    @Source("icons/32x32/actions/system-search.png")
    ImageResource magnifier32();
    
    @Source("icons/16x16/status/network-transmit.png")
    ImageResource connect16();
     
    @Source("icons/16x16/status/dialog-information.png")
    ImageResource hourglass16();

    @Source("icons/others/mqtt32.png")
    ImageResource mqtt32();

    @Source("icons/16x16/status/network-wireless-encrypted.png")
    ImageResource vpn();

    @Source("icons/32x32/status/network-wireless-encrypted.png")
    ImageResource vpn32();

    @Source("icons/32x32/mimetypes/application-certificate.png")
    ImageResource lock32();

    @Source("icons/16x16/categories/applications-development.png")
    ImageResource diagnostics();
    
    @Source("icons/32x32/categories/applications-development.png")
    ImageResource diagnostics32();
    
    @Source("icons/32x32/apps/preferences-desktop-remote-desktop.png")
    ImageResource provisioning32();
    
    @Source("icons/32x32/apps/osx_terminal.png")
    ImageResource command32();
    
    @Source("icons/16x16/actions/system-lock-screen.png")
    ImageResource systemLock16();
    
    @Source("icons/32x32/actions/system-lock-screen.png")
    ImageResource systemLock32();
    
    @Source("icons/32x32/apps/bluetooth.png")
    ImageResource bluetooth32();
    
    @Source("icons/16x16/apps/bluetooth.png")
    ImageResource bluetooth();

}
