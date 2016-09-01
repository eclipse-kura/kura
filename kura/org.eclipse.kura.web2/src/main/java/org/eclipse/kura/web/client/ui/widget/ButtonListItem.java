package org.eclipse.kura.web.client.ui.widget;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.base.AbstractListItem;
import org.gwtbootstrap3.client.ui.base.HasDataToggle;
import org.gwtbootstrap3.client.ui.base.HasHref;
import org.gwtbootstrap3.client.ui.base.HasIcon;
import org.gwtbootstrap3.client.ui.base.HasIconPosition;
import org.gwtbootstrap3.client.ui.base.HasTargetHistoryToken;
import org.gwtbootstrap3.client.ui.constants.IconFlip;
import org.gwtbootstrap3.client.ui.constants.IconPosition;
import org.gwtbootstrap3.client.ui.constants.IconRotate;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasText;

public class ButtonListItem extends AbstractListItem 
	implements HasHref, HasTargetHistoryToken, HasClickHandlers, Focusable, HasDataToggle, HasIcon, HasIconPosition, HasText{

	protected final Button button;
	protected final Span span;
    protected final Anchor anchor;

    protected ButtonListItem(final GwtConfigComponent component, final String text) {
        anchor = new Anchor();
        span = new Span();
        button = new Button();
        if(component.isFactoryComponent()){
        	add(button, (Element) getElement());
        }else{
        	add(span, (Element)getElement());
        }
        add(anchor, (Element) getElement());
        setText(text);
    }

    @Override
    public void setHref(final String href) {
        anchor.setHref(href);
    }

    @Override
    public String getHref() {
        return anchor.getHref();
    }

    @Override
    public void setTargetHistoryToken(final String targetHistoryToken) {
        anchor.setTargetHistoryToken(targetHistoryToken);
    }

    @Override
    public String getTargetHistoryToken() {
        return anchor.getTargetHistoryToken();
    }

    @Override
    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        return anchor.addClickHandler(handler);
    }

    @Override
    public int getTabIndex() {
        return anchor.getTabIndex();
    }

    @Override
    public void setAccessKey(final char key) {
        anchor.setAccessKey(key);
    }

    @Override
    public void setFocus(final boolean focused) {
        anchor.setFocus(focused);
    }

    @Override
    public void setTabIndex(final int index) {
        anchor.setTabIndex(index);
    }

    @Override
    public void setDataToggle(final Toggle toggle) {
        anchor.setDataToggle(toggle);
    }

    @Override
    public Toggle getDataToggle() {
        return anchor.getDataToggle();
    }


    @Override
    public void setIcon(final IconType iconType) {
        anchor.setIcon(iconType);
    }

    @Override
    public IconType getIcon() {
        return anchor.getIcon();
    }

    @Override
    public void setIconPosition(final IconPosition iconPosition) {
        anchor.setIconPosition(iconPosition);
    }

    @Override
    public IconPosition getIconPosition() {
        return anchor.getIconPosition();
    }

    @Override
    public void setIconSize(final IconSize iconSize) {
        anchor.setIconSize(iconSize);
    }

    @Override
    public IconSize getIconSize() {
        return anchor.getIconSize();
    }

    @Override
    public void setIconFlip(final IconFlip iconFlip) {
        anchor.setIconFlip(iconFlip);
    }

    @Override
    public IconFlip getIconFlip() {
        return anchor.getIconFlip();
    }

    @Override
    public void setIconRotate(final IconRotate iconRotate) {
        anchor.setIconRotate(iconRotate);
    }

    @Override
    public IconRotate getIconRotate() {
        return anchor.getIconRotate();
    }

    @Override
    public void setIconBordered(final boolean iconBordered) {
        anchor.setIconBordered(iconBordered);
    }

    @Override
    public boolean isIconBordered() {
        return anchor.isIconBordered();
    }

    @Override
    public void setIconMuted(final boolean iconMuted) {
        anchor.setIconMuted(iconMuted);
    }

    @Override
    public boolean isIconMuted() {
        return anchor.isIconMuted();
    }

    @Override
    public void setIconLight(final boolean iconLight) {
        anchor.setIconLight(iconLight);
    }

    @Override
    public boolean isIconLight() {
        return anchor.isIconLight();
    }

    @Override
    public void setIconSpin(final boolean iconSpin) {
        anchor.setIconSpin(iconSpin);
    }

    @Override
    public boolean isIconSpin() {
        return anchor.isIconSpin();
    }

    @Override
    public void setIconFixedWidth(final boolean iconFixedWidth) {
        anchor.setIconFixedWidth(iconFixedWidth);
    }

    @Override
    public boolean isIconFixedWidth() {
        return anchor.isIconFixedWidth();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        anchor.setEnabled(enabled);
    }
    
    @Override
    public void setText(final String text) {
        anchor.setText(text);
    }

    @Override
    public String getText() {
        return anchor.getText();
    }

}
