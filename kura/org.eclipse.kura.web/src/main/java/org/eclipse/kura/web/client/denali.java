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
package org.eclipse.kura.web.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.bootstrap.ui.EntryClassUi;
import org.eclipse.kura.web.shared.model.GwtBSGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.service.GwtBSDeviceService;
import org.eclipse.kura.web.shared.service.GwtBSDeviceServiceAsync;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class denali implements EntryPoint {

	private final GwtBSDeviceServiceAsync gwtBSDeviceService = GWT.create(GwtBSDeviceService.class);			

	private final EntryClassUi binder = GWT.create(EntryClassUi.class);

	/**
	 * Note, we defer all application initialization code to
	 * {@link #onModuleLoad2()} so that the UncaughtExceptionHandler can catch
	 * any unexpected exceptions.
	 */
	@Override
	public void onModuleLoad() {

		/*
		 * Install an UncaughtExceptionHandler which will produce
		 * <code>FATAL</code> log messages
		 */
		Log.setUncaughtExceptionHandler();

		/*
		 * // Disable the web UI log view unless VIEW_LOG is set to true if
		 * (!VIEW_LOG) { Widget divLogger =
		 * Log.getLogger(DivLogger.class).getWidget();
		 * divLogger.setVisible(false); }
		 */
		// use deferred command to catch initialization exceptions in
		// onModuleLoad2
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				onModuleLoad2();
			}
		});
	}

	public void onModuleLoad2() {

		Log.debug("Beginning GWTBootstrap3 render");
		System.out.println("Beginning GWTBootstrap3 render");
		RootPanel.get().add(binder);
		gwtBSDeviceService
				.findSystemProperties(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {
					@Override
					public void onSuccess(ArrayList<GwtBSGroupedNVPair> results) {

						GwtBSSession gwtBSSession = new GwtBSSession();

						if (results != null) {
							List<GwtBSGroupedNVPair> pairs = results;
							if (pairs != null) {
								for (GwtBSGroupedNVPair pair : pairs) {
									String name = pair.getName();
									if (name != null
											&& name.equals("kura.have.net.admin")) {
										Boolean value = Boolean.valueOf(pair
												.getValue());//FIXME (remove !)
										gwtBSSession
												.setNetAdminAvailable(value);
									}
									if (name != null
											&& name.equals("kura.version")) {
										gwtBSSession.setKuraVersion(pair
												.getValue());
									}
									if (name != null
											&& name.equals("kura.os.version")) {
										gwtBSSession.setOsVersion(pair
												.getValue());
									}
								}
							}
						}
						binder.setFooter(gwtBSSession);
						binder.initSystemPanel(gwtBSSession);
						binder.setSession(gwtBSSession);
						binder.initServicesTree();
						binder.setDirty(false);

					}

					@Override
					public void onFailure(Throwable caught) {
						//FailureHandler.handle(caught);
						binder.setFooter(new GwtBSSession());
						binder.initSystemPanel(new GwtBSSession());
						binder.setSession(new GwtBSSession());
					}
				});
	}

}