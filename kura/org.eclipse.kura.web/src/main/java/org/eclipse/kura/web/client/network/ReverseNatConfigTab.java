package org.eclipse.kura.web.client.network;

import java.util.ArrayList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.SwappableListStore;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtReverseNatEntries;
import org.eclipse.kura.web.shared.model.GwtReverseNatEntry;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;


public class ReverseNatConfigTab extends LayoutContainer {
	
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	private GwtSession            m_currentSession;

	private Grid<GwtReverseNatEntry>   m_grid;
	
	private BaseListLoader<ListLoadResult<GwtReverseNatEntry>> m_loader;
	private GwtReverseNatEntry m_selectedEntry;
	private boolean m_dirty;
	
	private GwtNetInterfaceConfig  m_selectNetIfConfig;
	private ToolBar m_reverseNatToolBar;
	private Button m_newButton;
	private Button m_editButton;
	private Button m_deleteButton;
		
	public ReverseNatConfigTab(GwtSession currentSession) {
		m_currentSession = currentSession; 
	}
	
	protected void onRender(final Element parent, int index) {
		
		super.onRender(parent, index);

		m_dirty = false;
		
		//
		// Borderlayout that expands to the whole screen
		setLayout(new FitLayout());
		setBorders(false);
		setId("reverse-nat");
		
        LayoutContainer mf = new LayoutContainer();
        mf.setLayout(new BorderLayout());
		
		//
		// Center Panel: Open Ports Table
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 1F);
		centerData.setMargins(new Margins(0, 0, 0, 0));
		centerData.setSplit(true);  
		centerData.setMinSize(0);
		
		ContentPanel reverseNatTablePanel = new ContentPanel();
		reverseNatTablePanel.setBorders(false);
		reverseNatTablePanel.setBodyBorder(false);
		reverseNatTablePanel.setHeaderVisible(false);
		reverseNatTablePanel.setScrollMode(Scroll.AUTO);
		reverseNatTablePanel.setLayout(new FitLayout());
		
		initToolBar();
        initGrid();
        
        reverseNatTablePanel.setTopComponent(m_reverseNatToolBar);
        reverseNatTablePanel.add(m_grid);
		mf.add(reverseNatTablePanel, centerData);
		
        add(mf);
        
        refresh();
	}
	
	public void refresh() {
		if (m_loader != null) {
			if (!m_dirty) {
				if ((gwtNetworkService != null) && (m_selectNetIfConfig != null)) {
					m_loader.load();
				}
			}
		}
	}
    
    public boolean isDirty() {
    	return m_dirty;
    }
    
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig) {
    	m_selectNetIfConfig = netIfConfig;
    }
    
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
    	if (m_grid != null) {
			List<GwtReverseNatEntry> reversenatEntries = m_grid.getStore().getModels();
			for (GwtReverseNatEntry gwtReverseNatEntry : reversenatEntries) {
				if ((gwtReverseNatEntry.getSourceNetwork() == null)
						|| (gwtReverseNatEntry.getSourceNetwork().length() == 0)) {
					gwtReverseNatEntry.setSourceNetwork("0.0.0.0/0");
				}
				
				if ((gwtReverseNatEntry.getDestinationNetwork() == null)
						|| (gwtReverseNatEntry.getDestinationNetwork().length() == 0)) {
					gwtReverseNatEntry.setDestinationNetwork("0.0.0.0/0");
				}
			}
	    	
	    	updatedNetIf.setReverseNatEntries(new GwtReverseNatEntries(reversenatEntries));
    	}
    }
    
    
	private void initToolBar() {
		
		m_reverseNatToolBar = new ToolBar();
		m_reverseNatToolBar.setId("reverse-nat-toolbar");
		
		//
		// New Open Port Button
		m_newButton = new Button(MSGS.newButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.add()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final ReverseNatForm reverseNatForm = new ReverseNatForm(m_currentSession);
				reverseNatForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
					public void handleEvent(ComponentEvent be) {
						// add the new entry to the grid and select it
						if (reverseNatForm.getNewReverseNatEntry() != null) {
							if(!duplicateEntry(reverseNatForm.getNewReverseNatEntry())) {
								m_grid.getStore().add(reverseNatForm.getNewReverseNatEntry());
								if (!reverseNatForm.isCanceled()) {
									//m_applyButton.enable();
									m_dirty = true;
									fireEvent(Events.Change);
								}
							} else {
								MessageBox.alert(MSGS.netReverseNatFormError(), MSGS.netReverseNatFormDuplicate(), new Listener<MessageBoxEvent>() {  
									public void handleEvent(MessageBoxEvent ce) {
										//noop
									}
								});
							}
						}
					}
				});
				reverseNatForm.show();
			}

		});
		m_reverseNatToolBar.add(m_newButton);
		m_reverseNatToolBar.add(new SeparatorToolItem());

		//
		// Edit Open Port Button
		m_editButton = new Button(MSGS.editButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.edit()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				if (m_grid != null) {
					final GwtReverseNatEntry reverseNatEntry = m_grid.getSelectionModel().getSelectedItem();
					if (reverseNatEntry != null) {
						final ReverseNatForm reverseNatForm = new ReverseNatForm(m_currentSession, m_grid.getSelectionModel().getSelectedItem());
						reverseNatForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
							public void handleEvent(ComponentEvent be) {
								if(!duplicateEntry(reverseNatForm.getNewReverseNatEntry())) {
									m_grid.getStore().remove(reverseNatEntry);
									m_grid.getStore().add(reverseNatForm.getExistingReverseNatEntry());
									if (!reverseNatForm.isCanceled()) {
										m_dirty = true;
										fireEvent(Events.Change);
									}
								} else {
									MessageBox.alert(MSGS.netReverseNatFormError(), MSGS.netReverseNatFormDuplicate(), new Listener<MessageBoxEvent>() {  
										public void handleEvent(MessageBoxEvent ce) {
											//noop
										}
									});
								}
							}
						});
						reverseNatForm.show();
					}
				}
			}

		});
		m_editButton.setEnabled(false);
		m_reverseNatToolBar.add(m_editButton);
		m_reverseNatToolBar.add(new SeparatorToolItem());

	    
		//
		// Delete Open Port Entry Button
		m_deleteButton = new Button(MSGS.deleteButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.delete()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				
				if (m_grid != null) {
					
					final GwtReverseNatEntry reverseNatEntry = m_grid.getSelectionModel().getSelectedItem();
					if (reverseNatEntry != null) {

						// ask for confirmation						
						MessageBox.confirm(MSGS.confirm(), MSGS.netReverseNatDeleteConfirmation(reverseNatEntry.getOutInterface()),
							new Listener<MessageBoxEvent>() {  
							    public void handleEvent(MessageBoxEvent ce) {
							    	
							    	Log.debug("Trying to delete: " + reverseNatEntry.getOutInterface());
							    	Log.debug("Button " + ce.getButtonClicked().getText());
							    	
							    	if(ce.getButtonClicked().getText().equals("Yes")) {
							    		m_grid.getStore().remove(reverseNatEntry);
							    		m_dirty = true;
										fireEvent(Events.Change);
							    	}
							    }
							}
						);
					}
				}
			}
		});
		m_deleteButton.setEnabled(false);
		m_reverseNatToolBar.add(m_deleteButton);
	}
	
	private void initGrid() {
		
		//
		// Column Configuration
		ColumnConfig column = null;
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
		
		column = new ColumnConfig("outInterface", MSGS.netReverseNatOutInterface(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("protocol", MSGS.netReverseNatFormProtocol(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("sourceNetwork", MSGS.netReverseNatSourceNetwork(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("destinationNetwork", MSGS.netReverseNatDestinationNetwork(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		// rpc data proxy  
		RpcProxy<ListLoadResult<GwtReverseNatEntry>> proxy = new RpcProxy<ListLoadResult<GwtReverseNatEntry>>() {
			@Override
			protected void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtReverseNatEntry>> callback) {
				gwtNetworkService.findReverseNatConfigurations(m_selectNetIfConfig.getName(), callback);
			}
		};
        
        m_loader = new BaseListLoader<ListLoadResult<GwtReverseNatEntry>>(proxy);
        m_loader.setSortDir(SortDir.DESC);  
        m_loader.setSortField("outInterface"); 
        
        SwappableListStore<GwtReverseNatEntry> m_store = new SwappableListStore<GwtReverseNatEntry>(m_loader);
        m_store.setKeyProvider( new ModelKeyProvider<GwtReverseNatEntry>() {            
            public String getKey(GwtReverseNatEntry reverseNatEntry) {
                return reverseNatEntry.getOutInterface();
            }
        });
        
        m_grid = new Grid<GwtReverseNatEntry>(m_store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setAutoExpandColumn("outInterface");
        m_grid.getView().setAutoFill(true);
        
        m_loader.addLoadListener(new DataLoadListener(m_grid));

        GridSelectionModel<GwtReverseNatEntry> selectionModel = new GridSelectionModel<GwtReverseNatEntry>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtReverseNatEntry>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtReverseNatEntry> se) {
                m_selectedEntry = se.getSelectedItem();
                if (m_selectedEntry != null) {
                	m_editButton.setEnabled(true);
                	m_deleteButton.setEnabled(true);
                } else {
                	m_editButton.setEnabled(false);
                	m_deleteButton.setEnabled(false);                 
                }
            }
        });
	}
	
	private class DataLoadListener extends LoadListener {
		private Grid<GwtReverseNatEntry> m_grid;
		private GwtReverseNatEntry m_selectedEntry;

		public DataLoadListener(Grid<GwtReverseNatEntry> grid) {
			m_grid = grid;
			m_selectedEntry = null;
		}

		public void loaderBeforeLoad(LoadEvent le) {
			m_selectedEntry = m_grid.getSelectionModel().getSelectedItem();
		}

		public void loaderLoad(LoadEvent le) {
			if (le.exception != null) {
				FailureHandler.handle(le.exception);
			}

			if (m_selectedEntry != null) {
				ListStore<GwtReverseNatEntry> store = m_grid.getStore();
				GwtReverseNatEntry modelEntry = store.findModel(m_selectedEntry
						.getOutInterface());
				if (modelEntry != null) {
					m_grid.getSelectionModel().select(modelEntry, false);
					m_grid.getView().focusRow(store.indexOf(modelEntry));
				}
			}
		}
	}
	 
	private boolean duplicateEntry(GwtReverseNatEntry reverseEntry) {

		boolean isDuplicateEntry = false;
		List<GwtReverseNatEntry> entries = m_grid.getStore().getModels();
		if (entries != null && reverseEntry != null) {
			for (GwtReverseNatEntry entry : entries) {
				if (entry.getOutInterface().equals(reverseEntry.getOutInterface())
						&& entry.getProtocol().equals(reverseEntry.getProtocol())
						&& entry.getSourceNetwork().equals(reverseEntry.getSourceNetwork())
						&& entry.getDestinationNetwork().equals(reverseEntry.getDestinationNetwork())) {
					isDuplicateEntry = true;
					break;
				}
			}
		}
		return isDuplicateEntry;
	}
}
