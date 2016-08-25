package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.ListBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite {

	interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
	}
	
	private static WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());
	
	private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
	private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	
	private static List<String> m_emitters;
	private static List<String> m_receivers;
	private static List<String> m_components;
	private static String       m_wires;
	private static String       m_graph;

	@UiField
	static ListBox formFactoryPid;
	
	public WiresPanelUi() {
		initWidget(uiBinder.createAndBindUi(this));
		m_emitters = new ArrayList<String>();
		m_receivers = new ArrayList<String>();
		m_components = new ArrayList<String>();
	}
	
	public static void load () {
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtWireService.getWiresConfiguration(token, new AsyncCallback<GwtWiresConfiguration>() {

					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(GwtWiresConfiguration result) {
						internalLoad(result);
						EntryClassUi.hideWaitModal();
					}
					
				});
			}
			
		});
	}
	
	private static void internalLoad(GwtWiresConfiguration config) {
		m_emitters.clear();
		m_receivers.clear();
		m_components.clear();
		
		logger.info(config.getWiresConfigurationJson());
		for (String emitter : config.getWireEmitterFactoryPids()) {
			if (m_emitters != null && !m_emitters.contains(emitter))
				m_emitters.add(emitter);
		}
		for (String receiver : config.getWireReceiverFactoryPids()) {
			if (m_receivers != null && !m_receivers.contains(receiver))
				m_receivers.add(receiver);
		}
		m_components = config.getWireComponents();
		m_wires = config.getWiresConfigurationJson();
		m_graph = config.getGraph();

		loadFactoriesList();
		loadGraph();
		exportJSNIUpdateWireConfig();
	}
	
	private static void loadFactoriesList() {
		// Create list of both Emitter and Receiver Factories
		// avoiding duplicates
		formFactoryPid.clear();
		Set<String> tmpSet = new HashSet<String>(m_emitters);
		tmpSet.addAll(m_receivers);
		List<String> tmpList = new ArrayList<String>(tmpSet);
		for (String factory : tmpList) {
			formFactoryPid.addItem(factory);
		}
	}
	
	private static JSONArray createComponentsJson() {
		
		JSONArray components = new JSONArray();
		int i = 0;
		
		for (String component : m_components) {
			JSONObject compObj = new JSONObject();
			String[] tokens = component.split("\\|");
			compObj.put("fPid", new JSONString(tokens[0]));
			compObj.put("pid", new JSONString(tokens[1]));
			compObj.put("name", new JSONString(tokens[2]));
			compObj.put("type", new JSONString(tokens[3]));
			components.set(i, compObj);
			i++;
		}

		return components;
	}
	
	private static void loadGraph() {
		JSONObject obj = new JSONObject();
		JSONArray emitters = new JSONArray();
		JSONArray receivers = new JSONArray();
		
		int i = 0;
		
		for (String emitter : m_emitters) {
			emitters.set(i, new JSONString(emitter));
			i++;
		}
		i = 0;
		for (String receiver : m_receivers) {
			receivers.set(i, new JSONString(receiver));
			i++;
		}

		obj.put("pFactories", emitters);
		obj.put("cFactories", receivers);
		obj.put("components", createComponentsJson());
		obj.put("wires", JSONParser.parseStrict(m_wires));
		obj.put("pGraph", JSONParser.parseStrict(m_graph));

		wiresOpen(obj.toString());
	}
	
	// ----------------------------------------------------------------
	//
	// JSNI
	//
	// ----------------------------------------------------------------	
	public static int jsniUpdateWireConfig(final String obj) {
		
		EntryClassUi.showWaitModal();
		// Parse JSON object into JointJS graph and components that need to be deleted
		//JSONObject jObj = (JSONObject) JSONParser.parseStrict(obj);
		//final JSONObject jGraph = (JSONObject) jObj.get("jointJs");

		// Create new components
		// "models" hold all existing components in JointJS graph. If the PID is "none", then we need to create
		// component in framework and set PID.
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				logger.info("hi");
				gwtWireService.updateWireConfiguration(token, obj, new AsyncCallback<GwtWiresConfiguration>() {
					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(GwtWiresConfiguration result) {
						internalLoad(result);
						EntryClassUi.hideWaitModal();
					}
					
				});
			}
		});
		
		return 0;
	}
	
	public static native void wiresOpen(String obj) /*-{
	    $wnd.kuraWires.render(obj);
	}-*/;
	
	public static native void exportJSNIUpdateWireConfig() /*-{
		$wnd.jsniUpdateWireConfig = $entry(
			@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateWireConfig(Ljava/lang/String;)
		);
	}-*/;
	
}
