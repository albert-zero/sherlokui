//------------------------------------------------------------------
/**
 * TClasses and TDetail 
 * @author  Albert Zedlitz
 * @version 1.5
 */
//------------------------------------------------------------------
package com.sap.douala;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

@SuppressWarnings("serial")
/**
 * Detail view "methods of classes" and "history of GC"
 *///---------------------------------------------------------------
class TDetails extends JPanel implements TDoualaView {
	private int DETAIL_METHODS = 0;		
	private int DETAIL_HISTORY = 1;
	private int DETAIL_HEAPDMP = 2;
	
	private TDoudiaTableModel  mMethodsModel;
	private TDoudiaTableModel  mHistoryModel;
	private TDoudiaTableModel  mHeapDmpModel;
	private TConnection mConnection;
	private String      mClassDetail = "";
	int                 mSelected    = 0;
	
	/** Static singleton instance of TDetails */
	static TDetails 	mInstance	 = null;
	JTabbedPane 		mDetailPane;
	
	/**
	 * @return Singleton instance of the detail pane
	 *///--------------------------------------------------------------
	static public TDetails getInstance() {
		if (mInstance == null) {
			mInstance = new TDetails();
		}
		return mInstance;
	}
	
	/**
	 * Creates the class detail view with all the graphical elements.
	 * Detail views are 
	 * <ul>
	 * <li><em>"method of class"</em> and</li> 
	 * <li><em>"GC history of class"</em></li></ul> 
	 *///------------------------------------------------------------------
	TDetails() {
		super(new BorderLayout());
		setPreferredSize(new Dimension(300, 200));
		mConnection   = TConnection.getInstance();
		mMethodsModel = new TDoudiaTableModel();
		mHistoryModel = new TDoudiaTableModel();
		mHeapDmpModel = new TDoudiaTableModel();
		
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		JTable aHistoryTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
		};
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		JTable aMethodsTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }
			public void addColumn(TableColumn aCol) {
				super.addColumn(aCol);
			}
		};
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		JTable aHeapDmpTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
		};
		
		aMethodsTable.setIntercellSpacing(new Dimension(15,2));
		aHistoryTable.setIntercellSpacing(new Dimension(15,2));
		aHeapDmpTable.setIntercellSpacing(new Dimension(15,2));
		
		// Paint output with synch. The XML parser inserts elements
		// directly into mMethodsModel. The overload method assures
		// that the values are not changing during output
		JScrollPane aMethodsPane = new JScrollPane(aMethodsTable) {
			public void paint(Graphics g) {
				synchronized(mMethodsModel) {
					super.paint(g);
				}
			}
			
		};
		JScrollPane aHistoryPane = new JScrollPane(aHistoryTable);
		JScrollPane aHeapDmpPane = new JScrollPane(aHeapDmpTable);

		mConnection.setTableListener(aMethodsTable, mMethodsModel, "MethodClass");
		mConnection.setTableListener(aHistoryTable, mHistoryModel, "ClassHistory");
		mConnection.setTableListener(aHeapDmpTable, mHeapDmpModel, "Heap");
		
		mDetailPane = new JTabbedPane() {
			public void paint(Graphics g) {
				synchronized(mMethodsModel) {
					super.paint(g);
				}
			}
		};
		
		mDetailPane.add("Methods", aMethodsPane);
		mDetailPane.add("History", aHistoryPane);
		mDetailPane.add("Heap",    aHeapDmpPane);
		
		add(mDetailPane, BorderLayout.CENTER);
		
		// Change listener tracks mouse selections in detail pane
		// ------------------------------------------------------------------
		mDetailPane.addChangeListener(new ChangeListener(){				
			public void stateChanged(ChangeEvent e) {
				JTabbedPane aPanel = (JTabbedPane)e.getSource();
				mSelected          = aPanel.getSelectedIndex();
				doUpdate();
			}}
		);	

		// Select the sort column by name
		//------------------------------------------------------------------
		final JTableHeader aHeader = aHeapDmpTable.getTableHeader();
		aHeader.addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point aPoint = e.getPoint();
				try {
					int 	aInx 		= aHeader.columnAtPoint(aPoint);
					String 	mSortColumn = mHeapDmpModel.getColumnName(aInx);
					mHeapDmpModel.mFilters.put("SortColumn", mSortColumn);
					doUpdate();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
		});

		// Update the table layout if new columns are available
		//------------------------------------------------------------------
		mHeapDmpModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int 	aRow = e.getFirstRow();
				int 	aCol = e.getColumn();
				String 	aValue;
				
				if (e.getType() == TableModelEvent.UPDATE) {
					// mDetail.setClass(((TDoudiaTableModel)mClassModel).getRowidAt(0));
				}
				
				if (e.getType() == TableModelEvent.UPDATE && aCol >= 0 && aRow >= 0) {			
					mHeapDmpModel.getRowidAt(0);
					aValue = (String)mHeapDmpModel.getValueAt(aRow, aCol);
					TableModel aModel = (TableModel)e.getSource();
					String aColName   = aModel.getColumnName(aCol);
					mHeapDmpModel.mFilters.put(aColName, aValue);
					doUpdate();
				}
			}
		});
	}
	
	/**
	 * Selects the specified ID for the detail view. The object ID is 
	 * associated with the XLM element <em>ID<em>, which is not displayed.   
	 * @param aClassId
	 *///------------------------------------------------------------------
	void setClass(String aClassId) {
		if (aClassId.equals(mClassDetail) || 
			aClassId.equals("")) {
			return;
		}
		mClassDetail = aClassId;
		doUpdate();
	}
	
	/**
	 * Implements the action for the button <em>Refresh</em>
	 *///------------------------------------------------------------------	
	public void doRefresh() {
		doUpdate();
	}
	
	/**
	 * Implements the action for the button <em>Update</em>.
	 * Depending on the selected items this method generates a Sherlok 
	 * command. Sherlok sends the response asynch to the XML parser,
	 * which in turn calls the model listeners. 
	 *///------------------------------------------------------------------	
	public void doUpdate() {
		String aCmd = null;
		
		if (mClassDetail.equals("") ||
			mClassDetail.equals("0x0") ) {
			return;
		}

		if (mSelected == DETAIL_METHODS) {
			aCmd    = "lsm -C" + mClassDetail;
		}

		if (mSelected == DETAIL_HISTORY) {
			aCmd 	= "lsc -h -C" + mClassDetail;
		}
		
		if (mSelected == DETAIL_HEAPDMP) {
			String aFilter;			
			aCmd 	= "lhd -C" + mClassDetail;

			aFilter = mHeapDmpModel.mFilters.get("SortColumn");
			if (aFilter != null) {
				aCmd = aCmd + " -s" + aFilter;
			}
			
			aFilter = mHeapDmpModel.mFilters.get("ClassName");
			if (aFilter != null) {
				aCmd = aCmd + " -f" + aFilter;
			}
		}
		
		if (aCmd == null) {
			return;
		}		
		mConnection.doCommand(aCmd);
	}	
}

//------------------------------------------------------------------
// Create a "Classes" pane
//------------------------------------------------------------------
@SuppressWarnings("serial")
public class TClasses extends JPanel implements TDoualaView {
	// private boolean DEBUG = false;
	private TDoudiaTableModel 	mClassModel;
	private TConnection 		mConnection;
	private JTable      		mClassTable;
	private TDetails    		mDetail;
	static  TClasses            mInstance;

	//------------------------------------------------------------------
	// Use the singleton for access
	//------------------------------------------------------------------
	static public TClasses getInstance() {
		if (mInstance == null) {
			mInstance = new TClasses();
		}
		return mInstance;
	}
	
	/**  
	 * Constructs the pane "Classes"
	 * with detail view to "Methods" 
	 *///---------------------------------------------------------------
	private TClasses() {
		super(new BorderLayout());
				
		mConnection 	= TConnection.getInstance();
		mDetail     	= new TDetails();
	    mClassModel 	= new TDoudiaTableModel();	    
	    
	    
    	mClassTable = new JTable() {    	    
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }
		};		
	    mClassTable.setIntercellSpacing(new Dimension(15,2));
		
		JPanel    aSubPanel 	= new JPanel(); 
		BoxLayout aLayout   	= new BoxLayout(aSubPanel, BoxLayout.Y_AXIS);
		aSubPanel.setLayout(aLayout);
	    mConnection.setTableListener(mClassTable, mClassModel, "Class");
		
		JScrollPane aMasterPane = new JScrollPane(mClassTable);		
		aSubPanel.add(aMasterPane);
		aSubPanel.add(mDetail);
		
		add(aSubPanel, BorderLayout.CENTER);
		
		mClassTable.setColumnSelectionAllowed(true);
		mClassTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel aRowSelModel = mClassTable.getSelectionModel();

		//------------------------------------------------------------------
		// Update the table if new rows are available
		//------------------------------------------------------------------
		aRowSelModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel aLsModel = (ListSelectionModel)e.getSource();
				
		        if (!aLsModel.isSelectionEmpty() && mClassModel.mReady) {
		        	int aRow = aLsModel.getMinSelectionIndex();
		        	//int aCol = aLsModel.getMinSelectionIndex();
		            TableModel aModel = mClassTable.getModel();
		            
		            if (aModel instanceof TDoudiaTableModel) {
		            	String aRowid = (String)((TDoudiaTableModel)aModel).getRowidAt(aRow);
		            	mDetail.setClass(aRowid);
		            }
		        }
			}			
		});
		
		//------------------------------------------------------------------
		// Update the table layout if new columns are available
		//------------------------------------------------------------------
		mClassModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int 	aRow = e.getFirstRow();
				int 	aCol = e.getColumn();
				String 	aValue;
								
				if (e.getType() == TableModelEvent.UPDATE && aCol >= 0 && aRow >= 0 && mClassModel.mReady) {			
					mDetail.setClass((String)((TDoudiaTableModel)mClassModel).getRowidAt(0));
					aValue = (String)mClassModel.getValueAt(aRow, aCol);
					TableModel aModel = (TableModel)e.getSource();
					String aColName   = aModel.getColumnName(aCol);
					setFilter(aColName, aValue);
				}
			}
		});
		
		//------------------------------------------------------------------
		// Select the sort column by name
		//------------------------------------------------------------------
		final JTableHeader aHeader = mClassTable.getTableHeader();
		aHeader.addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point aPoint = e.getPoint();
				try {
					int 	aInx 		= aHeader.columnAtPoint(aPoint);
					String 	mSortColumn = mClassModel.getColumnName(aInx);
					setFilter("SortColumn", mSortColumn);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
		});
		
		//------------------------------------------------------------------
		// Dummy implementation 
		//------------------------------------------------------------------
		addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {
			}

			public void componentMoved(ComponentEvent e) {
			}

			public void componentResized(ComponentEvent e) {
			}

			public void componentShown(ComponentEvent e) {
			}			
		});
	}

	/**
	 * Sets a new Filter 
	 * @param aColName Column name to apply filter 
	 * @param aValue Filter value
	 *///------------------------------------------------------------------
	void setFilter(String aColName, String aValue) {
		String aOldValue = mClassModel.mFilters.get(aColName);
		if (aOldValue == null || !aOldValue.equals(aValue) && mClassModel.mReady) {
			mClassModel.mFilters.put(aColName, aValue);
			doUpdate();
		}
	}
	//------------------------------------------------------------------
	// Implement the action for "Refresh" button
	//------------------------------------------------------------------	
	public void doRefresh() {
		doUpdate();
	}
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------	
	public void doReset() {
	}
	
	/**
	 * Launch <em>Sherlok</em> command. with given filter
	 * conditions.
	 *///------------------------------------------------------------------
	public void doUpdate() {
		String 	aFilter;		
		String 	aCmd = "lsc";		
		
		aFilter = mClassModel.mFilters.get("ClassName");
		if (aFilter != null && !aFilter.equals("")) {			
			if (aFilter.startsWith("-L")) {
				aCmd    = "lml";				
				aFilter = aFilter.substring(2);
				aCmd = aCmd + " -f" + aFilter;
			}
			else if (aFilter.startsWith("-F")) {
				aFilter = aFilter.substring(2);
				aCmd = aCmd + " -F" + aFilter;
			}
			else {
				aCmd = aCmd + " -f" + aFilter;
			}
		}
		
		aFilter = mClassModel.mFilters.get("CurrSize");		
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -m" + aFilter;
		}


		aFilter = mClassModel.mFilters.get("SortColumn");
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -s" + aFilter;
		}

		mClassModel.mReady = false;
		mConnection.doCommand(aCmd);
		mClassModel.mReady = true;
	}	
}
