// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Manages display of memory leaks
// ------------------------------------------------------------------

package com.sap.douala;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
// -----------------------------------------------------------------
// -----------------------------------------------------------------
public class TTraceLeak extends JPanel implements TDoualaView {
	JTable 				mLeakTable;
	JTable 				mLeakHistoryTable;
	JTable              mLeakHeapDumpTable;
	
	TDoudiaTableModel 	mLeakModel;
	TDoudiaTableModel 	mDetailHistory;
	TDoudiaTableModel 	mDetailHeap;
	
	JTabbedPane         mDetailPane;
	TConnection			mConnection;
	HashMap<String,TDoudiaTableModel> mModels;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	public TTraceLeak() {
		super(new BorderLayout());
		
		mConnection 	= TConnection.getInstance();		
		mLeakModel 	    = new TDoudiaTableModel();	
		mDetailHistory 	= new TDoudiaTableModel();	
		mDetailHeap 	= new TDoudiaTableModel();	

		mLeakTable  	= new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }
			
        	public void paint(Graphics g) {
		        TDoudiaTableModel  aInfoModel = TConnection.getInstance().getModel("Info");
			    synchronized( aInfoModel ) {
	        		super.paint(g);
				}
        	}

		};		
		
		mLeakHistoryTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
        	public void paint(Graphics g) {
		        TDoudiaTableModel  aInfoModel = TConnection.getInstance().getModel("Info");
			    synchronized( aInfoModel ) {
	        		super.paint(g);
				}
        	}

		};

		mLeakHeapDumpTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
		};
							
		mConnection.setTableListener(mLeakTable, 			mLeakModel, 	"Leak");
		mConnection.setTableListener(mLeakHistoryTable, 	mDetailHistory,  null);
		mConnection.setTableListener(mLeakHeapDumpTable, 	mDetailHeap,	 null);

		mLeakTable.setIntercellSpacing(new Dimension(15,2));
		mLeakHistoryTable.setIntercellSpacing(new Dimension(15,2));
		mLeakHeapDumpTable.setIntercellSpacing(new Dimension(15,2));

		mDetailPane = new JTabbedPane();
		mDetailPane.setPreferredSize(new Dimension(300, 200));
		
		JScrollPane aLeakPane 		= new JScrollPane(mLeakTable);
		JScrollPane aHistoryPane 	= new JScrollPane(mLeakHistoryTable);
		JScrollPane aDumpPane 		= new JScrollPane(mLeakHeapDumpTable);
		
		mDetailPane.add("Heap", 	aDumpPane);
		mDetailPane.add("History", 	aHistoryPane);

		add(aLeakPane, 	 BorderLayout.CENTER);
		add(mDetailPane, BorderLayout.SOUTH);		
		
		mDetailHistory.mKeepRows = true;
		// -----------------------------------------------------------------
		// -----------------------------------------------------------------
		mLeakModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE && mLeakModel.mReady) {
					int aRowSel = mLeakTable.getRowCount()-1;
					if (aRowSel >= 0) {
						mLeakTable.setRowSelectionInterval(aRowSel, aRowSel);
					}
				}
				
			}
		});

		//------------------------------------------------------------------
		//------------------------------------------------------------------
		final JTableHeader aHeader = mLeakTable.getTableHeader();
		aHeader.addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point aPoint = e.getPoint();
				int aInx   = aHeader.columnAtPoint(aPoint);
				setFilter("SortColumn", mLeakModel.getColumnName(aInx));
			}			
		});	
		
		// -----------------------------------------------------------------
		// -----------------------------------------------------------------
		mDetailPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JTabbedPane aPanel = (JTabbedPane)e.getSource();
				switch (aPanel.getSelectedIndex()) {
					case 0: break; 
					case 1: break; 
				}				
			}			
		});
		
		// -----------------------------------------------------------------
		// -----------------------------------------------------------------
		ListSelectionModel aRowSelModel = mLeakTable.getSelectionModel();
		aRowSelModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel aLsModel = (ListSelectionModel)e.getSource();
				
		        if (!aLsModel.isSelectionEmpty()) {
		        	int 	aRow1 	= aLsModel.getMinSelectionIndex();
		        	int 	aRow2 	= aLsModel.getMaxSelectionIndex();
		        	String  aRowId 	= (String)mLeakModel.getRowidAt(Math.max(aRow1, aRow2));
		        	
		        	TDoudiaTableModel aDetailModel = mLeakModel.getChild(aRowId);
		        	TDoudiaTableModel aModel;
		        	
		        	aModel = aDetailModel.findChild("HistoryGrowing");
		        	if (aModel != null) {
		        		if (mDetailHistory.mIsInitial) { 
		        			mDetailHistory.mIsInitial    = aModel.mColumnNames.size() > 0;
		        			mDetailHistory.mColumnNames  = aModel.mColumnNames;
		        			mDetailHistory.fireTableStructureChanged();
		        		}
			        	mDetailHistory.setRows(aModel);
			        	mDetailHistory.fireTableDataChanged();		        			        
		        	}

		        	aModel = aDetailModel.findChild("HeapGrowing");
		        	if (aModel != null) {
		        		if (mDetailHeap.mIsInitial) { 		        	
		        			mDetailHeap.mIsInitial 	  = aModel.mColumnNames.size() > 0;
		        			mDetailHeap.mColumnNames  = aModel.mColumnNames;
		        			mDetailHeap.fireTableStructureChanged();
		        		}
			        	mDetailHeap.setRows(aModel);
			        	mDetailHeap.fireTableDataChanged();
		        	}
		        }								
			}
			
		});
	}

	//------------------------------------------------------------------
	//------------------------------------------------------------------	
	void setFilter(String aColName, String aValue) {
		String aOldValue = mLeakModel.mFilters.get(aColName);
		if (aOldValue == null || !aOldValue.equals(aValue)) {
			mLeakModel.mFilters.put(aColName, aValue);
			doUpdate();
		}
	}	
		
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void doRefresh() {
		doUpdate();
	}
	
	// -----------------------------------------------------------------
	// -----------------------------------------------------------------
	public void doUpdate() {
		String  aFilter;
		
		String aCmd = "lml";
		aFilter = mLeakModel.mFilters.get("SortColumn");
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -s" + aFilter;
		}
		mLeakModel.mReady = false;
		mConnection.doCommand(aCmd);
		mLeakModel.mReady = true;
	}

	// -----------------------------------------------------------------
	// -----------------------------------------------------------------
	public void doReset() {
	}
}
