// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   The state panel allows to view and change the profiler settings
// ------------------------------------------------------------------
package com.sap.douala;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
// ------------------------------------------------------------------
// This renderer sets the ComboBox-Editor for files selection 
// ------------------------------------------------------------------
class TPropertyRenderer extends JComponent implements TableCellRenderer {	
	JComboBox mList = new JComboBox();
	
	public Component getTableCellRendererComponent(
			JTable 	aTable, 
			Object 	aValue, 
			boolean isSelected, 
			boolean hasFocus,
			int 	aRow, 
			int 	aCol) {
		
		mList.setFont(aTable.getFont());
		return mList;
	}	
	public void setComponent(JComboBox aComponent) {
		mList = aComponent;
	}
};	

@SuppressWarnings("serial")
// ------------------------------------------------------------------
// ------------------------------------------------------------------
public class TState extends JPanel implements TDoualaView {
	private TDoudiaTableModel mStatisticModel;
	private TDoudiaTableModel mPropertyModel;
	private	TDoudiaTableModel mFilesModel;
	private static TState mState = null;
	String  mConfigFile 	= null;
	JComboBox mFileCombo 	= new JComboBox();
	
	TConnection mConnection = TConnection.getInstance();
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public static TState getInstance() {
		if (mState == null) {
			mState = new TState();			
		}
		return mState;
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	private TState() {
		super(new BorderLayout());
		
		JPanel aSubPanel  = new JPanel();
		aSubPanel.setLayout(new BoxLayout(aSubPanel, BoxLayout.Y_AXIS));
		
		JTable  aStatistic;
		JTable  aProperty;
			    
		// ------------------------------------------------------------------
		// ------------------------------------------------------------------
    	aStatistic 	= new JTable() {
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {				
		        return TDouala.getInstance().getRenderer();
		    }			
    	};

    	// ------------------------------------------------------------------
		// ------------------------------------------------------------------
    	aProperty  = new JTable() {
			// ------------------------------------------------------------------
			// ------------------------------------------------------------------
			public TableCellRenderer getCellRenderer(int aRow, int aCol) {
				String aProperty = (String)getModel().getValueAt(aRow, 0);
				if (aProperty.equals("ConfigFile")&& aCol == 1) {
					TPropertyRenderer aRenderer = new TPropertyRenderer();
					aRenderer.setComponent(mFileCombo);
					return aRenderer;
				}
		        return TDouala.getInstance().getRenderer();
		    }    		
			// ------------------------------------------------------------------
			// ------------------------------------------------------------------
			public TableCellEditor getCellEditor(
					int aRow,
                    int aCol) {
				
				String aProperty = (String)getModel().getValueAt(aRow, 0);
				if (aCol == 1 && aProperty.equals("ConfigFile")) {		
					TableCellEditor aCellEditor = new DefaultCellEditor(mFileCombo);
					return aCellEditor;
				}
				return super.getCellEditor(aRow, aCol);				
			}
			// ------------------------------------------------------------------
			// ------------------------------------------------------------------
			public void setValueAt(Object aValue, int aRow, int aCol)  {				
				super.setValueAt(aValue, aRow, aCol);
			}
    	};
    	aStatistic.setIntercellSpacing(new Dimension(15,2));
    	aProperty.setIntercellSpacing(new Dimension(15,2));
    	
	    mStatisticModel	= new TDoudiaTableModel();
	    mPropertyModel 	= new TDoudiaTableModel();
	    mFilesModel     = new TDoudiaTableModel();
	    
	    mConnection.setTableListener(aStatistic, 	mStatisticModel, 	"Statistic");
	    mConnection.setTableListener(aProperty,  	mPropertyModel,  	"Config");
	    mConnection.setTableListener(null,  		mFilesModel,  		"File");
	    	    
    	// ------------------------------------------------------------------
		// ------------------------------------------------------------------
	    mStatisticModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				for (int i = 0; i < mStatisticModel.getRowCount(); i++) {
					TDouala.getInstance().setState(
							(String)mStatisticModel.getValueAt(i, 0),
							(String)mStatisticModel.getValueAt(i, 1));
				}
			}	    	
	    });
	    
    	// ------------------------------------------------------------------
		// ------------------------------------------------------------------
	    mPropertyModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int 	aRow 	= e.getFirstRow();
				int 	aCol 	= e.getColumn();
				String 	aKey;
				String 	aValue;
								
				if (e.getType() == TableModelEvent.UPDATE && mPropertyModel.mReady) {
					if (aCol >= 0 && aRow >= 0) {													
						aKey   	= (String)mPropertyModel.getValueAt(aRow, 0);
						aValue 	= (String)mPropertyModel.getValueAt(aRow, 1);
										
						mConnection.doCommand("set " + aKey + "=" + aValue);
						if (!aKey.equals("Tracer")) {
							mConnection.doCommand("reset -s");
						}
						if (aKey.equals("ConfigFile")) {
							doUpdate();
						}
					}
					else if (mConfigFile == null) {
						for (int i = 0; i < mPropertyModel.getRowCount(); i++) {
							aKey   	= (String)mPropertyModel.getValueAt(i, 0);
							aValue 	= (String)mPropertyModel.getValueAt(i, 1);
					
							if (aKey.equals("ConfigFile")) {
								mConfigFile = aValue;
								mFileCombo.addItem(mConfigFile);
							}
						}
					}
				}
			}
	    });
	    
    	// ------------------------------------------------------------------
		// ------------------------------------------------------------------
	    mFilesModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int i;
				String aFileName   = "";
				
				if (mFileCombo.getItemCount() == mFilesModel.getRowCount()) {
					return;
				}
				mFileCombo.removeAllItems();
				
				for (i = 0; i < mFilesModel.getRowCount(); i++) {
					aFileName = mFilesModel.getCelByName(i, "FileName");
					if (aFileName == null) {
						aFileName = mConfigFile;
					}
					if (aFileName == null) {
						return;
					}
					mFileCombo.addItem(aFileName);
					if (aFileName.equals(mConfigFile)) {
						mFileCombo.setSelectedIndex(i);
					}					
				}
				mPropertyModel.fireTableDataChanged();
				mConfigFile = null;
			}
		});
	    
	    mFileCombo.setEditable(true);
	    
		JScrollPane aScrollPane;

		aScrollPane  = new JScrollPane(aStatistic);
		aScrollPane.setPreferredSize(new Dimension(100, 200));
		aSubPanel.add(aScrollPane);
		
		aScrollPane = new JScrollPane(aProperty);
		aSubPanel.add(aScrollPane);
		setPreferredSize(new Dimension(100, 500));

		add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		add(aSubPanel, BorderLayout.CENTER);		
	}
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	void startMonitor() {
		mConnection.doCommand("start monitor");
		mConnection.doCommand("lss");
	}
	
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	void stopMonitor() {
		mConnection.doCommand("stop monitor");
		mConnection.doCommand("lss");
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	void startTrace() {
		mConnection.doCommand("start trace");
		mConnection.doCommand("lss");
	}
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	void stopTrace() {
		mConnection.doCommand("stop trace");
		mConnection.doCommand("lss");
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	void doReset() {
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void doRefresh() {
		doUpdate();
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void doUpdate() {
		mPropertyModel.mReady = false;
		mConnection.doCommand("lss");
		mConnection.doCommand("lsp");
		mConnection.doCommand("lcf");
		mPropertyModel.mReady = true;
		TDouala.getInstance().doReset();
	}
}
