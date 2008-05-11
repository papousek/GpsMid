package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.names.Names;

public class C {
	/**
	 * Specifies the format of the map on disk we expect to see
	 * This constant must be in sync with Osm2GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 11;
	
	public final static byte NODE_MASK_ROUTENODELINK=0x1;
	public final static byte NODE_MASK_TYPE=0x2;
	public final static byte NODE_MASK_NAME=0x4;
	public final static byte NODE_MASK_ROUTENODE=0x8;
	public final static byte NODE_MASK_NAMEHIGH=0x10;
	
	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	
	/**
	 * minimum distances to set the is_in name to the next city
	 * to get the minimum distance use: <code>MAX_DIST_CITY[node.getType(null)]</code>
	 */

	public final static byte NAME_CITY=1;
	public final static byte NAME_SUBURB=2;
	public final static byte NAME_STREET=3;
	public final static byte NAME_AMENITY=4;
	
	private POIdescription[] pois;
	private WayDescription[] ways;
	
	private final static Logger logger=Logger.getInstance(C.class,Logger.TRACE);
	
	public C() throws IOException {
		InputStream is = GpsMid.getInstance().getConfig().getMapResource("/legend.dat");
		
		if (is == null) {
			logger.error("Failed to open the legend file");
			return;			
		}

		DataInputStream ds = new DataInputStream(is);
		
		/**
		 * Check to see if we have the right version of the Map format
		 */
		short mapVersion = ds.readShort();
		if (mapVersion != MAP_FORMAT_VERSION) {
			logger.fatal("The Map files are not the version we expected, " +
					"please ues the correct Osm2GpsMid to recreate the map " +
					"data.  Expected: " + MAP_FORMAT_VERSION + " Read: " + mapVersion);
			throw new IOException("Wrong map file format");
		}
		
		readPOIdescriptions(ds);
		readWayDescriptions(ds);
		//readWayDescriptionsOld(ds);
		
		ds.close();				
	}
	
	private void readPOIdescriptions(DataInputStream ds) throws IOException {		
		Image generic = Image.createImage("/unknown.png");
		pois = new POIdescription[ds.readByte()];
		for (int i = 0; i < pois.length; i++) {
			pois[i] = new POIdescription();
			if (ds.readByte() != i)
				logger.error("Read legend had troubles");
			byte flags = ds.readByte();
			pois[i].description = ds.readUTF();
			//System.out.println("POI: " +  pois[i].description);
			pois[i].imageCenteredOnNode = ds.readBoolean();
			pois[i].maxImageScale = ds.readInt();
			if ((flags & LEGEND_FLAG_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//System.out.println("trying to open image " + imageName);
				try {
					pois[i].image = Image.createImage(imageName);
				} catch (IOException e) {
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].image = generic;
				}				
			}
			if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
				String imageName = ds.readUTF();
				System.out.println("trying to open search image " + imageName);
				try {
					pois[i].searchIcon = Image.createImage(imageName);
				} catch (IOException e) {
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].searchIcon = generic;
				}				
			} else if (pois[i].image != null) {
				pois[i].searchIcon = pois[i].image;
			}
			if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0)
				pois[i].maxTextScale = ds.readInt();
			else
				pois[i].maxTextScale = pois[i].maxImageScale; 
			if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0)			
				pois[i].textColor = ds.readInt();
		}
	}
	
	private void readWayDescriptions(DataInputStream ds) throws IOException {		
		Image generic = Image.createImage("/unknown.png");
		ways = new WayDescription[ds.readByte()];		
		for (int i = 0; i < ways.length; i++) {
			ways[i] = new WayDescription();
			if (ds.readByte() != i)
				logger.error("Read legend had troubles");
			byte flags = ds.readByte();
			ways[i].description = ds.readUTF();
			System.out.println("WayDesc: " +  ways[i].description);
			ways[i].maxScale = ds.readInt();
			ways[i].isArea = ds.readBoolean();
			ways[i].lineColor = ds.readInt();
			ways[i].boardedColor = ds.readInt();
			ways[i].wayWidth = ds.readByte();
			System.out.println("   WayWidth: " +  ways[i].wayWidth);
			boolean lineStyle = ds.readBoolean();
			if (lineStyle)
				ways[i].lineStyle = Graphics.DOTTED;
			else
				ways[i].lineStyle = Graphics.SOLID;			
		}
	}	
	
	public int getNodeTextColor(byte type) {
		return pois[typeTotype(type)].textColor;
	}
	
	public Image getNodeImage(byte type)  {
		return pois[typeTotype(type)].image;
	}
	public Image getNodeSearchImage(byte type)  {
		return pois[typeTotype(type)].searchIcon;
	}
	
	public int getNodeMaxScale(byte type) {
		return pois[typeTotype(type)].maxImageScale;
	}
	public int getNodeMaxTextScale(byte type) {
		return pois[typeTotype(type)].maxTextScale;
	}
	
	public boolean isNodeImageCentered(byte type) {
		return pois[typeTotype(type)].imageCenteredOnNode;
	}
	
	public String getNodeTypeDesc(byte type) {
		if (type < 0 || type > pois.length) {
			System.out.println("ERROR: wrong type " + type);
			return null;
		}
		return pois[type].description;
	}
	
	public WayDescription getWayDescription(byte type) {			
		if (type >= ways.length) {
			logger.error("Invalid type request: " + type);
			return null;
 		}
		return ways[type];
	}
	
	public byte getMaxType() {
		return (byte)pois.length;
	}
	
	private byte typeTotype(byte type) {
		return type;		
	}
	
	public byte scaleToTile(int scale) {
		if (scale < 45000) {
			return 3;
		}
		if (scale < 180000) {
			return 2;
		}
		if (scale < 900000) {
			return 1;
		}		
		return 0;
	}
}
