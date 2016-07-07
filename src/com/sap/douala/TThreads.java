// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Manages the threads pane

// Copyright (C) 2015  Albert Zedlitz
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

// ------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
//-----------------------------------------------------------------
//-----------------------------------------------------------------
public class TThreads extends JPanel implements TDoualaView {
	JTable 				mMasterTable;
	JTable 				mDetailTable;
	TDoudiaTableModel 	mMasterModel;
	TDoudiaTableModel 	mDetailModel;
	TConnection		  	mConnection;
	ListSelectionModel  mRowSelModel;
	int                 mSelected = 0;
	
	//-----------------------------------------------------------------
	//-----------------------------------------------------------------
	public TThreads() {
		super(new BorderLayout());
		mConnection  = TConnection.getInstance();		
		
		//-----------------------------------------------------------------
		//-----------------------------------------------------------------
		mMasterTable = new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
		};		
		//-----------------------------------------------------------------
		//-----------------------------------------------------------------
		mDetailTable = new JTable(){
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }			
		};
		mMasterModel = new TDoudiaTableModel();
		mDetailModel = new TDoudiaTableModel();
		
		mMasterTable.setIntercellSpacing(new Dimension(15,2));
		mDetailTable.setIntercellSpacing(new Dimension(15,2));
		
		mConnection.setTableListener(mMasterTable, mMasterModel, "Thread");
		mConnection.setTableListener(mDetailTable, mDetailModel, "Callstack");
		
		JScrollPane aMasterPane = new JScrollPane(mMasterTable);
		JScrollPane aDetailPane = new JScrollPane(mDetailTable);
		
		add(aMasterPane, BorderLayout.CENTER);
		add(aDetailPane, BorderLayout.SOUTH);
		aDetailPane.setPreferredSize(new Dimension(300, 200));
		
		// -----------------------------------------------------------------
		// -----------------------------------------------------------------
		mRowSelModel = mMasterTable.getSelectionModel();
		mRowSelModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() || !mMasterModel.mReady) {
					return;
				}
				ListSelectionModel aLsModel = (ListSelectionModel)e.getSource();
				
		        if (!aLsModel.isSelectionEmpty()) {
		        	TDoudiaTableModel aModel;
		        	int 	aRow 	= aLsModel.getMinSelectionIndex();
		        	String  aRowId 	= mMasterModel.getRowidAt(aRow);
		        	mSelected       = aRow;
		        	
		        	aModel = mMasterModel.findChild(aRowId);
		        	// aModel = aModel.getChild(aRowId);
		        	if (aModel != null) {
		        		if (mDetailModel.mIsInitial) {
			        		if (aModel.mColumnNames.size() > 2) {
			        			mDetailModel.mIsInitial 	= false;
			        			mDetailModel.mColumnNames 	= aModel.mColumnNames;
				        		mDetailModel.fireTableStructureChanged();				        		
			        			mDetailModel.setRows(aModel);
			        			mDetailModel.setDataVector();
			        			mDetailModel.fireTableDataChanged();
				        	}
		        		}
		        		else {
		        			mDetailModel.setRows(aModel);
		        			mDetailModel.setDataVector();
		        			mDetailModel.fireTableDataChanged();
		        		}
		        	}
		        	else {
		        		mDetailModel.clearRows();
		        		mDetailModel.setDataVector();
		        		mDetailModel.fireTableDataChanged();
		        	}
		        }	
		        
				if (mMasterTable.getRowCount() > mSelected) {
					mMasterTable.setRowSelectionInterval(mSelected, mSelected);					
				}

			}			
		});		
		
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		mMasterModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int 	aRow = e.getFirstRow();
				int 	aCol = e.getColumn();
				String 	aValue;
								
				if (e.getType() == TableModelEvent.UPDATE && aCol >= 0 && aRow >= 0 && mMasterModel.mReady) {			
					aValue 				= (String)mMasterModel.getValueAt(aRow, aCol);
					TableModel aModel 	= (TableModel)e.getSource();
					String aColName   	= aModel.getColumnName(aCol);
					mMasterModel.mFilters.put(aColName, aValue);
					doUpdate();
				}
			}
		});
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		final JTableHeader aHeader = mMasterTable.getTableHeader();
		aHeader.addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point aPoint = e.getPoint();
				int aInx        = aHeader.columnAtPoint(aPoint);
				String aColName = mMasterTable.getColumnName(aInx);
				mMasterModel.mFilters.put("SortColumn", aColName);
				doUpdate();
			}			
		});			

	}
	
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void doUpdate() {
		String 	aFilter;
		String 	aCmd = "dt -c";

		aFilter = mMasterModel.mFilters.get("Depth");
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -m" + aFilter;
		}

		aFilter = mMasterModel.mFilters.get("SortColumn");
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -s" + aFilter;
		}

		aFilter = mMasterModel.mFilters.get("Type");
		if (aFilter != null && !aFilter.equals("")) {
			aCmd = aCmd + " -a";
		}
		mMasterModel.mReady = false;
		mMasterModel.clearChildren();
		mMasterModel.clearRows();		
		mMasterModel.fireTableDataChanged();
		
		mDetailModel.clearRows();
		mDetailModel.fireTableDataChanged();		
		mConnection.doCommand(aCmd);
		mMasterModel.mReady = true;
	}
	//-----------------------------------------------------------------
	//-----------------------------------------------------------------
	public void doRefresh() {
		doUpdate();
	}
	
	//-----------------------------------------------------------------
	//-----------------------------------------------------------------
	public void doReset() {
	}
}
