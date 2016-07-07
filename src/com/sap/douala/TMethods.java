// ------------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   TMethods organizes the methods page 
//   The master table contains the methods and the detail is 
//   a list of parameters

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

// ------------------------------------------------------------------------

package com.sap.douala;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
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
// ------------------------------------------------------------------------
//------------------------------------------------------------------------
public class TMethods extends JPanel implements TDoualaView {
    JTable mMasterTable;
    JTable mDetailTable;
    static final boolean DEBUG = false;
    private TDoudiaTableModel  mModel;
    private TDoudiaTableModel  mDetail;
    private TConnection        mConnection;
    private static TMethods    mMethods = null;
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    public static TMethods getInstance() {
        if (mMethods == null) {
            mMethods = new TMethods();
        }
        return mMethods;
    }
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private TMethods() {
        super(new BorderLayout());
        
        mConnection             = TConnection.getInstance();
        JPanel    aSubPanel     = new JPanel(); 
        BoxLayout aLayout       = new BoxLayout(aSubPanel, BoxLayout.Y_AXIS);
        aSubPanel.setLayout(aLayout);
        
        // The master table need update synchronization in respect to the model
        mModel  = new TDoudiaTableModel();
        mDetail = new TDoudiaTableModel();
        
        mMasterTable = new JTable() {
            public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
                return TDouala.getInstance().getRenderer();
            }
            
            protected void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g);
                } catch (Exception e) {
                    /**/
                }
            }
        };

        mDetailTable = new JTable() {
            public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
                return TDouala.getInstance().getRenderer();
            }        
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        
        mMasterTable.setIntercellSpacing(new Dimension(15,2));
        mDetailTable.setIntercellSpacing(new Dimension(15,2));
        
        mConnection.setTableListener(mMasterTable, mModel,  "Method");       
        mConnection.setTableListener(mMasterTable, mModel,  "MethodMethod");       
        mConnection.setTableListener(mDetailTable, mDetail, "MethodParameter");
        
        JScrollPane aMasterPane = new JScrollPane(mMasterTable);
        JScrollPane aDetailPane = new JScrollPane(mDetailTable);
        aDetailPane.setPreferredSize(new Dimension(300, mDetailTable.getFont().getSize()));
        aSubPanel.add(aMasterPane);
        aSubPanel.add(aDetailPane);
        add(aSubPanel, BorderLayout.CENTER);
        
        mMasterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel aRowSelModel = mMasterTable.getSelectionModel();

        //------------------------------------------------------------------
        //------------------------------------------------------------------
        aRowSelModel.addListSelectionListener(new ListSelectionListener() {
            int mSelected = 0;
            
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || !mModel.mReady) {
                    return;
                }
                ListSelectionModel aLsModel = (ListSelectionModel)e.getSource();
                int aRow  = aLsModel.getMinSelectionIndex();
                mSelected = Math.max(0, aRow);
                
                if (!aLsModel.isSelectionEmpty()) {
                    TableModel aModel = mMasterTable.getModel();

                    if (aModel instanceof TDoudiaTableModel) {
                        String aRowid = ((TDoudiaTableModel)aModel).getRowidAt(mSelected);
                        if (!aRowid.equals("") && !aRowid.equals("0x0")) {
                            mConnection.doCommand("lsm -M" + aRowid + " -p");                                
                        }
                    }
                }
                if (mMasterTable.getRowCount() > mSelected) {
                    mMasterTable.setRowSelectionInterval(mSelected, mSelected);                    
                }

            }            
        });        
        //------------------------------------------------------------------
        //------------------------------------------------------------------
        mModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                int     aRow = e.getFirstRow();
                int     aCol = e.getColumn();
                String  aValue;
                
                if (e.getType() == TableModelEvent.UPDATE && aCol >= 0 && aRow >= 0) {                                    
                    aValue = (String)mModel.getValueAt(aRow, aCol);
                    TableModel aModel = (TableModel)e.getSource();
                    String aColName   = aModel.getColumnName(aCol);
                    setFilter(aColName, aValue);
                }
            }
        });        
        //------------------------------------------------------------------
        //------------------------------------------------------------------
        final JTableHeader aHeader = mMasterTable.getTableHeader();
        aHeader.addMouseListener(new MouseInputAdapter() {
            public void mouseClicked(MouseEvent e) {
                Point aPoint = e.getPoint();
                int   aInx   = aHeader.columnAtPoint(aPoint);
                setFilter("SortColumn", mMasterTable.getColumnName(aInx));
            }            
        });            
    }
    
    //------------------------------------------------------------------
    //------------------------------------------------------------------    
    void setFilter(String aColName, String aValue) {
        String aOldValue = mModel.mFilters.get(aColName);
        if (aOldValue == null || !aOldValue.equals(aValue) && mModel.mReady) {
            mModel.mFilters.put(aColName, aValue);
            doUpdate();
        }
    }    
    //------------------------------------------------------------------
    //------------------------------------------------------------------    
    public void doRefresh() {
        doUpdate();
    }
    
    //------------------------------------------------------------------
    //------------------------------------------------------------------    
    public void doReset() {
    }
    
    //------------------------------------------------------------------
    //------------------------------------------------------------------    
    public void doUpdate() {
        String  aFilter;
        String  aMethodName = null;
        String  aClassName  = null;
        
        String aCmd = "lsm";
        aFilter = mModel.mFilters.get("CpuTime");        
        if (aFilter != null && !aFilter.equals("")) {
            aCmd = aCmd + " -m" + aFilter;
        }
        
        aFilter = mModel.mFilters.get("NrCalls");        
        if (aFilter != null && !aFilter.equals("")) {
            aCmd = aCmd + " -n" + aFilter;
        }
        
        aFilter = mModel.mFilters.get("ClassName");        
        if (aFilter != null && !aFilter.equals("")) {
            aClassName = aFilter;
        }
        
        aFilter = mModel.mFilters.get("MethodName");        
        if (aFilter != null && !aFilter.equals("")) {
            if (aFilter.startsWith("0x")) {
                aCmd = aCmd + " -M" + aFilter;
            }
            else {
                aMethodName = aFilter;
            }
        }
        
        aFilter = mModel.mFilters.get("Elapsed");
        if (aFilter != null && !aFilter.equals("")) {
            aCmd = aCmd + " -e" + aFilter;
        }

        aFilter = mModel.mFilters.get("Signature");
        if (aFilter != null && !aFilter.equals("")) {
            if (aFilter.startsWith("0x")) {
                aCmd = aCmd + " -M" + aFilter;
            }
        }

        aFilter = mModel.mFilters.get("SortColumn");
        if (aFilter != null && !aFilter.equals("")) {
            aCmd = aCmd + " -s" + aFilter;
        }
        
        // Build filter for class and method name
        if (aClassName != null) {
            if (aMethodName == null) {
                aCmd = aCmd + " -f" + aClassName;
            }
            else {
                aCmd = aCmd + " -f" + aClassName + "." + aMethodName;
            }
        }
        else {
            if (aMethodName != null) {
                aCmd = aCmd + " -f." + aMethodName;
            }
        }    
        mModel.mReady = false;
        mConnection.doCommand(aCmd);
        mModel.mReady = true;
    }    
}
