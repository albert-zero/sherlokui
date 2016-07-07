// ------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
// ------------------------------------------------------------------
package com.sap.douala;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

//------------------------------------------------------------------
//------------------------------------------------------------------
public class TLogFileReader  {

	private static TLogFileReader mInstance = null;
	TDouala 	   		mDouala;
	InputSource    		mXmlSource    		= null;
	Thread              mConnectionThread	= null;
	PipedInputStream    mInput;
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	private TLogFileReader() {
	}
	
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public static TLogFileReader getInstance() {
		if (mInstance == null) {
			mInstance = new TLogFileReader();
		}
		return mInstance;
	}

	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	public void readLogFile(String aLogFilePath) {
		PipedOutputStream 	aOutPipe 	= null;
		PipedInputStream  	aInpPipe 	= null;
		LineNumberReader 	aLineReader = null;
		boolean             aHasInput   = true;
		
		try {
			File 		aXmlLogFile = new File(aLogFilePath);
			FileReader 	aXmlLogRead = new FileReader(aXmlLogFile);
			
			aLineReader = new LineNumberReader(aXmlLogRead);			
			aOutPipe	= new PipedOutputStream();
			aInpPipe 	= new PipedInputStream(aOutPipe);			
			mXmlSource 	= new InputSource(aInpPipe);
			
			// send the start sequence
			aOutPipe.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><sherlok><Message Info=\"Connected\"/>".getBytes());
			
			// run the parser in a separate thread
			mConnectionThread = new Thread() {
				public void run() {
					XMLReader mParser;
					try {
						mParser = TDoudiaXmlHandler.getParser();
						mParser.parse(mXmlSource);					
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			mConnectionThread.start();
			String aLogLine;
			
			// Read the header line
			aHasInput = (aLogLine = aLineReader.readLine()) != null;
			aHasInput = aHasInput && aLogLine.startsWith("===");
			
			while(aHasInput && (aLogLine = aLineReader.readLine()) != null) {				
				for (int aInx = 0; aInx < aLogLine.length(); aInx++) {
					// consume spaces 
					if (aLogLine.charAt(aInx) == ' ') {
						continue;
					}
					// this is the end of a run
					if (aLogLine.charAt(aInx) == '=') {
						aOutPipe.write("</sherlok>".getBytes());
						aLineReader.close();
						aLineReader = null;
						aHasInput   = false;
						break;
					}					
					// each line start with a XML tag
					if (aLogLine.charAt(aInx) != '<') {
						break;
					}					
					// ignore XML header
					if (aLogLine.substring(aInx).startsWith("<?")) {
						continue;
					}
					// ignore start of stream					
					if (aLogLine.substring(aInx).startsWith("<sherlok")) {
						continue;
					}
					// ignore end of stream
					if (aLogLine.substring(aInx).startsWith("</sherlok")) {
						continue;
					}
					aOutPipe.write(aLogLine.substring(aInx).getBytes());
					break;
				}
			}
		} 
		catch (Exception e) {
			//--
		}
		finally {
			// send the end sequence			
			try {
				if (aLineReader != null) {
					aOutPipe.write("</sherlok>".getBytes());
				}
			} catch (IOException e) {
				// --
			}		
		}
	}
}
