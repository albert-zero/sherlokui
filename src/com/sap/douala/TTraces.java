// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Collection of trace tabs

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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")

//------------------------------------------------------------------
//------------------------------------------------------------------
public class TTraces extends JPanel implements TDoualaView {

    TTraceTrigger     mTraceTrigger;
    TTraceLeak        mTraceLeak;
    TThreads          mThreadDump;
    JTabbedPane       mTracePane;
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
        
        mTracePane      = new JTabbedPane();
        mTraceTrigger   = new TTraceTrigger();
        mTraceLeak      = new TTraceLeak();
        mThreadDump     = new TThreads();
        
        mTracePane.add("Trigger",    mTraceTrigger);
        mTracePane.add("MemoryLeak", mTraceLeak);
        mTracePane.add("Threads",    mThreadDump);
        mTracePane.add("TimeLaps",   TTimeLaps.getInstance());
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
