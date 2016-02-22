package org.eclipse.kura.web.client.ui.wires;

import java.util.logging.Logger;

import org.gwtbootstrap3.client.shared.event.ModalShowEvent;
import org.gwtbootstrap3.client.shared.event.ModalShowHandler;
import org.gwtbootstrap3.client.shared.event.ModalShownEvent;
import org.gwtbootstrap3.client.shared.event.ModalShownHandler;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite {

	interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
	}
	
	private static WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());
	
	@UiField
	Modal wiresModal;
	@UiField
	Button wiresButton;
	
	public WiresPanelUi() {
		initWidget(uiBinder.createAndBindUi(this));
		wiresModal.addShownHandler(new ModalShownHandler() {

			@Override
			public void onShown(ModalShownEvent evt) {
				wiresModalOpen();
				
			}
			
		});
	}
	
	public static native void wiresModalOpen() /*-{
	    $wnd.ethWires.test();
	}-*/;
	
}
