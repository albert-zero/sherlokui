// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   The trigger generates a tree output for all trace trigger events
// ------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;


@SuppressWarnings("serial")
//------------------------------------------------------------------
//------------------------------------------------------------------
public class TTraceTrigger extends JPanel implements TDoualaView {
	TConnection 	 	mConnection;	
	JTable      		mTriggerTable;
	JTabbedPane 	    mThreads;
	TDoudiaTableModel 	mTriggerModel;	
	Hashtable<String, TTraceCallstack> mHashThread;
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public TTraceTrigger() {
		super(new BorderLayout());
		mConnection  	= TConnection.getInstance();
		mTriggerModel   = new TDoudiaTableModel();
		mTriggerTable   = new JTable();
		mHashThread     = new Hashtable<String, TTraceCallstack>();
		mThreads        = new JTabbedPane();
		
		mConnection.setTableListener(mTriggerTable,   mTriggerModel,   "TraceTrigger");
		
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		mTriggerModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				String                  aRowId;
				TDoudiaTableModel      	aCallstackModel;
				
				if (e.getType() != TableModelEvent.UPDATE) {
					return;
				}
				
				if (mTriggerModel.mIsInitial) {
					return;
				}
				aRowId          = mTriggerModel.getRowidAt(0);				
				aCallstackModel = mTriggerModel.getChild(aRowId);
				
				TTraceCallstack aTraceCallstack = mHashThread.get(aRowId);
				
				if (aTraceCallstack == null) {
					String aThreadName = mTriggerModel.getCelByName(0, "ThreadName");
					aTraceCallstack = new TTraceCallstack();
					mHashThread.put(aRowId, aTraceCallstack);
					mThreads.add(aThreadName, aTraceCallstack);
				}
				aTraceCallstack.doPrintCallstack(aCallstackModel);
			}
		});	
		
		add(mThreads);				
	}
	// ------------------------------------------------------------------
	// The button "Refresh" clears all structure elements
	// ------------------------------------------------------------------
	public void doRefresh() {
		for (TTraceCallstack aTraceCallstack: mHashThread.values()) {
			mThreads.remove(aTraceCallstack);
		}
		mHashThread.clear();
		mTriggerModel.clearChildren();
	}	
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doUpdate() {
	}
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doReset() {		
	}
}
