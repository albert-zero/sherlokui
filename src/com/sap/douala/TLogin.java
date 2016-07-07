// ------------------------------------------------------------------
// Author: Albert Zedlitz
// Description:
//   TLogin organizes the login page 

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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.net.URL;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

@SuppressWarnings("serial")
//------------------------------------------------------------------
//------------------------------------------------------------------
public class TLogin extends JPanel implements TDoualaView {
	Image mImage;
	Image mBgImage;
	
	final JTextField 		aTxtHost   	= new JTextField();		
	final JTextField 		aTxtPort  	= new JTextField();
	final JTextField 		aTxtUser  	= new JTextField();
	final JPasswordField 	aTxtPwd		= new JPasswordField();
	static private TLogin   mLogin      = null;
	
	static public TLogin getInstance() {
		if (mLogin == null) {
			mLogin = new TLogin();			
		}
		return mLogin;
	}
	// ------------------------------------------------------------------
	// ------------------------------------------------------------------
	private TLogin() {
		super(new BorderLayout());
		
		GridBagConstraints aConstr 	= new GridBagConstraints();
		GridBagLayout aLayout   	= new GridBagLayout();

		// Create the input panel 
		JPanel     aNicPanel 		= new JPanel();
		JPanel     aSubPanel 		= new JPanel(aLayout);
		
		JLabel     aLblHost  		= new JLabel("Host");
		JLabel     aLblPort  		= new JLabel("Port");		
		JLabel     aLblUser  		= new JLabel("User");		
		JLabel     aLblPwd   		= new JLabel("Password");		

		URL aUrl = ClassLoader.getSystemResource("icons/sherlok.jpg");
		//URL aUrl = this.getClass().getResource("icons/sherlok.jpg");
		//System.out.println(aUrl.toString());
		if (aUrl != null) {
			mImage = Toolkit.getDefaultToolkit().getImage(aUrl);
			mImage = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(mImage.getSource(), new Background(100)));
		}
		setOpaque(false);
		aNicPanel.setOpaque(false);
		aNicPanel.add(aSubPanel);
		add(Box.createVerticalStrut(50), BorderLayout.NORTH);
		add(aNicPanel, BorderLayout.CENTER);
		//add(aSubPanel);
		
		aTxtPort.setPreferredSize(new Dimension(150, 15));
		aTxtHost.setPreferredSize(new Dimension(150, 15));
		
		aSubPanel.setOpaque(false);
		aSubPanel.add(aLblHost);
		aSubPanel.add(aTxtHost);
		aSubPanel.add(aLblPort);
		aSubPanel.add(aTxtPort);
		aSubPanel.add(aLblUser);
		aSubPanel.add(aTxtUser);
		aSubPanel.add(aLblPwd);
		aSubPanel.add(aTxtPwd);
		
		aConstr.ipadx     = 10;
		aConstr.ipady     =  4;
		aConstr.fill      = GridBagConstraints.HORIZONTAL;
		aConstr.weightx   = 1.0;
		aConstr.gridwidth = 1;
		aLayout.setConstraints(aLblHost, aConstr);
		
		aConstr.gridwidth = GridBagConstraints.REMAINDER;
		aLayout.setConstraints(aTxtHost, aConstr);
		aTxtHost.setText("localhost");
		
		aConstr.gridwidth = 1;
		aLayout.setConstraints(aLblPort, aConstr);

		aConstr.gridwidth = GridBagConstraints.REMAINDER;
		aLayout.setConstraints(aTxtPort, aConstr);
		aTxtPort.setText("2222");

		aConstr.gridwidth = 1;
		aLayout.setConstraints(aLblUser, aConstr);

		aConstr.gridwidth = GridBagConstraints.REMAINDER;
		aLayout.setConstraints(aTxtUser, aConstr);
		aTxtUser.setText("Administrator");

		aConstr.gridwidth = 1;
		aLayout.setConstraints(aLblPwd, aConstr);

		aConstr.gridwidth = GridBagConstraints.REMAINDER;
		aLayout.setConstraints(aTxtPwd, aConstr);
		
		// Key listener to implement ENTER on this page
		aTxtPwd.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					doLogin();
				}				
			}

			public void keyReleased(KeyEvent e) {				
			}

			public void keyTyped(KeyEvent e) {				
			}
			
		});
	}	

	//------------------------------------------------------------------
	// Login with the data on this page and request sherlok state
	//------------------------------------------------------------------
	public void doLogin() {		
		TConnection.getInstance().doLogin(
					aTxtHost.getText(), 
					aTxtPort.getText(), 
					aTxtUser.getText(),
					aTxtPwd.getPassword());
		TState.getInstance().doUpdate();
	}
	//------------------------------------------------------------------
	// Send exit command. This will return the ending XML tag and close
	// the socket.
	//------------------------------------------------------------------
	public void doLogout() throws Exception {
		TConnection.getInstance().doCommand("exit");
	}
	
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void paint(Graphics g) {	
		g.drawImage(mImage, 0, 0, this);
		// super.paint(g);
		printChildren(g);
	}
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doReset() {
		TState.getInstance().doReset();
		TDouala.getInstance().doReset();
		
	}
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doRefresh() {
		doUpdate();
	}
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	public void doUpdate() {	
		try {
			TConnection.getInstance().doCommand("lss");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}

//------------------------------------------------------------------
//------------------------------------------------------------------
class Background extends RGBImageFilter {
	private int mAlpha;
	
	public Background(int aAlpha) {
		mAlpha = aAlpha;
	}

	@Override 
	public int filterRGB(int x, int y, int rgb) {		
		DirectColorModel aColorModel = (DirectColorModel)ColorModel.getRGBdefault();
		int alpha = aColorModel.getAlpha(rgb);
		int red   = aColorModel.getRed  (rgb);
		int green = aColorModel.getGreen(rgb);
		int blue  = aColorModel.getBlue (rgb);
		
		if (alpha != 0) {
			alpha = mAlpha; 
		}
		return alpha << 24 | red << 16 | green << 8 | blue;
	}
}
