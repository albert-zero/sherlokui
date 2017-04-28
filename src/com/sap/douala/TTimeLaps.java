// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   The TTimeLaps view collects trace events like GC, methods or exceptions.
//   The table shows the tags "Timestamp", "Event" and "Info". The rest of the info is
//   visible as tool tip or in the info status line.
// ------------------------------------------------------------------
package com.sap.douala;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
/**
 * Samples events into a table
 *///------------------------------------------------------------------
public class TTimeLaps extends JPanel implements TDoualaView {
	/** Table of event list with supported tooltips 
	 *  @see javax.swing.JToolTip */
	JTable 				mTimeTable;
	TDoudiaTableModel 	mTimeModel;
	TDoudiaTableModel 	mGarbageModel;
	TDoudiaTableModel 	mClassesModel;
	TDoudiaTableModel 	mMethodsModel;
	TDoudiaTableModel 	mExceptionModel;
	TConnection 		mConnection;
	
	/** Static singleton <code>TTimeLaps</code> instance */
	private static TTimeLaps  mInstance = null;
		
	/**
	 * @return Static singleton instance
	 *///------------------------------------------------------------------------
	public static TTimeLaps getInstance() {
		if (mInstance == null) {
			mInstance = new TTimeLaps();			
		}
		return mInstance;
	}
	
	/**
	 * Constructs a <code>TTimeLaps</code> instance.
	 *///------------------------------------------------------------------------
	private TTimeLaps() {
		super(new BorderLayout());
		
		mConnection = TConnection.getInstance();
		
		// ------------------------------------------------------------------------
		// The time table supports tooltips
		//------------------------------------------------------------------------
		mTimeTable = new JTable() {
			// ------------------------------------------------------------------------
			// ------------------------------------------------------------------------	
			public TableCellRenderer getCellRenderer(int aRow, int aColumn) {
		        return TDouala.getInstance().getRenderer();
		    }	
			// ------------------------------------------------------------------------
			// ------------------------------------------------------------------------	
			public String getToolTipText(MouseEvent e) {
		        String aToolTip = null;
		        java.awt.Point aMousePoint = e.getPoint();
		        int aRowIndex = rowAtPoint(aMousePoint);
		        int aColIndex = columnAtPoint(aMousePoint);
		        
		        int aRealColIndex = convertColumnIndexToModel(aColIndex);

		        if (aRealColIndex == 0) { 
		        	aToolTip = mTimeModel.getColumnTip(aRowIndex, aColIndex);
		        } 
		        return aToolTip;
		    }
		};
		mTimeTable.setIntercellSpacing(new Dimension(15,2));
		
		// ------------------------------------------------------------------------
		//------------------------------------------------------------------------	
		mTimeModel = new TDoudiaTableModel() {
			// ------------------------------------------------------------------
			// The table has no editable entries
			// ------------------------------------------------------------------
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			// ------------------------------------------------------------------
			// The tooltip is used to show the extended data set
			// ------------------------------------------------------------------
			public String getColumnTip(int aRowIndex, int aColIndex) {
				if (aColIndex > 0) {
					return null;
				}
				Vector<String>	  aColumns  = getRowAt(aRowIndex);;
				Vector<String>	  aColName;
				String            aKey		= getRowidAt(aRowIndex);
				Integer           aRow      = mRowInx.get(aRowIndex);
				TDoudiaTableModel aModel	= mModels.get(aKey);;

				StringBuffer aToolTip 	= new StringBuffer();
				aColumns 				= aModel.getRowAt(aRow);
				aColName				= aModel.mColumnNames;
				
				// Convert time stamp				
				aToolTip.append("<html><table border=0>");
				for (int i = 0; i < aColName.size(); i++) {
					aToolTip.append("<tr><td>");
					aToolTip.append(aColName.get(i));
					aToolTip.append("</td><td>");
					
					if (aColName.get(i).equals("Timestamp")) {
						String    aTimestampStr = aColumns.get(i).replaceAll("[.]", "");
						long      aTimestampInt = new Long(aTimestampStr).longValue();
						Timestamp aTimestamp 	= new Timestamp(aTimestampInt);
						aToolTip.append(aTimestamp.toString());
					}
					else {
						aToolTip.append(aColumns.get(i));
					}
					aToolTip.append("</td></tr>");
				}
				aToolTip.append("</table></html>");
				return aToolTip.toString();
			}			
		};
		mGarbageModel 	= new TDoudiaTableModel();
		mClassesModel   = new TDoudiaTableModel();
		mMethodsModel   = new TDoudiaTableModel();
		mExceptionModel = new TDoudiaTableModel();
		
		// Specify the supported models
		mConnection.setTableListener(mTimeTable, mTimeModel, "TimeLaps");
		mConnection.setTableListener(null, mGarbageModel, 	 "GC");
		mConnection.setTableListener(null, mGarbageModel, 	 "GCV9");
		mConnection.setTableListener(null, mClassesModel, 	 "ClassLoad");
		mConnection.setTableListener(null, mClassesModel, 	 "ClassUnload");
		mConnection.setTableListener(null, mMethodsModel, 	 "Performance");
		mConnection.setTableListener(null, mExceptionModel,  "Exception");
		
		mTimeModel.mColumnNames = new Vector<String>();
		mTimeModel.mColumnNames.add("Event");
		mTimeModel.mColumnNames.add("Timestamp");
		mTimeModel.mColumnNames.add("Info");
		mTimeModel.setDataVector();
		
		// ------------------------------------------------------------------------
		//------------------------------------------------------------------------	
		mGarbageModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					setTimeRow("GC", (TDoudiaTableModel)e.getSource(), "Type", "Time");
				}
			}
		});

		// ------------------------------------------------------------------------
		//------------------------------------------------------------------------	
		mClassesModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					setTimeRow("Classes", (TDoudiaTableModel)e.getSource(), "Type", "ClassName");
				}
			}
		});

		// ------------------------------------------------------------------------
		//------------------------------------------------------------------------	
		mMethodsModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					setTimeRow("Performance", (TDoudiaTableModel)e.getSource(), "Event", "MethodName");
				}
			}
		});
		// ------------------------------------------------------------------------
		//------------------------------------------------------------------------	
		mExceptionModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					setTimeRow("Exception", (TDoudiaTableModel)e.getSource(), "Type", "Signature");
				}
			}
		});
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		mTimeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				ListSelectionModel 	aSelModel;
				TDoudiaTableModel   aModel;
				Vector<String>		aColumns;
				Vector<String>		aNames;
				
				if (e.getValueIsAdjusting()) {
					return;
				}
				aSelModel = (ListSelectionModel)e.getSource();
				
		        if (!aSelModel.isSelectionEmpty()) {
		        	int 	aRowInx = aSelModel.getMinSelectionIndex();
		        	int 	aColInx = aSelModel.getMinSelectionIndex();
		        	if (aRowInx < 0 || aColInx < 0) {
		        		return;
		        	}
		            
		        	String  aKey 	= mTimeModel.getRowidAt(aRowInx);
		            Integer aRow    = mTimeModel.mRowInx.get(aRowInx);

		            aModel 		= mTimeModel.getChild(aKey);
		            aColumns	= aModel.getRowAt(aRow);
		            aNames      = aModel.mColumnNames;
		            	
		            aModel      = mConnection.getModel("Info");		            
		            aModel.clearRows();

		            String aOutput = new String();
	            	Vector<String> aNexCol = new Vector<String>();
	            	int i;
	            	
	            	for (i = 0; i < aModel.mColumnNames.size(); i++) {
	            		aNexCol.add("");
	            	}
	            	
	            	if (aKey.compareTo("Exception") == 0) {
	            		String aCatch = new String("Catch=[ ");
	            		String aThrow = new String("Throw=[ ");
	            		String aSepCatch  = new String("");
	            		String aSepThrow  = new String("");
	            		String aSepOut    = new String("");
	            		
	            		for (i = 0; i < aColumns.size(); i++) {
	            			if (aNames.get(i).startsWith("Catch")) {
	            				aCatch += aSepCatch + aColumns.get(i);
	            				aSepCatch = ", ";
	            			}
	            			else if (aNames.get(i).startsWith("Throw")) {
	            				aThrow += aSepThrow + aColumns.get(i);
	            				aSepThrow = ", ";
	            			}
	            			else {
	            				aOutput += aSepOut + aNames.get(i) + "=" + aColumns.get(i);
	            				aSepOut = ", ";
	            			}
	            		}
	            		aCatch += "]";
	            		aThrow += "]";
	            		
	            		aNexCol = new Vector<String>();
	            		aNexCol.add(0, aOutput);
	            		aModel.addRow(aNexCol, "0x0", 0);
	            		
	            		aNexCol = new Vector<String>();
	            		aNexCol.add(0, aThrow);
	            		aModel.addRow(aNexCol,  "0x0", 0);

	            		aNexCol = new Vector<String>();
	            		aNexCol.add(0, aCatch);
	            		aModel.addRow(aNexCol,  "0x0", 0);
	            	}
	            	else {
	            		for (i = 0; i < aColumns.size(); i++) {
	            			aOutput += " " + aNames.get(i) + "=" + aColumns.get(i);
	            		}
	            		aNexCol.set(0, aOutput);
	            		aModel.addRow(aNexCol, "0x0", 0);
	            	}	            	
		            aModel.setDataVector();
		        }				
			}
		});
		
		JScrollPane aTimePane = new JScrollPane(mTimeTable);
		add(aTimePane, BorderLayout.CENTER);
	}

	/**
	 * Add a row to <code>TTimeLaps</code> model. The master model points 
	 * to the different timer events. 
	 * <ul>
	 * <li>GC</li>
	 * <li>Classes</li>
	 * <li>Performance</li>
	 * <li>Exception</li>
	 * </ul>
	 * @param aKey Value of XML tag <code>Type</code>
	 * @param aMasterModel Master model for different timer events 
	 * @param aEventColumn The event column name is either <code>Type</code> or <code>Event</code>
	 * @param aInfoColumn Choose the info column by name
	 *///------------------------------------------------------------------------
	void setTimeRow(
			String 				aKey, 
			TDoudiaTableModel 	aMasterModel, 
			String				aEventColumn,
			String 				aInfoColumn) {
		
		int                 aRowInx;
		TDoudiaTableModel 	aModel;
		Vector<String> 		aTimeCol = new Vector<String>();
		
		aModel = mTimeModel.getChild(aKey);
		if (aModel.mIsInitial) {
			aModel.mIsInitial = false;
			aModel.mColumnNames = aMasterModel.mColumnNames;
		}
		
		aRowInx = aMasterModel.getRowCount() - 1;
		if (aRowInx < 0) {
			return;
		}
		
		Vector<String> aRow = aMasterModel.getRowAt(aRowInx);
		aModel.addRow(aRow, aKey, aRowInx);
		aRowInx = aModel.getRowCount() - 1;		
		
		aTimeCol.add("");
		aTimeCol.add("");
		aTimeCol.add("");
		
		for (int i = 0; i < aModel.mColumnNames.size(); i++) {
			if (aModel.mColumnNames.get(i).compareTo(aEventColumn) == 0) {
				aTimeCol.set(0, aRow.get(i));
			}
			if (aModel.mColumnNames.get(i).compareTo("Timestamp") == 0) {
				aTimeCol.set(1, aRow.get(i));
			}
			if (aModel.mColumnNames.get(i).compareTo(aInfoColumn) == 0) {
				aTimeCol.set(2, aRow.get(i));
			}
		}
		mTimeModel.addRow(aTimeCol, aKey, aRowInx);
		mTimeModel.fireTableDataChanged();	
		aMasterModel.clearRows();
	}

	/**
	 * Clears the entire table 
	 *///------------------------------------------------------------------------
	public void doRefresh() {
		mTimeModel.clearChildren();
		mTimeModel.clearRows();
		mTimeModel.fireTableDataChanged();
		doUpdate();
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	public void doUpdate() {
	}
	
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	public void doReset() {		
	}
}
