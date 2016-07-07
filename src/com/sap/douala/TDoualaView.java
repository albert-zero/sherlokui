// ----------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   This class is basis for all views in this project. It ensures 
//   implementation of the functions 
//   - "doUpdate"  for framework update events
//   - "doRefresh" for action associated with "Refresh" button
// ----------------------------------------------------------------------
package com.sap.douala;

//---------------------------------------------
//---------------------------------------------
public interface TDoualaView {
	//-----------------------------------------
	// Initial fill routine
	//-----------------------------------------
	public void doUpdate();
	
	//-----------------------------------------
	// Called for button "Refresh"
	//-----------------------------------------
	public void doRefresh();
}

