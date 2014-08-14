package org.eclipse.kura.web.client.network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.TextFieldValidator;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtReverseNatEntry;
import org.eclipse.kura.web.shared.model.GwtReverseNatProtocol;
import org.eclipse.kura.web.shared.model.GwtSession;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class ReverseNatForm extends Window {
	
	private static final Messages MSGS = GWT.create(Messages.class);

    private static final int  LABEL_WIDTH_FORM = 190; 
    
	//private GwtSession m_currentSession;
	private GwtReverseNatEntry m_newEntry;
	private GwtReverseNatEntry m_existingEntry;
	private FormPanel m_formPanel;
	private Status m_status;
	private boolean m_isCanceled;
	private ComponentPlugin m_dirtyPlugin;
    
    public ReverseNatForm(GwtSession session) {
    	//m_currentSession = session;
    	m_existingEntry = null;
    	
    	 setModal(true);
         setSize(600, /*500*/250);
         setLayout(new FitLayout());
         setResizable(false);
         String heading = MSGS.netReverseNatFormNew();
         setHeading(heading);
         
		final ReverseNatForm theTab = this;
		m_dirtyPlugin = new ComponentPlugin() {
			public void init(Component component) {
				component.addListener(Events.Change,
						new Listener<ComponentEvent>() {
							public void handleEvent(ComponentEvent be) {
								FormUtils.addDirtyFieldIcon(be.getComponent());
								theTab.fireEvent(Events.Change);
							}
						});
			}
		};   
    }
    
	public ReverseNatForm(GwtSession session, GwtReverseNatEntry existingEntry) {
		this(session);
		m_existingEntry = existingEntry;
		if (m_existingEntry != null) {
			setHeading(MSGS.netReverseNatFormUpdate(m_existingEntry
					.getOutInterface()));
		}
	}

	public GwtReverseNatEntry getNewReverseNatEntry() {
		return m_newEntry;
	}

	public GwtReverseNatEntry getExistingReverseNatEntry() {
		return m_existingEntry;
	}

	public boolean isCanceled() {
		return m_isCanceled;
	}
	
	public boolean isValid() {
    	if (m_formPanel != null) {
    		for (Field<?> field : m_formPanel.getFields()) {
    			if (!field.isValid()) {
    				return false;
    			}
    		}
    	}
    	return true;
    }

	protected void onRender(Element parent, int index) {
		
		super.onRender(parent, index);
    	setId("reverse-nat-form");
    	
    	FormData formData = new FormData("-30");

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(true);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());

        FieldSet fieldSet = new FieldSet();
        fieldSet.setHeading(MSGS.netReverseNatFormInformation());
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        
        //
    	// interface name
        //
        final LabelField outInterfaceNameLabel = new LabelField();
        outInterfaceNameLabel.setName("outInterfaceNameLabel");
        outInterfaceNameLabel.setFieldLabel(MSGS.netReverseNatFormOutInterfaceName());
        outInterfaceNameLabel.setLabelSeparator(":");
        fieldSet.add(outInterfaceNameLabel, formData);

        final TextField<String> outInterfaceNameField = new TextField<String>();
        outInterfaceNameField.setAllowBlank(false);
        outInterfaceNameField.setName("interfaceName");
        outInterfaceNameField.setFieldLabel(MSGS.netReverseNatFormOutInterfaceName());
        outInterfaceNameField.setValidator(new TextFieldValidator(outInterfaceNameField, FieldType.ALPHANUMERIC));
        outInterfaceNameField.addPlugin(m_dirtyPlugin);
        fieldSet.add(outInterfaceNameField, formData);
        
        //
    	// protocol
        //
        /*
        final LabelField protocolLabel = new LabelField();
        protocolLabel.setName("protocolLabel");
        protocolLabel.setFieldLabel(MSGS.netReverseNatFormProtocol());
        protocolLabel.setLabelSeparator(":");
        fieldSet.add(protocolLabel, formData);
		*/
        final SimpleComboBox<String> protocolCombo = new SimpleComboBox<String>();
        protocolCombo.setName("protocolCombo");
        protocolCombo.setFieldLabel(MSGS.netReverseNatFormProtocol());
        protocolCombo.setEditable(false);
        protocolCombo.setTypeAhead(true);  
        protocolCombo.setTriggerAction(TriggerAction.ALL);
        for (GwtReverseNatProtocol protocol : GwtReverseNatProtocol.values()) {
        	protocolCombo.add(protocol.name());
        }
        protocolCombo.setSimpleValue(GwtReverseNatProtocol.all.name());
        fieldSet.add(protocolCombo, formData);
        
        //
    	// Source Network
        //
        final LabelField sourceNetworkLabel = new LabelField();
        sourceNetworkLabel.setName("sourceNetworkLabel");
        sourceNetworkLabel.setFieldLabel(MSGS.netReverseNatFormSourceNetwork());
        sourceNetworkLabel.setLabelSeparator(":");
        fieldSet.add(sourceNetworkLabel, formData);

        final TextField<String> sourceNetworkField = new TextField<String>();
        sourceNetworkField.setAllowBlank(true);
        sourceNetworkField.setName("address");
        sourceNetworkField.setFieldLabel(MSGS.netReverseNatFormSourceNetwork());
        sourceNetworkField.setValidator(new TextFieldValidator(sourceNetworkField, FieldType.NETWORK));
        sourceNetworkField.addPlugin(m_dirtyPlugin);
        fieldSet.add(sourceNetworkField, formData);
        
        //
    	// Destination Network
        //
        final LabelField destinationNetworkLabel = new LabelField();
        destinationNetworkLabel.setName("destinationNetworkLabel");
        destinationNetworkLabel.setFieldLabel(MSGS.netReverseNatFormDestinationNetwork());
        destinationNetworkLabel.setLabelSeparator(":");
        fieldSet.add(destinationNetworkLabel, formData);
        
        final TextField<String> destinationNetworkField = new TextField<String>();
        destinationNetworkField.setAllowBlank(true);
        destinationNetworkField.setName("address");
        destinationNetworkField.setFieldLabel(MSGS.netReverseNatFormDestinationNetwork());
        destinationNetworkField.setValidator(new TextFieldValidator(destinationNetworkField, FieldType.NETWORK));
        destinationNetworkField.addPlugin(m_dirtyPlugin);
        fieldSet.add(destinationNetworkField, formData);
        
        //add the fieldSet to the panel
        m_formPanel.add(fieldSet);
    	
        //disable the labels
        outInterfaceNameLabel.setVisible(false);
        sourceNetworkLabel.setVisible(false);
        destinationNetworkLabel.setVisible(false);
    	
		m_status = new Status();
		m_status.setBusy(MSGS.waitMsg());
		m_status.hide();
		m_status.setAutoWidth(true);
		
		m_formPanel.setButtonAlign(HorizontalAlignment.LEFT);
		m_formPanel.getButtonBar().add(m_status);
		m_formPanel.getButtonBar().add(new FillToolItem());

        m_formPanel.addButton(new Button(MSGS.submitButton(), new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	// make sure all visible fields are valid before performing the action
            	for (Field<?> field : m_formPanel.getFields()) {
            		if (field.isVisible() && !field.isValid()) {
                		return;
            		}
            	}

            	//we need to add a new row to the open ports table
            	if(m_existingEntry == null) {
            		//create a new entry
            		m_newEntry = new GwtReverseNatEntry();
            		m_newEntry.setOutInterface(outInterfaceNameField.getValue());
            		m_newEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_newEntry.setSourceNetwork(sourceNetworkField.getValue());
            		m_newEntry.setDestinationNetwork(destinationNetworkField.getValue());
            	} else {
            		m_existingEntry = new GwtReverseNatEntry();
            		m_existingEntry.setOutInterface(outInterfaceNameField.getValue());
            		m_existingEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_existingEntry.setSourceNetwork(sourceNetworkField.getValue());
            		m_existingEntry.setDestinationNetwork(destinationNetworkField.getValue());
            	}
            	
            	m_isCanceled = false;
            	hide();
            }
        }));
        
        m_formPanel.addButton(new Button(MSGS.cancelButton(), new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	m_isCanceled = true;
                hide();
            }

        }));
        m_formPanel.setButtonAlign(HorizontalAlignment.CENTER);

        // populate if necessary
        if (m_existingEntry != null) {
        	
        	outInterfaceNameLabel.setValue(m_existingEntry.getOutInterface());
        	outInterfaceNameField.setValue(m_existingEntry.getOutInterface());
        	outInterfaceNameField.setOriginalValue(m_existingEntry.getOutInterface());
        	
        	//protocolLabel.setValue(m_existingEntry.getProtocol());
        	protocolCombo.setSimpleValue(m_existingEntry.getProtocol());
        	
        	sourceNetworkLabel.setValue(m_existingEntry.getSourceNetwork());
        	sourceNetworkField.setValue(m_existingEntry.getSourceNetwork());
        	sourceNetworkField.setOriginalValue(m_existingEntry.getSourceNetwork());
        	
        	destinationNetworkLabel.setValue(m_existingEntry.getDestinationNetwork());
        	destinationNetworkField.setValue(m_existingEntry.getDestinationNetwork());
        	destinationNetworkField.setOriginalValue(m_existingEntry.getDestinationNetwork());
        }
        
        add(m_formPanel);
	}
}
