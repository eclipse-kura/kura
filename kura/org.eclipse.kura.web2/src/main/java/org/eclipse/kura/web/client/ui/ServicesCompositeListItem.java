package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Div;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ServicesCompositeListItem extends Composite implements ClickHandler{
	private Div mainDiv;
	private Widget listItem;
	private Button button;
	private GwtConfigComponent service;
	private EntryClassUi mainUi;
	
	public ServicesCompositeListItem(GwtConfigComponent service, EntryClassUi mainUi){
		this.service = service;
		this.mainUi = mainUi;
		mainDiv = new Div();
		mainDiv.addStyleName("nav");
		mainDiv.addStyleName("nav-pills");
		listItem = new ServicesAnchorListItem(service, mainUi);
		button = new Button();
		button.setIcon(IconType.MINUS);
		button.setSize(ButtonSize.EXTRA_SMALL);
		button.addClickHandler(this);
		
		mainDiv.addStyleName("row");
		Div rightDiv = new Div();
		rightDiv.addStyleName("col-md-10 col-xs-10");
		Div leftDiv = new Div();
		leftDiv.addStyleName("col-md-1 col-xs-1");
		rightDiv.add(listItem);
		leftDiv.add(button);
		mainDiv.add(leftDiv);
		mainDiv.add(rightDiv);

		if(!service.isFactoryComponent()){
			button.setVisible(false);
		}
		initWidget(mainDiv);
	}

	@Override
	public void onClick(ClickEvent event) {
		mainUi.deleteFactoryConfiguration(service);
	}
}
