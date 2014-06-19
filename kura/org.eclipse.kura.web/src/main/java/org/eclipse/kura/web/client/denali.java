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

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Theme;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class denali implements EntryPoint 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	//private final boolean VIEW_LOG = true;
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

	/**
	 * Note, we defer all application initialization code to
	 * {@link #onModuleLoad2()} so that the UncaughtExceptionHandler can catch
	 * any unexpected exceptions.
	 */
	public void onModuleLoad() 
	{
		/*
	     * Install an UncaughtExceptionHandler which will produce <code>FATAL</code> log messages
	     */
	    Log.setUncaughtExceptionHandler();

	    /*
	    // Disable the web UI log view unless VIEW_LOG is set to true
	    if (!VIEW_LOG) {
	    	Widget divLogger = Log.getLogger(DivLogger.class).getWidget();
	    	divLogger.setVisible(false);
	    }
	    */
		// use deferred command to catch initialization exceptions in
		// onModuleLoad2
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				onModuleLoad2();
			}
		});
	}

	
	/**
	 * This is the 'real' entry point method.
	 */
	public void onModuleLoad2() {

		// set the default theme
		GXT.setDefaultTheme(Theme.GRAY, true);
		
		// load custom CSS/JS
    	loadCss("denali/skin/skin.css");
    	ScriptInjector.fromUrl("skin/skin.js?v=1").inject(); // Make sure this request is not cached
		
		gwtDeviceService.findSystemProperties( new AsyncCallback<ListLoadResult<GwtGroupedNVPair>>() {    				
			public void onSuccess(ListLoadResult<GwtGroupedNVPair> results) {
				
				GwtSession gwtSession = new GwtSession();
				
				if (results != null) {
					List<GwtGroupedNVPair> pairs = results.getData();
					if (pairs != null) {
						for (GwtGroupedNVPair pair : pairs) {
							String name = pair.getName();
							if (name != null && name.equals("kura.have.net.admin")) {
								Boolean value = Boolean.valueOf(pair.getValue());
								gwtSession.setNetAdminAvailable(value);
							}
							if (name != null && name.equals("kura.version")) {
								gwtSession.setKuraVersion(pair.getValue());
							}
							if (name != null && name.equals("kura.os.version")) {
								gwtSession.setOsVersion(pair.getValue());
							}
						}
					}
				}
				
				render(gwtSession);
			}
			
			public void onFailure(Throwable caught) {
				FailureHandler.handle(caught);
				render( new GwtSession());
			}
		});
	}
	
	private static native void loadCss(String url) /*-{
		var l = $doc.createElement("link");
		l.setAttribute("id", url);
		l.setAttribute("rel", "stylesheet");
		l.setAttribute("type", "text/css");
		l.setAttribute("href", url + "?v=1"); // Make sure this request is not cached
		$doc.getElementsByTagName("head")[0].appendChild(l);
	}-*/;
	
    private void render(GwtSession gwtSession) 
    {    	
    	Log.debug("Beginning page render");
    	
        final Viewport viewport = new Viewport();
        
        final BorderLayout borderLayout = new BorderLayout();
        viewport.setLayout(borderLayout);
        viewport.setStyleAttribute("padding", "5px");
        
        //
        // north
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 52);  
        northData.setCollapsible(false);  
        northData.setFloatable(false);  
        northData.setHideCollapseTool(false);  
        northData.setSplit(false);
        northData.setMargins(new Margins(0, 0, 5, 0));  
        viewport.add(new NorthView(gwtSession), northData);

        //
        // center
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);  
        centerData.setMargins(new Margins(0));  

        ContentPanel center = new ContentPanel();
        center.setLayout(new FitLayout());
        center.setBorders(false);
        center.setBodyBorder(false);
        center.setId("center-panel-wrapper");
        viewport.add(center, centerData);
        
        //
        // west
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 180);  
        westData.setSplit(true);  
        westData.setCollapsible(true);  
        westData.setMargins(new Margins(0,5,0,0));
        WestNavigationView westView = new WestNavigationView(gwtSession, center);
        viewport.add(westView, westData);

        //
        // south
        BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 18);  
        southData.setCollapsible(false);  
        southData.setFloatable(false);  
        southData.setHideCollapseTool(false);  
        southData.setSplit(false);
        southData.setMargins(new Margins(3, 5, 0, 0));
        
        HorizontalPanel south = new HorizontalPanel();
        south.setTableWidth("100%");
        south.setId("south-panel-wrapper");
        Label copyright = new Label(MSGS.copyright());
        copyright.setStyleName("x-form-label");
        TableData td = new TableData();
        td.setHorizontalAlign(HorizontalAlignment.LEFT);
        south.add(copyright, td);
        
        Label version = new Label(gwtSession.getKuraVersion());
        version.setStyleName("x-form-label");
        TableData tdVersion = new TableData();
        tdVersion.setHorizontalAlign(HorizontalAlignment.RIGHT);
        south.add(version, tdVersion);

        viewport.add(south, southData);

        //
        // Initial Selection
//        center.setIconAbstractImagePrototype.create(Resources.INSTANCE.alerts()));
//        center.setHeading(MSGS.announcements());
//        center.removeAll();                  
//        center.add(new Overview(currentSession));
//        center.layout();

        //
        // RootPanel
        RootPanel.get().add(viewport);
    }
}
