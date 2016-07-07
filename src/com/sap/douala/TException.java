// ----------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Manages the exception pane

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

// ----------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
// ----------------------------------------------------------------
// ----------------------------------------------------------------
public class TException extends JPanel implements TDoualaView {
    
    JTable               mExceptTable;
    TDoudiaTableModel    mExceptModel;
    TConnection          mConnection;
    static TException    mInstance = null;
    
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    static TException getInstance() {
        if (mInstance == null) {
            mInstance = new TException();
        }
        return mInstance;
    }
    
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    TException() {
        super(new BorderLayout());
        
        mConnection = TConnection.getInstance();
        
        mExceptTable = new JTable() {
            public TableCellRenderer getCellRenderer(int aRow, int aColumn) {                
                return TDouala.getInstance().getRenderer();
            }
        };
        mExceptModel = new TDoudiaTableModel();
        mConnection.setTableListener(mExceptTable,     mExceptModel, "Exceptions");
        
        JScrollPane aScrollPane  = new JScrollPane(mExceptTable);
        add(aScrollPane, BorderLayout.CENTER);
    }
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    public void doReset() {        
    }
    
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    public void doRefresh() {
        doUpdate();
    }

    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    public void doUpdate() {
        String aCmd = "dex";        
        TConnection.getInstance().doCommand(aCmd);
    }
}
