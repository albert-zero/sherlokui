// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Collection of trace tabs
// ------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")

//------------------------------------------------------------------
//------------------------------------------------------------------
public class TTraces extends JPanel implements TDoualaView {

	TTraceTrigger 	mTraceTrigger;
	TTraceLeak    	mTraceLeak;
	TThreads	  	mThreadDump;
	JTabbedPane 	mTracePane;
	static private TTraces mTraces = null;
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	static public TTraces getInstance() {
		if (mTraces == null) {
			mTraces = new TTraces();
		}
		return mTraces;
	}
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	private TTraces() {
		super(new BorderLayout());
		
		mTracePane 		= new JTabbedPane();
		mTraceTrigger	= new TTraceTrigger();
		mTraceLeak		= new TTraceLeak();
		mThreadDump     = new TThreads();
		
		mTracePane.add("Trigger",    mTraceTrigger);
		mTracePane.add("MemoryLeak", mTraceLeak);
		mTracePane.add("Threads", 	 mThreadDump);
		mTracePane.add("TimeLaps", 	 TTimeLaps.getInstance());
		mTracePane.add("Exception",  TException.getInstance());
		
		add(mTracePane, BorderLayout.CENTER);
		
		mTracePane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					JTabbedPane aPanel = (JTabbedPane)e.getSource();
					TDoualaView aView  = (TDoualaView)aPanel.getComponentAt(aPanel.getSelectedIndex());					
					aView.doUpdate();
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}				
			}
			
		});
	}
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void doRefresh() {
		TDoualaView aView = (TDoualaView)mTracePane.getSelectedComponent();
		aView.doRefresh();
	}

	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doUpdate() {	
		TDoualaView aView = (TDoualaView)mTracePane.getSelectedComponent();
		aView.doUpdate();
	}

	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doReset() {
		mTraceLeak.doReset();
		mThreadDump.doReset();	
		mTraceTrigger.doReset();
		TException.getInstance().doReset();
		TTimeLaps.getInstance().doReset();
	}
}
