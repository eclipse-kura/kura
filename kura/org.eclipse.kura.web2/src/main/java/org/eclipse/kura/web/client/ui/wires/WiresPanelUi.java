/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite {

	interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
	}

	private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);

	private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());

	private static List<String> m_components;
	private static List<String> m_drivers;
	private static List<String> m_emitters;
	private static String m_graph;
	private static List<String> m_receivers;
	private static String m_wires;

	private static WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

	public WiresPanelUi() {
		this.initWidget(uiBinder.createAndBindUi(this));
		m_emitters = new ArrayList<String>();
		m_receivers = new ArrayList<String>();
		m_components = new ArrayList<String>();
		m_drivers = new ArrayList<String>();
	}

	private static JSONArray createComponentsJson() {

		final JSONArray components = new JSONArray();
		int i = 0;

		for (final String component : m_components) {
			final JSONObject compObj = new JSONObject();
			final String[] tokens = component.split("\\|");
			compObj.put("fPid", new JSONString(tokens[0]));
			compObj.put("pid", new JSONString(tokens[1]));
			compObj.put("name", new JSONString(tokens[2]));
			compObj.put("type", new JSONString(tokens[3]));
			components.set(i, compObj);
			i++;
		}

		return components;
	}

	public static native void exportJSNIUpdateWireConfig() /*-{
															$wnd.jsniUpdateWireConfig = $entry(
															@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateWireConfig(Ljava/lang/String;)
															);
															}-*/;

	private static void internalLoad(final GwtWiresConfiguration config) {
		if (m_emitters != null) {
			m_emitters.clear();
		}
		if (m_receivers != null) {
			m_receivers.clear();
		}
		if (m_components != null) {
			m_components.clear();
		}
		if ((m_drivers != null)) {
			m_drivers.clear();
		}

		logger.info(config.getWiresConfigurationJson());
		for (final String emitter : config.getWireEmitterFactoryPids()) {
			if ((m_emitters != null) && !m_emitters.contains(emitter)) {
				m_emitters.add(emitter);
			}
		}
		for (final String receiver : config.getWireReceiverFactoryPids()) {
			if ((m_receivers != null) && !m_receivers.contains(receiver)) {
				m_receivers.add(receiver);
			}
		}

		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {
			@Override
			public void onFailure(final Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(final GwtXSRFToken token) {
				gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {
					@Override
					public void onFailure(final Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(final List<String> result) {
						m_drivers.addAll(result);
						EntryClassUi.hideWaitModal();
					}

				});
			}
		});

		m_components = config.getWireComponents();
		m_wires = config.getWiresConfigurationJson();
		m_graph = config.getGraph();

		loadGraph();
		exportJSNIUpdateWireConfig();
	}

	// ----------------------------------------------------------------
	//
	// JSNI
	//
	// ----------------------------------------------------------------
	public static int jsniUpdateWireConfig(final String obj) {

		EntryClassUi.showWaitModal();
		// Create new components
		// "models" hold all existing components in JointJS graph. If the PID is
		// "none", then we need to create
		// component in framework and set PID.
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {
			@Override
			public void onFailure(final Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(final GwtXSRFToken token) {
				gwtWireService.updateWireConfiguration(token, obj, new AsyncCallback<GwtWiresConfiguration>() {
					@Override
					public void onFailure(final Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(final GwtWiresConfiguration result) {
						internalLoad(result);
						EntryClassUi.hideWaitModal();
					}

				});
			}
		});

		return 0;
	}

	public static void load() {
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

			@Override
			public void onFailure(final Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(final GwtXSRFToken token) {
				gwtWireService.getWiresConfiguration(token, new AsyncCallback<GwtWiresConfiguration>() {

					@Override
					public void onFailure(final Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(final GwtWiresConfiguration result) {
						internalLoad(result);
						EntryClassUi.hideWaitModal();
					}

				});
			}

		});
	}

	private static void loadGraph() {
		final JSONObject obj = new JSONObject();
		final JSONArray emitters = new JSONArray();
		final JSONArray receivers = new JSONArray();
		final JSONArray drivers = new JSONArray();

		int i = 0;
		for (final String emitter : m_emitters) {
			emitters.set(i, new JSONString(emitter));
			i++;
		}
		i = 0;
		for (final String receiver : m_receivers) {
			receivers.set(i, new JSONString(receiver));
			i++;
		}
		i = 0;
		for (final String driver : m_drivers) {
			drivers.set(i, new JSONString(driver));
			i++;
		}

		obj.put("pFactories", emitters);
		obj.put("cFactories", receivers);
		obj.put("drivers", drivers);
		obj.put("components", createComponentsJson());
		obj.put("wires", JSONParser.parseStrict(m_wires));
		obj.put("pGraph", JSONParser.parseStrict(m_graph));

		wiresOpen(obj.toString());
	}

	public static native void wiresOpen(String obj) /*-{
													$wnd.kuraWires.render(obj);
													}-*/;

}
