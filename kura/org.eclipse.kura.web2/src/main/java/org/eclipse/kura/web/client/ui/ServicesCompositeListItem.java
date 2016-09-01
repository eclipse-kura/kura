package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Div;

import com.google.gwt.user.client.ui.Composite;

public class ServicesCompositeListItem extends Composite{
	private Div div;
	private ServicesAnchorListItem listItem;
	private Button button;
	
	public ServicesCompositeListItem(GwtConfigComponent service, EntryClassUi mainUi){
		div = new Div();
		div.addStyleName("nav");
		div.addStyleName("nav-pills");
		listItem = new ServicesAnchorListItem(service, mainUi);
		button = new Button();
		button.setIcon(IconType.MINUS);
		button.setSize(ButtonSize.EXTRA_SMALL);
		button.addStyleName("pull-right");
		
		div.add(listItem);
		if(service.isFactoryComponent()){
			div.add(button);
		}
		initWidget(div);
	}
}
