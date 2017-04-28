// ----------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   Create connection to Sherlok
//   Implement communication protocol
//   Create a common table model
// ----------------------------------------------------------------------
package com.sap.douala;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


@SuppressWarnings("serial")

/**
 *	The table model organizes flexible rows and columns.
 *  Each TDoudiaTableModel has a list of children, which represent
 *  hierarchical XML-tag structures.
 */// ----------------------------------------------------------------
class TDoudiaTableModel extends DefaultTableModel {

	// The table header
	protected Vector<String> 			mColumnNames;
	
	// The table as string-string vector of rows 
	private  Vector<Vector<String>> 	mRows;
	
	// The object ID
	protected Vector<String> 			mRowId;

	// The row index allows to link two models  
	protected Vector<Integer> 			mRowInx;
	
	String 			mInfo 			= "";
	int 			mRowIndex 		= 0;
	int 			mState 			= 0;
	public boolean 	mConnected 		= false;
	public boolean  mKeepRows       = false;
	public boolean  mIsBlock    	= false;
	public boolean  mIsInitial  	= true;
	boolean  		mClearRows  	= false;
	public boolean  mReady          = true;
	public boolean  mModelChanged 	= false;
	public HashMap<String, String> mFilters;
	public String   mHeader		  	= "";
	public String   mElementName  	= "";
	protected String  mKey          = "";
	public boolean  mToolTip      	= false;
	HashMap<String, TDoudiaTableModel> mModels;

	/**
	 * Constructs a table model
	 */// ------------------------------------------------------------------
	public TDoudiaTableModel() {
		super();
		mIsInitial 		= true;
		mReady          = true;
		mConnected 		= false;
		mColumnNames 	= new Vector<String>();
		mRows 			= new Vector<Vector<String>>();
		mRowId          = new Vector<String>();
		mRowInx         = new Vector<Integer>();
		mFilters        = new HashMap<String, String>();
		mModels         = new HashMap<String, TDoudiaTableModel>();
	}
	
	/**
	 * Interface to generate a <em>tooltip</em> for a cell entry. 
	 * @see javax.swing.JToolTip
	 * @param aRowIndex Index of the selected row
	 * @param aColIndex Index of the selected column
	 * @return help text
	 *///--------------------------------------------------------------------
	public String getColumnTip(int aRowIndex, int aColIndex) {
		return null;
	}

	/**
	 * Find the object ID of a given row
	 * @param aRowIndex Index of the selected row
	 * @return object ID
	 *///--------------------------------------------------------------------
	public String getRowidAt(int aRowIndex) {
		return mRowId.get(aRowIndex);
	}

	/**
	 * Get a table row at a given index as a vector of strings 
	 * @param aRowIndex Index of the selected row
	 * @return string vector of values 
	 *///--------------------------------------------------------------------
	public Vector<String> getRowAt(int aRowIndex) {
		return mRows.get(aRowIndex);
	}
	
	/**
	 * Add a row to the model. The model can decide to add a row or to 
	 * merge according the object ID's. The model has to set the
	 * desired strategy by setting member <code>mKeepRows=true</code>
	 * @param aColumn String vector as column values
	 * @param aRowId Object ID
	 * @param aInx Row index to organize cross references
	 *///--------------------------------------------------------------------
	public void addRow(Vector<String> aColumn, String aRowId, int aInx) {
		// It's possible to change the view without deleting existing rows
		if (mKeepRows && !aRowId.equals("0x0")) {
			for (int i = 0; i < mRows.size(); i++) {
				if (mRowId.get(i).equals(aRowId)) {
					mRows.remove(i);
					mRowId.remove(i);
					mRowInx.remove(i);
					break;
				}
			}
		}
		mRowId.add(aRowId);
		mRows.add(aColumn);
		mRowInx.add(new Integer(aInx));
	}
	
	/**
	 * Get the value content of a column by name. If a column with the given name is not
	 * found, the value of column with the name <em>"Type"</em> is returned instead.
	 * @param aRowIndex Index of the selected row
	 * @param aName header name of the column
	 * @return string value for the given column 
	 *///--------------------------------------------------------------------
	public String getCelByName(int aRowIndex, String aName) {		
		Vector<String> aColumn	= null;
		String aType 			= null;
		
		if (mRows.size() < aRowIndex+1) {
			return null;
		}
		aColumn = mRows.get(aRowIndex);
		for (int i = 0; i < Math.min(mColumnNames.size(), aColumn.size()); i++) {
			if (mColumnNames.get(i).equals(aName)) {
				return aColumn.get(i);
			}
			if (mColumnNames.get(i).equals("Type")) {
				aType = aColumn.get(i);
			}
		}
		return aType;
	}

	/**
	 * Clears all rows for a new request/response cycle
	 *///--------------------------------------------------------------------
	public void clearRows() {
		mRows.clear();
		mRowId.clear();
		mRowInx.clear();
		setRowCount(0);
	}
	
	/**
	 * Set a reference to the rows of the source or merge models. The merge mode will assure, that 
	 * each object <em>ID</em> will stay unique. The model has to set the
	 * desired strategy by setting member <code>mKeepRows=true</code>
	 * @param aModel Source container model
	 *///--------------------------------------------------------------------
	public void setRows(TDoudiaTableModel aModel) {
		if (mKeepRows) {
			for (int i = 0; i < aModel.mRows.size(); i++) {
				addRow(aModel.mRows.get(i), aModel.mRowId.get(i), 0);
			}
		}
		else {
			mRows  	= aModel.mRows;
			mRowId 	= aModel.mRowId;
			mRowInx = aModel.mRowInx; 
		}
	}
	
	/**
	 * @return Number of rows
	 */// ------------------------------------------------------------------
	public int getRowCount() {
		if (mRows == null) 
			return 0;
		return mRows.size();
	}
	
	/**
	 * Shrink the result set. This method cuts the table to the given count.
	 * @param aCount maximal row index to keep  
	 */// ------------------------------------------------------------------
	public void setRowCount(int aCount) {
		if (mRows != null && aCount > mRows.size()) {
			mRows.setSize(aCount);
			mRowId.setSize(aCount);
			mRowInx.setSize(aCount);
		}
	}

	/**
	 * Make all cells editable.
	 * @return <code>true</code>
	 *///------------------------------------------------------------------
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	/**
	 * Gets the child model for a given key. If the child doesn't exist this
	 * method would create an object for this key. 
	 * The key represents the 
	 * XML element name for sub structures.  
	 * @param aKey Unique key identifier 
	 * @return Child model
	 *///------------------------------------------------------------------
	public TDoudiaTableModel getChild(String aKey) {
		TDoudiaTableModel aModel;
		aModel = mModels.get(aKey);

		if (aModel == null) {
			aModel = new TDoudiaTableModel();
			mModels.put(aKey, aModel);
		}
		return aModel;
	}	

	/**
	 * Gets a child model for a given key.
	 * @see #getChild(String)
	 * @param aKey
	 * @return Child model or null, if no model found with given key 
	 *///------------------------------------------------------------------
	public TDoudiaTableModel findChild(String aKey) {
		TDoudiaTableModel aModel;
		aModel = mModels.get(aKey);
		return aModel;
	}	

	/**
	 * Removes all children from model 
	 *///------------------------------------------------------------------
	public void clearChildren() {
		mModels.clear();
	}	

	/**
	 * @return Number of columns
	 *///------------------------------------------------------------------
	public int getColumnCount() {
		return mColumnNames.size();
	}

	/**
	 * @return Column name at the specified index
	 *///------------------------------------------------------------------
	public String getColumnName(int aInx) {
		return mColumnNames.get(aInx);
	}
	
	/**
	 * Add a given model as child. 
	 * @param aKey Unique key to access the child model
	 * @param aModel Existing model to register
	 *///------------------------------------------------------------------
	public void setChild(String aKey, TDoudiaTableModel aModel) {
		mModels.put(aKey, aModel);	
	}

	/**
	 * The filter string is displayed as part of the table. This
	 * method detects the filter row and subtracts this from the number
	 * of rows if necessary
	 * @return number of rows in result set without filter row
	 */// ------------------------------------------------------------------
	public int getResultRows() {
		int aRows = mRows.size();
		if (mFilters.size() > 0) {
			aRows--;
		}
		return aRows;
	}

	/**
	 * Add a new row to the model
	 * @param aNewRow string array representing the new row entry
	 *///------------------------------------------------------------------
	public void addRow(String[] aNewRow) {
		Vector<String> aRow = new Vector<String>();
		
		for (int i = 0; i < mColumnNames.size(); i++) {
			if (i < aNewRow.length) {
				aRow.add(aNewRow[i]);
			}
			else {
				aRow.add("");
			}
		}
		addRow(aRow, "0x0", 0);
	}

	/**
	 * Update filter row before output
	 *///------------------------------------------------------------------
	public void check() {
		String  aName;
		String  aFilter;
		Vector<String> aColumn;
		
		boolean aHasFilter = mFilters.size() > 0;
		boolean aHasRows   = mRows.size() 	 > 0;
		
		if (aHasRows) {
			if (!aHasFilter) {
				return;
			}
		}	
		// Force output of an empty line for filter input 
		aHasFilter 	= !aHasRows;		
		aColumn 	= new Vector<String>();
		
		for (int i = 0; i < mColumnNames.size(); i++) {
			aName   = (String)mColumnNames.elementAt(i);
			aFilter = mFilters.get(aName);

			if (aFilter == null || aFilter.equals("")) {
				aFilter = "";			
			}
			aColumn.add(aFilter);
		}		
    	mRows.add(aColumn);
    	mRowId.add("0x0");
	}
	
	/**
	 * Sets the data vector for the user interface
	 * @see javax.swing.table.DefaultTableModel#setDataVector(Vector, Vector)
	 *///------------------------------------------------------------------
	void setDataVector() {
		super.setDataVector(mRows, mColumnNames);
	}
}

// @SuppressWarnings("serial")
/**
 * TElement collects the stream information for each XML-tag.
 * An element searches its model using the attributes <code>Type</code> and <code>Detail</code>
 * Tags without <code>Type</code> attribute has no model and is not visible.
 *///------------------------------------------------------------------
class TElement {
	/** String vector of column names */
	public Vector<String> 	mColumnNames;
	/** String vector of column entries */
	public Vector<String> 	mColumnEntry;
	/** XML tag name */
	public String		 	mTagName;
	/** Value for XML attribute <code>Type</code> */
	public String 			mKey;
	/** Value for XML attribute <code>Detail</code> */
	public String 			mDetail;
	/** Value for XML attribute <code>ID</code> */
	public String 			mRowId;
	/** <code>true</code> for XML tag <code>Traces</code> and <code>List</code> */
	public boolean          mIsBlock;
	/** Model for XML structure */
	TDoudiaTableModel       mModel;

	/**
	 * Constructs a TElement
	 *///------------------------------------------------------------------
	public TElement ( ) {
		mColumnNames = new Vector<String>();
		mColumnEntry = new Vector<String>();
		mIsBlock     = false;
		mModel       = null;
	}
	
}

// @SuppressWarnings("serial")
/**
 * Implements the XML handler for the protocol
 *///------------------------------------------------------------------
class TDoudiaXmlHandler extends DefaultHandler {
	int 	aIndex 				= 0;
	boolean mModelChanged 		= true;
	TConnection					mConnection;
	Vector<TDoudiaTableModel> 	mModelStack;
	Vector<TElement> 			mElementStack;
	
	/** Static singleton instance */
	public static TDoudiaXmlHandler mXmlHandler = null;
	
	/**
	 * @return Static singleton XML handler instance
	 *///------------------------------------------------------------------
	public static TDoudiaXmlHandler getInstance() {
		if (mXmlHandler == null) {
			mXmlHandler = new TDoudiaXmlHandler();
			mXmlHandler.mElementStack = new Vector<TElement>();
			mXmlHandler.mModelStack   = new Vector<TDoudiaTableModel>();
			mXmlHandler.mConnection   = TConnection.getInstance();
		}
		return mXmlHandler;
	}

	/**
	 * 
	 * @return Parser instance for XML handler
	 * @throws SAXException
	 *///------------------------------------------------------------------
	public static XMLReader getParser() throws SAXException {
		XMLReader 		  aReader;
		TDoudiaXmlHandler aXmlHandler = getInstance();
		aReader = XMLReaderFactory.createXMLReader();
		aReader.setContentHandler(aXmlHandler);
		
		aXmlHandler.mElementStack.clear();
		aXmlHandler.mModelStack.clear();
		
		return aReader;
	}

	/**
	 * Constructs the XML handler
	 *///------------------------------------------------------------------
	private TDoudiaXmlHandler() {
		super();
	}

	/**
	 * Callback for the start of an XML element tag. 
	 * Depending on the <code>Type</code>, the element searches the corresponding model.
	 * The local name and the attribute <code>Key</code> are used to organize the model hierarchy.
	 * @param aUri Unique resource 
	 * @param aLocalName Local name of the XML element. 
	 * @param aName Name of the XML element
	 * @param aAttributes List of attribute value pairs
	 *///------------------------------------------------------------------
	public void startElement(
			String 		aUri, 
			String 		aLocalName, 
			String 		aName, 
			Attributes 	aAttributes) throws SAXException {
		
		int i;
		int aInx;
		TDoudiaTableModel 	aModel	    = null;
		TDoudiaTableModel 	aModelChild = null;
		TElement 			aElement    = new TElement();
		TElement            aLast       = null;
		String 				aRowId      = "0x0";
		String              aException  = null;
		TDoudiaTableModel   aInfoModel  = TConnection.getInstance().getModel("Info");
		
		aInx  = mElementStack.size();
		if (aInx > 0) {
			aLast  = mElementStack.get(aInx-1);
			aRowId = aLast.mKey;
		} 
		else {
			synchronized (aInfoModel) {
				aInfoModel.mConnected = true;
				aInfoModel.notifyAll();
			}
		}
		mElementStack.add(aElement);
				
		aElement.mTagName = aName;
		aElement.mModel	  = null;
		aElement.mRowId   = "0x0";
		
		for (i = 0; i < aAttributes.getLength(); i++) {			
			if (aAttributes.getLocalName(i).compareTo("ID") == 0) {
				aElement.mRowId = aAttributes.getValue(i);
			}
			else if (aAttributes.getLocalName(i).compareTo("Exception") == 0) {
				aException = "Exception = " + aAttributes.getValue(i);
				aInfoModel.addRow(new String[] {aException});
			}
			else {
				aElement.mColumnEntry.add(aAttributes.getValue(i));
				aElement.mColumnNames.add(aAttributes.getLocalName(i));
			}
		}

		aElement.mKey 	 = (String)aAttributes.getValue("Type");
		aElement.mDetail = (String)aAttributes.getValue("Detail");
		
		if (aElement.mDetail != null) {
			aElement.mKey = aElement.mKey.concat(aElement.mDetail);
		}

		if (mModelStack.size() > 0) {
			aElement.mModel = mModelStack.get(mModelStack.size()-1);
		}
		else {
			aElement.mModel  = mConnection.getModel(aElement.mKey);
			if (aElement.mModel != null)  {
				aElement.mModel.mKey = aElement.mKey;
			}
		}

		if (aElement.mModel != null)  {
			if (aElement.mModel.mClearRows) {
				aElement.mModel.mClearRows = false;
				aElement.mModel.clearRows();
			}
		}
		
		// Traces, Lists and Growing are tags which starts a list view
		// and have to be organized in a model stack 
		if (aLocalName.equals("Traces") ||
			aLocalName.equals("List")   ||
			aLocalName.equals("Growing")) {
			
			if (aElement.mRowId.compareTo("0x0") != 0) {
				aRowId = aElement.mRowId;
			}
			aModel = aElement.mModel;
			if (aModel != null) {			
				if (mModelStack.size() > 0) {
					// Link the new model to the parent 
					// The RowID is the entry point for the master table
					// Detail views have to be organized below this RowID using a unique name
					if (aElement.mDetail != null) {
						aModelChild		= aModel.getChild(aRowId);
						aModelChild		= aModelChild.getChild(aElement.mKey);
						aElement.mModel = aModelChild;
						aModel          = aModelChild;
					}
					else {
						aModel			= aModel.getChild(aRowId);
					}				
					aModel.mKey = aElement.mKey;				
				}
				
				if (!aModel.mKeepRows) {
					aModel.clearChildren();
					aModel.clearRows();
				}
				
				aElement.mIsBlock = true;
				mModelStack.add(aModel);
			}
		}
		else if (aLocalName.equals("Messages")) {
			aElement.mModel   = null;
			aElement.mIsBlock = true;
			mModelStack.add(aInfoModel);
			if (aInfoModel.mClearRows) {
				aInfoModel.mClearRows = false;
				aInfoModel.clearRows();
			}
		}
		else if (aLocalName.equals("Message")) {
			aElement.mModel = aInfoModel;
			if (aInfoModel.mClearRows) {
				aInfoModel.mClearRows = false;
				aInfoModel.clearRows();
			}
		}
	}

	/**
	 * Callback for the end of an XML element tag
	 * The element data are transferred to the element model and some info is passed
	 * to the status bar. The call to {@link TDoudiaTableModel#fireTableDataChanged()} 
	 * starts the output processing.
	 * The model with key <code>"Info"</code> is used to synchronize input and output
	 * processing.
	 *///------------------------------------------------------------------
	public void endElement(String aUri, String aLocalName, String aName) throws SAXException {
		int 	aInx;
		int     i;
		TDoudiaTableModel 	aModel	 	= null;
		TElement            aElement 	= null;
		TDoudiaTableModel   aInfoModel  = TConnection.getInstance().getModel("Info");
		
		aInx 	 = mElementStack.size() - 1;
		aElement = mElementStack.get(aInx);
		mElementStack.setSize(aInx);
		
		// Check if the element has data for a model
		aModel = aElement.mModel;				
		if (aModel == aInfoModel) {
			synchronized (aInfoModel) {				
				String 			aInfoOutput = new String();
				Vector<String> 	aInfoRow 	= new Vector<String>();
				if (aInfoModel.getRowCount() > 5) {
					aInfoModel.setRowCount(4);
				}
				for (i = 0; i < aElement.mColumnNames.size(); i++) {
					aInfoOutput += aElement.mColumnNames.get(i) + "=" + aElement.mColumnEntry.get(i) + " ";
				}
				aInfoRow.add(aInfoOutput);
				aInfoModel.addRow(aInfoRow, "0x0", 0);
				aInfoModel.setDataVector();
				aInfoModel.notifyAll();
			}
			return;
		}
		
		synchronized (aInfoModel) {	
			aInfoModel.mClearRows = true;
			
			if (aModel != null) {
				if (aModel.getRowCount() == 0) {
					if (aModel.mColumnNames.size() != aElement.mColumnNames.size()) {
						aModel.mElementName = aElement.mTagName;
						aModel.mColumnNames = aElement.mColumnNames;
						aModel.mIsInitial = true;
					}
					else {
						if (aModel.mColumnNames.size() > 1) {  
							i = aModel.mColumnNames.size() - 1;
							if (aModel.mColumnNames.get(i).compareTo(aElement.mColumnNames.get(i)) != 0) {
								aModel.mColumnNames = aElement.mColumnNames;
								aModel.mElementName = aElement.mTagName;
								aModel.mIsInitial = true;
							}
						}
					}
				}
				
			    if (aElement.mTagName == aModel.mElementName &&
				    aElement.mColumnEntry.size() == aModel.mColumnNames.size()) {
				    aModel.addRow(aElement.mColumnEntry, aElement.mRowId, 0);
			    }
			}
		
			if (aElement.mIsBlock) {
    			if (mModelStack.size() > 0) {
					aInx 	= mModelStack.size() - 1;
					aModel 	= mModelStack.get(aInx);
					mModelStack.setSize(aInx);
					if (aModel.mIsInitial && aModel.getRowCount() > 0) {
						aModel.mIsInitial = false;
						aModel.setDataVector();
					}
					aModel.check();
					aModel.fireTableDataChanged();
				}
				aInfoModel.notifyAll();
		    }	
		
			if (aModel != null) {
				if (aModel.mElementName.compareTo("Trace") == 0) {
					aModel.setDataVector();
				}			
			}	
			else if (aInx == 1) {
			    //--aInfoModel.notifyAll();
			}
		}
	}
}

/**
 * Organizes and maintains the connection to Sherlok. 
 * Maintains all root models 
 *///----------------------------------------------------------------------
public class TConnection {

	/** Static singleton instance*/
	static private TConnection mInstance = null;
	
	private Socket 			mSocket;
	private OutputStream 	mOutput;
	private String 			mUser;
	private String 			mPwd;
	private XMLReader 		mParser;	
	private InputSource 	mInput;
	private TDoudiaTableModel mModel;
	private TDoudiaTableModel mInfoModel = null;
	public  Thread          mConnectionThread = null;
	private HashMap<String, TableModel> mHashModel   = new HashMap<String, TableModel>(); 
	public  Vector<TDoudiaTableModel>   mModelStack  = new Vector<TDoudiaTableModel>(); 
	
	
	/**
	 * Creates a connection object
	 */// ------------------------------------------------------------------
	private TConnection() {
		// mCmdStack  = new Vector<String>();
	}

	/**
	 * @return Singleton <code>TConnection</code> instance 
	 */// ------------------------------------------------------------------
	static TConnection getInstance() {
		if (mInstance == null) {
			mInstance = new TConnection();
		}
		return mInstance;
	}

	/**
	 * Processes the login sequence and start the XML parser on Sherlok socket.
	 * @param aHost Remote <em>Sherlok</em> host
	 * @param aPort Remote <em>Sherlok</em> port
	 * @param aUser <em>Sherlok</em> user
	 * @param aPwd  <em>Sherlok</em> password
	 *///------------------------------------------------------------------
	private void doConnect(String aHost, String aPort, String aUser, char[] aPwd)  {
		Integer aIntPort = new Integer(aPort);
		
		try {
			mSocket = new Socket(aHost, aIntPort.intValue());
		} catch (Exception e1) {
			return;
			//--
		}
		if (aPwd.length == 0) {
			mPwd = new String(" ");
		}
		else {
			mPwd = new String(aPwd);
		}
		mUser   = aUser;

		try {
			mOutput = mSocket.getOutputStream();
			mInput  = new InputSource(mSocket.getInputStream());
			mModel  = new TDoudiaTableModel();
			mParser = TDoudiaXmlHandler.getParser(); 
		} catch (Exception e1) {
			try {
				e1.printStackTrace();
				mSocket.close();
			} catch (Exception e2) {
				//-- 
			}
			return;
		}

		mConnectionThread = new Thread(new Runnable() {
			public void run() {
				try {
					mOutput.write(mUser.getBytes());
					mOutput.write("\n".getBytes());
					mOutput.write(mPwd.getBytes());
					mOutput.write("\n".getBytes());					
					mOutput.write("\n".getBytes());				

					mParser.parse(mInput);					
				} catch (Exception e) {					
					System.out.println("Socket closed ");	
					e.printStackTrace();
				} finally {
					try {
						mOutput.close();
						mSocket.close();
					} catch (IOException e) {
					}

					synchronized (mInfoModel) {
						mInfoModel.addRow(new String[] {"Socket closed"});
						mInfoModel.fireTableDataChanged();
						mInfoModel.mConnected = false;
						mInfoModel.notifyAll();
					}						
					TDouala.getInstance().doReset();					
				}
			}
		});
		mConnectionThread.start();
	}

	/**
	 * @return <code>true</code> if connected to a <em>Sherlok</em> server
	 *///------------------------------------------------------------------
	public boolean isConnected() {
		return (mSocket != null && mSocket.isConnected() && !mSocket.isClosed() && mInfoModel.mConnected);
	}

	/**
	 * Process the login 
	 * @param aHost Remote <em>Sherlok</em> host
	 * @param aPort Remote <em>Sherlok</em> port
	 * @param aUser <em>Sherlok</em> user
	 * @param aPwd  <em>Sherlok</em> password
	 *///------------------------------------------------------------------
	public void doLogin(String aHost, String aPort, String aUser, char[] aPwd) {
		
		if (mInfoModel == null) {
			mInfoModel = getModel("Info");
		}
		
		if (isConnected()) {
			return;
		}
		
		// Connect and wait for the parser
		try {
			String  aLogFile = TDouala.getInstance().getLogFile();
			
			if (TDouala.getInstance().getLogFile() != null) {
				TLogFileReader aLogReader = TLogFileReader.getInstance();
				aLogReader.readLogFile(aLogFile);
				return;
			}
			
			synchronized (mInfoModel) {
				doConnect(aHost, aPort, aUser, aPwd);
				mInfoModel.wait(2000);				
			}
		}
		catch (Exception e) {
			return;
		}
		TState.getInstance().doUpdate();
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void setTableListener(JTable aListener, TDoudiaTableModel mEventModel, String aModelName) {
		if (aListener != null) {
			mEventModel.addTableModelListener(aListener);
			aListener.setModel(mEventModel);
		}
		if (aModelName != null) {
			mHashModel.put(aModelName, mEventModel);
		}
	}
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void setModel(TableModel aModel, String aModelName) {
		mHashModel.put(aModelName, aModel);
	}
	// ------------------------------------------------------------------
	// Get the memory
	// ------------------------------------------------------------------
	public TDoudiaTableModel getModel(String aKey) {		
		TDoudiaTableModel aModel = null;		
		if (aKey != null) {
			aModel = (TDoudiaTableModel)mHashModel.get(aKey);
		}
		return aModel;
	}

	/**
	 * Sends a command string to a <em>Sherlok</em> server
	 * @param aCmd <em>Sherlok</em> command
	 *///------------------------------------------------------------------
	public void doCommand(String aCmd) {
		TDoudiaTableModel aInfoModel = TConnection.getInstance().getModel("Info");

		if (!isConnected()) {
			return;			
		}
		
		try {
			synchronized (aInfoModel) {
				mOutput.write(aCmd.concat("\n").getBytes());
				aInfoModel.wait();
			}
		} catch (Exception e) {
			try {
				mSocket.close();
			} catch (IOException e1) {
				return;
			}
		}				
	}

	// ------------------------------------------------------------------
	// Get the XML stream for debug
	// ------------------------------------------------------------------
	public void doReadStream() throws IOException {
		if (!mSocket.isConnected()) {
			throw (new IOException());
		}
		byte[] aStr = new byte[1];
		InputStream aIn = mSocket.getInputStream();

		while (aIn.read(aStr) > 0) {
			System.out.print(new String(aStr));
		}
		synchronized (mModel) {
			mModel.notifyAll();
		}
	}
}
