// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Manage the login
//   Create a unique renderer
//   Start the user interface

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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
/**
 * Aligns all integer values to right cell boundary
 * Set cell spacing for all text entries
 * Set the selection color
 *///------------------------------------------------------------------
class TAllignRenderer extends JComponent implements TableCellRenderer {    
    /** Label component for column cell */
    JLabel    mText = new JLabel();
    
    /**
     * @return Label with applied rendering 
     * @param aTable Table to draw
     * @param aValue Value do draw
     * @param isSelected Selection state
     * @param aRow Selected row index
     * @param aCol Selected column index
     */// ------------------------------------------------------------------
    public Component getTableCellRendererComponent(
            JTable  aTable, 
            Object  aValue, 
            boolean isSelected, 
            boolean hasFocus,
            int     aRow, 
            int     aCol) {
        
        if (aValue == null || ((String)aValue).length() == 0) {
            return null;
        }
        char aDigit = ((String)aValue).charAt(0);
        mText.setFont(aTable.getFont());
        
        if (isSelected) {            
            mText.setOpaque(true);
            mText.setBackground(Color.lightGray);
        }
        else {
            mText.setBackground(Color.white);
        }
        
        if (Character.isDigit(aDigit)) {
            mText.setHorizontalAlignment(SwingConstants.TRAILING);
        }
        else {
            mText.setHorizontalAlignment(SwingConstants.LEADING);
        }
        mText.setText((String)aValue);
        return mText;
    }
};        

@SuppressWarnings("serial")
// ------------------------------------------------------------------
// Main class
// ------------------------------------------------------------------
public class TDouala extends JPanel {    
    boolean             mConnected = false;
    TLogin              mLogin;
    JPanel              mButtonPanel;
    JTabbedPane         mTabbedPanel;
    TDoudiaTableModel   mStatusModel;
    TClasses            mClasses;
    TMethods            mMethods;
    TTraces             mTracers;
    TState              mStates;
    TAllignRenderer     mRenderer;
    
    JToggleButton       mConnectButton;
    JToggleButton       mMonitorButton;
    JButton             mRefreshButton;
    JToggleButton       mTracersButton;
    JButton             mGarbageButton;
    JTable              mStatusTable;    
    JButton             mTrace;
    JButton             mLogging;
    JButton             mReqestGC;
    JButton             mRefresh;
    JButton             mFileButton;
    TConnection         mConnection = TConnection.getInstance();
    TState              mState      = TState.getInstance();

    static String           mLogFile  = null;    
    static private TDouala  mInstance = null;
    
    // ------------------------------------------------------------------
    // Access singleton
    // ------------------------------------------------------------------
    public static TDouala getInstance() {
        if (mInstance == null) {
            mInstance = new TDouala();
        }
        return mInstance;        
    }

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------
    TDouala() {
        super(new BorderLayout());
        mClasses            = TClasses.getInstance();
        mMethods            = TMethods.getInstance();
        mStates             = TState.getInstance();
        mStatusModel        = new TDoudiaTableModel();
        mRenderer           = new TAllignRenderer();
        mLogin              = TLogin.getInstance();
        mTracers            = TTraces.getInstance();
        mStatusTable        = new JTable();

        mTabbedPanel        = new JTabbedPane() {
            public void paint(Graphics g) {
                super.paint(g);
            }
        };    
        
        mTabbedPanel.add("Login",    mLogin);
        mTabbedPanel.add("State",    mStates);
        mTabbedPanel.add("Classes",  mClasses);
        mTabbedPanel.add("Methods",  mMethods);
        mTabbedPanel.add("Trace",    mTracers);
        
        mButtonPanel        = new JPanel();
        mConnectButton      = new JToggleButton("Disconnected");
        mMonitorButton      = new JToggleButton("Mointor");
        mTracersButton      = new JToggleButton("Trace");
        mGarbageButton      = new JButton("GC");
        mRefreshButton      = new JButton("Refresh");
        mFileButton         = new JButton("LogFile");
        
        mMonitorButton.setEnabled(false);
        
        GridBagConstraints aConstraints;
        GridBagLayout  aButtonLayout = new GridBagLayout();
        mButtonPanel.setLayout(aButtonLayout);
        aConstraints        = new GridBagConstraints();
        aConstraints.fill   = GridBagConstraints.HORIZONTAL;
        aConstraints.gridx  = 0;
        aConstraints.gridy  = GridBagConstraints.RELATIVE;        
        aConstraints.insets = new Insets(10,10,0,10);
        
        mButtonPanel.add(mConnectButton, aConstraints);        
        mButtonPanel.add(mMonitorButton, aConstraints);
        mButtonPanel.add(mTracersButton, aConstraints);
        mButtonPanel.add(mGarbageButton, aConstraints);
        mButtonPanel.add(mRefreshButton, aConstraints);
        mButtonPanel.add(mFileButton, aConstraints);
        
        mStatusTable.setShowVerticalLines(false);    
        mConnection.setTableListener(mStatusTable, mStatusModel, "Info");
        mStatusModel.mConnected = false;

        JPanel aStatusPanel = new JPanel(new GridBagLayout());
        
        aConstraints        = new GridBagConstraints();
        aConstraints.fill   = GridBagConstraints.HORIZONTAL;
        aConstraints.insets = new Insets(10,10,10,10);
        aConstraints.gridx  = 0;
        aConstraints.gridy  = 0;
        aConstraints.weightx= 0.1;
        aStatusPanel.add(new JLabel("Info:"), aConstraints);
        aConstraints.weightx= 0.9;
        aConstraints.gridx  = 1;
        aStatusPanel.add(mStatusTable, aConstraints);
        
        add(mTabbedPanel, BorderLayout.CENTER);
        add(mButtonPanel, BorderLayout.EAST);
        add(aStatusPanel, BorderLayout.SOUTH);

        doReset();
        mStatusModel.mColumnNames = new Vector<String>();
        mStatusModel.mColumnNames.add("Message");
        mStatusModel.getDataVector();
        // ------------------------------------------------------------------
        // Manage mouse action on panel
        // ------------------------------------------------------------------
        mTabbedPanel.addChangeListener(new ChangeListener(){                
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
        
        // ------------------------------------------------------------------
        // ------------------------------------------------------------------
        mFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser aChooser = new JFileChooser();
                int returnVal = aChooser.showOpenDialog(TDouala.getInstance());
                
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    mLogFile = aChooser.getSelectedFile().getAbsolutePath();
                    mLogin.doLogin();
                }
            }            
        });
        
        // ------------------------------------------------------------------
        // Button pressed "Connection"
        // ------------------------------------------------------------------
        mConnectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (mConnection.isConnected()) {
                        mConnection.doCommand("exit");
                    }
                    else {
                        mLogin.doLogin();
                    }
                } catch (Exception ex) {
                }
            }
        });        
        // ------------------------------------------------------------------
        // Button pressed "GC"
        // ------------------------------------------------------------------        
        mGarbageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mConnection.doCommand("gc");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });        
        // ------------------------------------------------------------------
        // Button pressed "Refresh"
        // ------------------------------------------------------------------
        mRefreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TDoualaView aView = (TDoualaView)mTabbedPanel.getSelectedComponent();
                aView.doRefresh();
            }
        });        
        // ------------------------------------------------------------------
        // Button pressed "Monitor"
        // ------------------------------------------------------------------
        mMonitorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((AbstractButton)e.getSource()).isSelected()) {
                    mState.startMonitor();
                }
                else {
                    mState.stopMonitor();
                }
            }
        });        
        // ------------------------------------------------------------------
        // Button pressed "Trace"
        // ------------------------------------------------------------------
        mTracersButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((AbstractButton)e.getSource()).isSelected()) {
                    mState.startTrace();
                }
                else {
                    mState.stopTrace();
                }
            }
        });    
    }

    // ------------------------------------------------------------------
    // Resets the button labels according to the connection state
    // ------------------------------------------------------------------
    public void doReset() {
        if (TConnection.getInstance().isConnected()) {
            mMonitorButton.setEnabled(true);
            mTracersButton.setEnabled(true);
            mGarbageButton.setEnabled(true);
            mRefreshButton.setEnabled(true);
            mConnectButton.setSelected(true);
            mConnectButton.setText("Connected");                
        }
        else {
            mMonitorButton.setEnabled(false);
            mTracersButton.setEnabled(false);
            mGarbageButton.setEnabled(false);
            mRefreshButton.setEnabled(false);
            mConnectButton.setSelected(false);
            mConnectButton.setText("Disconnected");                
            
        }
    }
    
    // ------------------------------------------------------------------
    // Set the state according the returned values for command "lss" 
    // ------------------------------------------------------------------
    public void setState(String aKey, String aValue) {
        if (aKey.equals("Monitor")) {
            if (aValue.indexOf("running") >= 0) {
                mMonitorButton.setText("Monitor Running");
                mMonitorButton.setSelected(true);
            }
            else {
                mMonitorButton.setText("Monitor Stopped");
                mMonitorButton.setSelected(false);
            }
            return;
        }

        if (aKey.equals("Trace")) {
            if (aValue.indexOf("started") >= 0) {
                mTracersButton.setText("Trace Running");
                mTracersButton.setSelected(true);
            }
            else {
                mTracersButton.setText("Trace Stopped");
                mTracersButton.setSelected(false);
            }
            return;
        }
        
        
    }
    // ------------------------------------------------------------------
    // Access to global cell renderer
    // ------------------------------------------------------------------
    public TableCellRenderer getRenderer() {
        return mRenderer;        
    }
    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    public String getLogFile() {
        return mLogFile;
    }
    // ------------------------------------------------------------------
    // Start the user interface
    // ------------------------------------------------------------------
    private static void createAndShowGUI() {
        JFrame aFrame = new JFrame("Douala@SAP");
        
        URL aUrl = ClassLoader.getSystemResource("icons/sherlok.jpg");
        if (aUrl != null) {
            Image  aImage = Toolkit.getDefaultToolkit().getImage(aUrl);
            aFrame.setIconImage(aImage);
        }
        aFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TDouala aContentPanel = getInstance();
        aContentPanel.setOpaque(true);
        
        aFrame.add(aContentPanel);
        aFrame.pack();
        aFrame.setVisible(true);
    }

    // ------------------------------------------------------------------
    // The main program
    // ------------------------------------------------------------------
    public static void main(String[] args) throws Exception {        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
