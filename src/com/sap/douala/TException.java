// ----------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Manages the exception pane
// ----------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
// ----------------------------------------------------------------
// ----------------------------------------------------------------
public class TException extends JPanel implements TDoualaView {
	
	JTable 				mExceptTable;
	TDoudiaTableModel 	mExceptModel;
	TConnection			mConnection;
	static TException	mInstance = null;
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
	    mConnection.setTableListener(mExceptTable, 	mExceptModel, "Exceptions");
	    
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
