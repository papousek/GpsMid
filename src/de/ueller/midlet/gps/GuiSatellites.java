package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * Drawing code moved from Trace.java, 
 * (c) Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.tile.C;

public class GuiSatellites extends KeyCommandCanvas implements CommandListener,
		GpsMidDisplayable, LocationMsgReceiver  {

	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	
	private final static Logger mLogger = Logger.getInstance(GuiSatellites.class,
			Logger.DEBUG);
	
	private final Trace mParent;
	
	private final LocationMsgProducer mLocationProducer;
	
	private String mSolution;

	private Satelit[] mSatellites;

	private Image mSatImage;


	public GuiSatellites(Trace parent, LocationMsgProducer locprod) {
		// #debug
		mLogger.info("Init GuiSatellites");

		mParent = parent;
		mLocationProducer = locprod;
		
		addCommand(BACK_CMD);
		setCommandListener(this);
		// TODO: Get the key for this from the configuration.
		singleKeyPressCommand.put(KEY_NUM7, BACK_CMD);
		
		try {
			mSatImage = Image.createImage("/satelit.png");
		} catch (IOException ioe) {
			mLogger.exception("Failed to load satellite icon", ioe);
		}

	}

	protected void paint(Graphics g) {
		//#debug debug
		mLogger.debug("Drawing Satellites screen");
		
		int h = getHeight();
		int w = getWidth();
		
		// Clear the screen to the map background colour.
		g.setColor(C.BACKGROUND_COLOR);
		g.fillRect(0, 0, w, h);

		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		int dia = Math.min(getWidth(), getHeight()) - 6;
		int r = dia / 2;
		g.setColor(255, 50, 50);
		g.drawArc(centerX - r, centerY - r, dia, dia, 0, 360);
		if (mSatellites == null) {
			g.setColor(0, 0, 0);
			g.drawString("Satellites n/a", centerX, centerY - g.getFont().getHeight() / 2, Graphics.TOP|Graphics.HCENTER);
			return;
		}
		for (byte i = 0; i < mSatellites.length; i++) {
			Satelit s = mSatellites[i];
			if (s == null) {
				continue; //This array may be sparsely filled.
			}
			if (s.id != 0) {
				double el = s.elev / 180d * Math.PI;
				double az = s.azimut / 180 * Math.PI;
				double sr = r * Math.cos(el);
				if (s.isLocked()) {
					g.setColor(0, 255, 0);
				} else {
					g.setColor(255, 0, 0);
				}
				int px = centerX + (int) (Math.sin(az) * sr);
				int py = centerY - (int) (Math.cos(az) * sr);
				// g.drawString(""+s.id, px, py,
				// Graphics.BASELINE|Graphics.HCENTER);
				g.drawImage(mSatImage, px, py, Graphics.HCENTER
						| Graphics.VCENTER);
				py += 9;
				// draw a bar under image that indicates green/red status and
				// signal strength
				g.fillRect(px - 9, py, (int)(s.snr * 18.0 / 100.0), 2);
			}
		}
		g.setColor(0, 0, 0);
		if (mSolution != null) {
			g.drawString(mSolution, w - 1, 1, Graphics.TOP | Graphics.RIGHT);
		}
	}

	public void show() {
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		GpsMid.getInstance().show(this);
		if (mLocationProducer != null) {
			mLocationProducer.addLocationMsgReceiver(this);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			if (mLocationProducer != null) {
				mLocationProducer.removeLocationMsgReceiver(this);
			}
			mParent.show();
		}
	}

	public void receivePosition(Position pos) {
		// Not interested
	}

	public void receiveSatellites(Satelit[] sats) {
		mSatellites = sats;
		repaint();
	}

	public void receiveMessage(String s) {
		// Not interested
	}
	
	public void receiveStatistics(int[] statRecord, byte quality) {
		// Not interested
	}

	public void receiveSolution(String solution) {
		mSolution = solution;
		repaint();
	}

	public void locationDecoderEnd() {
		// Not interested, handled by Trace.
	}
	
	public void locationDecoderEnd(String msg) {
		// Not interested, handled by Trace.
	}
}
