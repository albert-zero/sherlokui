// ----------------------------------------------------------------------
// Author     : Albert Zedlitz
// Description:
//   This class is basis for all views in this project. It ensures 
//   implementation of the functions 
//   - "doUpdate"  for framework update events
//   - "doRefresh" for action associated with "Refresh" button

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

