// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   The trigger generates a tree output for all trace trigger events

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
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTree;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

@SuppressWarnings("serial")

//------------------------------------------------------------------
//------------------------------------------------------------------
public class TTraceCallstack extends JPanel implements TDoualaView {

    int mDepthInx = -1;
    int mClassInx = -1;
    int mMethdInx = -1;
    int mEventInx = -1;
    TConnection                      mConnection;
    JTree                            mTree;
    JScrollBar                       mScrollbar;
    Vector<DefaultMutableTreeNode>   mTreeNodes;
    Vector<DefaultMutableTreeNode>   mEventTreeNodes;
    DefaultTreeModel                 mTreeModel;
    TDoudiaTableModel                mEventModel;
    JTable                           mEventTable;
    
    /**
     * Creates a TTraceCallstack element with all components.
     *///------------------------------------------------------------------
    TTraceCallstack() {
        super(new BorderLayout());
        DefaultMutableTreeNode aTreeNode;    
        
        mConnection     = TConnection.getInstance();
        aTreeNode       = new DefaultMutableTreeNode("Callstack");
        mTreeModel      = new DefaultTreeModel(aTreeNode);        
        mTree           = new JTree(mTreeModel);
        mTreeNodes      = new Vector<DefaultMutableTreeNode>();
        mEventTreeNodes = new Vector<DefaultMutableTreeNode>();
        
        mTreeModel.setRoot(aTreeNode);
        mTreeNodes.add(aTreeNode);
        
        mEventTable = new JTable() {
            public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
                return TDouala.getInstance().getRenderer();
            }
        };
        
        mEventTable.setRowSelectionAllowed(true);
        mEventTable.setIntercellSpacing(new Dimension(15,2));
        
        mEventModel = new TDoudiaTableModel();
        mConnection.setTableListener(mEventTable,       mEventModel,     "Event");
        
        ListSelectionModel aRowSelModel = mEventTable.getSelectionModel();
        
        aRowSelModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                ListSelectionModel aLsModel = (ListSelectionModel)e.getSource();
                
                if (!aLsModel.isSelectionEmpty()) {
                    int aRow = aLsModel.getMinSelectionIndex();                    
                    TreePath aPath = new TreePath(mEventTreeNodes.get(aRow).getPath());
                    mTree.scrollPathToVisible(aPath);
                    mTree.setSelectionPath(aPath);
                }                                
            }
            
        });

        JScrollPane aScrollPane;
        
        aScrollPane = new JScrollPane(mTree);        
        add(aScrollPane, BorderLayout.CENTER);    
        
        mScrollbar  = aScrollPane.getVerticalScrollBar();        
        aScrollPane = new JScrollPane(mEventTable);
        aScrollPane.setPreferredSize(new Dimension(100, 200));
        add(aScrollPane, BorderLayout.SOUTH);    
        
        // --------------------------------
        // Initialize the node
        // --------------------------------    
        doRefresh();        
    }

    //------------------------------------------------------------------
    //------------------------------------------------------------------
    @Override
    public void doUpdate() {
        // TODO Auto-generated method stub
        
    }

    //------------------------------------------------------------------
    //------------------------------------------------------------------
    @Override
    public void doRefresh() {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Displays a callstack in a tree representation.
     * Each callstack is associated with a thread and each thread has its
     * own panel. 
     * There is a detail view, which collects trigger events in a separate list.
     * The list stores objects of type <code>DefaultMutableTreeNode</code>,
     * which allows to synch tree view on selection.
     * 
     * @param aCallstackModel Model containing the elements of a callstack
     *///------------------------------------------------------------------
    void doPrintCallstack(TDoudiaTableModel aCallstackModel) {
        int                     aInx;
        int                     aRow;
        int                     aDepth;                
        StringBuffer            aValue;        
        DefaultMutableTreeNode  aTreeNode = null;
        DefaultMutableTreeNode  aRootNode = null;
        TDoudiaTableModel       aModel;

        aModel = aCallstackModel;
        
        if (mDepthInx < 0) {
            for (aInx = 0; aInx < aModel.getColumnCount(); aInx++) {
                if (aModel.getColumnName(aInx).equals("Depth")) {
                    mDepthInx = aInx;
                }
                else if (aModel.getColumnName(aInx).equals("ClassName")) {
                    mClassInx = aInx;
                }
                else if (aModel.getColumnName(aInx).equals("MethodName")) {
                    mMethdInx = aInx;
                }
                else if (aModel.getColumnName(aInx).equals("Event")) {
                    mEventInx = aInx;
                }
            }
        }
        
        for (aRow = 0; aRow < aModel.getRowCount(); aRow++) {
            String aNumber = (String)aModel.getValueAt(aRow, mDepthInx);
            if (aNumber.equals("")) {
                continue;
            }
            aValue   = new StringBuffer();
            aDepth   = new Integer(aNumber);
            aValue.append((String)aModel.getValueAt(aRow, mClassInx))
                  .append('.')
                  .append((String)aModel.getValueAt(aRow, mMethdInx));
            
            // Evaluate the root node
            // mTreeNodes contains at least one element
            if (aRootNode  == null) {
                aRootNode = mTreeNodes.get(mTreeNodes.size()-1);
                
                // Fill tree with unknown methods 
                // to keep the structure with correct depth
                while (mTreeNodes.size() < aDepth) {
                    aTreeNode = new DefaultMutableTreeNode("Unknown Method");
                    mTreeModel.insertNodeInto(aTreeNode, aRootNode, aRootNode.getChildCount());
                    mTreeNodes.add(aRootNode);
                    aRootNode = aTreeNode;
                    mTreeNodes.add(aRootNode);
                }
                aRootNode = mTreeNodes.get(aDepth-1);
                mTreeNodes.setSize(aDepth);        
            }
            aTreeNode = new DefaultMutableTreeNode(aValue);

            mTreeModel.insertNodeInto(aTreeNode, aRootNode, aRootNode.getChildCount());
            aRootNode = aTreeNode;                    
            mTreeNodes.add(aRootNode);
            
            String aEvent = (String)aModel.getValueAt(aRow, mEventInx);
            if (!aEvent.equals("Call")) {
                mEventModel.addRow(aModel.getRowAt(aRow), "0x0", 0);
                mEventTreeNodes.add(aRootNode);
    
                if (mEventModel.mIsInitial) {
                    mEventModel.mIsInitial   = false;
                    mEventModel.mColumnNames = aModel.mColumnNames;
                    mEventModel.fireTableStructureChanged();
                }                    
                mEventModel.fireTableDataChanged();
            }
            mTree.scrollPathToVisible(new TreePath(aRootNode.getPath()));
        }            
        aModel.clearRows();
    }
}
