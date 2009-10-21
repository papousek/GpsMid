/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.model.name.Names;

public class Way extends Entity implements Comparable<Way> {
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_LAYER = 4;
	public static final byte WAY_FLAG_RESERVED_FLAG = 8;
	public static final byte WAY_FLAG_ONEWAY = 16;	
	public static final byte WAY_FLAG_NAMEHIGH = 32;
	public static final byte WAY_FLAG_AREA = 64;
	public static final int WAY_FLAG_ADDITIONALFLAG = 128;

	public static final byte WAY_FLAG2_ROUNDABOUT = 1;
	public static final byte WAY_FLAG2_TUNNEL = 2;
	public static final byte WAY_FLAG2_BRIDGE = 4;
	public static final byte WAY_FLAG2_CYCLE_OPPOSITE = 8;
	/** TODO: Is this really in use??? */
	public static final byte WAY_FLAG2_LONGWAY = 16;
	public static final byte WAY_FLAG2_MAXSPEED_WINTER = 32;

	public Path path = null;

	Bounds bound = null;
	
	/** Travel modes for which this way can be used (motorcar, bicycle, etc.) */
	public byte wayTravelModes = 0;
	
	public static Configuration config;
	/**
	 * Indicates that this way was already written to output;
	 */
	public boolean used = false;
	private byte type = -1;

	public Way(long id) {
		this.id = id;
	}
	
	/**
	 * create a new Way which shares the tags with the other way, has
	 * the same type and id, but no Nodes
	 * @param other
	 */
	public Way(Way other) {
		super(other);		
		this.type = other.type;
	}
	
	public void cloneTags(Way other) {
		super.cloneTags(other);		
		this.type = other.type;
	}

	public boolean isHighway() {
		return (getAttribute("highway") != null);
	}

	public void determineWayRouteModes() {
		if (config == null) {
			config = Configuration.getConfiguration();
		}
		
		// check if wayDesc is null otherwise we could route along a way we have no description how to render, etc.
		WayDescription wayDesc = config.getWayDesc(type);
		if (wayDesc == null) {
			return;
		}
		
		// for each way the default route accessibility comes from its way description
		wayTravelModes = wayDesc.wayDescTravelModes;
		
		// modify the way's route accessibility according to the route access restriction for each routeMode
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			switch (isAccessPermittedOrForbiddenFor(i)) {
				case 1:
					wayTravelModes |= 1<<i;
					break;
				case -1:
					wayTravelModes &= ~(1<<i);
					break;
			}	
		}
		
		// Mark mainstreet net connections
		if (isAccessForAnyRouting()) {
			String wayValue = ";" + wayDesc.value.toLowerCase() + ";"; 
			if (";motorway;motorway_link;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_MOTORWAY;
				TravelModes.numMotorwayConnections++;
			}
			if (";trunk;trunk_link;primary;primary_link;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_TRUNK_OR_PRIMARY;
				TravelModes.numTrunkOrPrimaryConnections++;
			}
			if (";motorway;motorway_link;trunk;trunk_link;primary;primary_link;secondary;secondary_link;tertiary;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_MAINSTREET_NET;
				TravelModes.numMainStreetNetConnections++;
			}
		}
	}

	public boolean isAccessForRouting(int travelModeNr) {
		return ((wayTravelModes & (1 << travelModeNr)) != 0); 
	}

	public boolean isAccessForAnyRouting() {
		return (wayTravelModes != 0);
	}

	/**
	 * Check way tags for routeAccessRestriction from style-file
	 * @param travelModeNr: e.g. for motorcar or bicycle
	 * @return -1 if restricted, 1 if permitted, 0 if neither
	 */
	public int isAccessPermittedOrForbiddenFor(int travelModeNr) {
		String value;
		for (RouteAccessRestriction rAccess: TravelModes.getTravelMode(travelModeNr).getRouteAccessRestrictions()) {		
			value = getAttribute(rAccess.key);
			if (value != null && rAccess.values.indexOf(value) != -1) {
				if (rAccess.permitted) {
					return 1;
				} else {
					return -1;
				}
			}			
		}
		return 0;
	}
	
	public boolean isRoundabout() {
		String jType = getAttribute("junction");
		if (jType != null) {
			return (jType.equalsIgnoreCase("roundabout"));
		} else {
			return false;
		}
	}

	public boolean isTunnel() {
		return (containsKey("tunnel"));
	}

	public boolean isBridge() {
		return (containsKey("bridge"));
	}

    public byte getType(Configuration c) {
    	if (type == -1) {
			type = calcType(c);
		}
		return type;
	}

    public byte getType() {
    	return type;
	}   

	private byte calcType(Configuration c) {
		WayDescription way = (WayDescription)super.calcType(c.getWayLegend());
		
		if (way == null) {
			type = -1;
		} else {
			type = way.typeNum;
			way.noWaysOfType++;
		}
		
		/** Check to see if the way corresponds to any of the POI types
		 *  If it does, then we insert a POI node to reflect this, as otherwise
		 *  the nearest POI search or other POI features don't work on ways
		 *  
		 */
		POIdescription poi = (POIdescription)super.calcType(c.getPOIlegend());
		if (poi != null && poi.createPOIsForAreas) {
			if (isValid()) {
				/**
				 * TODO: Come up with a sane solution to find out where to place
				 * the node to represent the area POI
				 */
				Node n = path.getSubPaths().getFirst().get(0);
				n.wayToPOItransfer(this, poi);
			}
		}
		return type;
	}
	
	public String getName() {
		if (type != -1) {
			WayDescription desc = Configuration.getConfiguration().getWayDesc(type);
			if (desc != null) {
				String name = getAttribute(desc.nameKey);
				String nameFallback = null;
				if (desc.nameFallbackKey != null && desc.nameFallbackKey.equals("*") ) {
					nameFallback = getAttribute(desc.key);
				} else {
					nameFallback = getAttribute(desc.nameFallbackKey);
				}
				if (name != null && nameFallback != null) {
					name += " (" + nameFallback + ")";
				} else if ((name == null) && (nameFallback != null)) {
					name = nameFallback;
				}
				//System.out.println("New style name: " + name);
				return (name != null ? name.trim() : "");
			}
		}
		return null;
	}

	public byte getZoomlevel(Configuration c) {
		byte type = getType();
		
		if (type == -1) {
			//System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getWayDesc(type).minEntityScale;
		if (maxScale < 45000) {
			return 3;
		}
		if (maxScale < 180000) {
			return 2;
		}
		if (maxScale < 900000) {
			return 1;
		}
		return 0;		
	}
	
	/**
     * Returns the maximum speed in km/h if explicitly set for this way,
     * if not, it returns -1.0
     * @return
     */
	public float getMaxSpeed() {
		float maxSpeed = -1.0f;
		if (containsKey("maxspeed")) {
			String maxSpeedAttr = getAttribute("maxspeed");
			try {
				boolean mph = false;
				
				if (maxSpeedAttr.equalsIgnoreCase("variable") ||
						maxSpeedAttr.equalsIgnoreCase("default") ||
						maxSpeedAttr.equalsIgnoreCase("signals") ||
						maxSpeedAttr.equalsIgnoreCase("none") ||
						maxSpeedAttr.equalsIgnoreCase("no")) {
					/**
					 * We can't really do anything sensible with these,
					 * so ignore them
					 */
					return maxSpeed;
				}
				if (maxSpeedAttr.toLowerCase().endsWith("mph")) {
					mph = true;
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("km/h")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 4).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kmh")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kph")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} 
				maxSpeed = (Float.parseFloat(maxSpeedAttr));
				if (mph) {
					maxSpeed *= 1.609; //Convert to km/h
				}
			} catch (NumberFormatException e) {
				int maxs = config.getMaxspeedTemplate(maxSpeedAttr);
				if (maxs < 0) {
					System.out.println("Unhandled maxspeed for way " + toString());
				} else {
					maxSpeed = maxs;
				}
			}
		}
		return maxSpeed;
	}

	/**
     * Returns the winter maximum speed in km/h if explicitly set for this way,
     * if not, it returns -1.0
     * @return
     */
	public float getMaxSpeedWinter() {
		float maxSpeed = -1.0f;
		if (containsKey("maxspeed:seasonal:winter")) {
			String maxSpeedAttr = getAttribute("maxspeed:seasonal:winter");
			try {
				boolean mph = false;
				
				if (maxSpeedAttr.equalsIgnoreCase("variable") ||
						maxSpeedAttr.equalsIgnoreCase("default") ||
						maxSpeedAttr.equalsIgnoreCase("signals") ||
						maxSpeedAttr.equalsIgnoreCase("none") ||
						maxSpeedAttr.equalsIgnoreCase("no")) {
					/**
					 * We can't really do anything sensible with these,
					 * so ignore them
					 */
					return maxSpeed;
				}
				if (maxSpeedAttr.toLowerCase().endsWith("mph")) {
					mph = true;
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("km/h")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 4).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kmh")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kph")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} 
				maxSpeed = (Float.parseFloat(maxSpeedAttr));
				if (mph) {
					maxSpeed *= 1.609; //Convert to km/h
				}
			} catch (NumberFormatException e) {
				int maxs = config.getMaxspeedTemplate(maxSpeedAttr);
				if (maxs < 0) {
					System.out.println("Unhandled maxspeedwinter for way + " + toString() +": " + getAttribute("maxspeed"));
				} else {
					maxSpeed = maxs;
				}
			}
		}
		return maxSpeed;
	}

    /**
     * Get or estimate speed in m/s for routing purposes.
     * @param routeModeNr Route mode to use
     * @return routing speed
     */
	public float getRoutingSpeed(int routeModeNr) {
		if (config == null) {
			config = Configuration.getConfiguration();
		}
		float maxSpeed = getMaxSpeed();
		float typicalSpeed = config.getWayDesc(type).typicalSpeed[routeModeNr];
		if (maxSpeed <= 0) {
			maxSpeed = 60.0f; //Default case;
		}
		if (typicalSpeed != 0) {
			if (typicalSpeed < maxSpeed) {
				maxSpeed = typicalSpeed;
			}
		}
		return maxSpeed / 3.6f;
	}

	public int compareTo(Way o) {
		byte t1 = getType();
		byte t2 = o.getType();
		if (t1 < t2) {
			return 1;
		} else if (t1 > t2) {
			return -1;
		}
		return 0;
	}

	public Bounds getBounds() {
		if (bound == null) {
			bound = new Bounds();
			path.extendBounds(bound);
		}
		return bound;
	}

	public void clearBounds() {
		bound = null;
	}

	public String toString() {
		String res = "id=" + id + 
			((nearBy == null) ? "" : (" near " + nearBy)) + 
			" type=" + getType() + " [";
		Set<String> tags = getTags();
		if (tags != null) {
			for (String key : tags) {
				res = res + key + "=" + getAttribute(key) + " "; 
			}			 
		}
		res = res + "]";
		return res;
	}

	/**
	 * @return
	 */
	public String getIsIn() {
		return getAttribute("is_in");
	}

	/**
	 * @return
	 */
	public byte getNameType() {
		String t = getAttribute("highway");
		if (t != null) {
			return (Constants.NAME_STREET);
		}
		return Constants.NAME_AMENITY;
	}

	/**
	 * @return
	 */
	public Node getMidPoint() {
		List<Node> nl = path.getSubPaths().getFirst().getNodes();
		int splitp = nl.size() / 2;
		return (nl.get(splitp));
	}

	/**
	 * @return
	 */
	public boolean isOneWay() {		
		return (Configuration.attrToBoolean(getAttribute("oneway")) > 0);
	}
	
	/** Check if cycleway=opposite or cycleway=opposite_track or cycleway=opposite_lane is set */
	public boolean isOppositeDirectionForBicycleAllowed() {
		String s = getAttribute("cycleway");
		if ( s == null ) {
			return false;
		}
		return ("|opposite|opposite_track|opposite_lane|".indexOf("|" + s.toLowerCase() + "|") >= 0);
	}
	
	public boolean isExplicitArea() {
		return Configuration.attrToBoolean(getAttribute("area")) > 0;
	}

	public void write(DataOutputStream ds,Names names1,Tile t) throws IOException {		
		Bounds b = new Bounds();
		int flags = 0;
		int flags2 = 0;
		int maxspeed = 50;
		int maxspeedwinter = 50;
		int nameIdx = -1;
		int isinIdx = -1;
		byte layer = 0;
		
		if (config == null) {
			config = Configuration.getConfiguration();
		}

		byte type = getType();
		
		if (getName() != null && getName().trim().length() > 0) {			
			flags += WAY_FLAG_NAME;
			nameIdx = names1.getNameIdx(getName());
			if (nameIdx >= Short.MAX_VALUE) {
				flags += WAY_FLAG_NAMEHIGH;
			}
		}
		maxspeed = (int)getMaxSpeed();
		maxspeedwinter = (int)getMaxSpeedWinter();
		if (maxspeedwinter > 0) {
			flags2 += WAY_FLAG2_MAXSPEED_WINTER;
		} else {
			maxspeedwinter = 0;
		}
		if (maxspeed > 0) {
			flags += WAY_FLAG_MAXSPEED;
		}
		
		if (containsKey("layer")) {
			try {
				layer = (byte)Integer.parseInt(getAttribute("layer"));
				flags += WAY_FLAG_LAYER;
			} catch (NumberFormatException e) {
			}
		}
		if ((config.getWayDesc(type).forceToLayer != 0)) {
			layer = config.getWayDesc(type).forceToLayer;
			flags |= WAY_FLAG_LAYER;
		}


		boolean isWay = false;
		boolean longWays = false;
		
		if (type < 1) {
			System.out.println("ERROR! Invalid way type for way " + toString());
		}
		
		for (SubPath s:path.getSubPaths()) {
			if (s.size() >= 255) {
				longWays = true;
			}
			if (s.size() > 1) {
				isWay = true;
			}
		}
		if (isWay) {
			if (path.isMultiPath()) {
//				flags += WAY_FLAG_MULTIPATH;
				System.err.println("MULTIPATH");
			}
			if (isOneWay()) {
				flags += WAY_FLAG_ONEWAY;
			}
			if (isExplicitArea()) {				
				flags += WAY_FLAG_AREA;				
			}
			if (isRoundabout()) {
				flags2 += WAY_FLAG2_ROUNDABOUT;
			}
			if (isTunnel()) {
				flags2 += WAY_FLAG2_TUNNEL;
			}
			if (isBridge()) {
				flags2 += WAY_FLAG2_BRIDGE;
			}
			if (isOppositeDirectionForBicycleAllowed()) {
				flags2 += WAY_FLAG2_CYCLE_OPPOSITE;				
			}
			if (longWays ) {
				flags2 += WAY_FLAG2_LONGWAY;
			}
			if (flags2 != 0) {
				flags += WAY_FLAG_ADDITIONALFLAG; 
			}
			ds.writeByte(flags);

			b = getBounds();
			ds.writeShort((short)(MyMath.degToRad(b.minLat - t.centerLat) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.minLon - t.centerLon) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.maxLat - t.centerLat) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.maxLon - t.centerLon) * Tile.fpm));
			
//			ds.writeByte(0x58);
			ds.writeByte(type);
			
			ds.writeByte(wayTravelModes);
			
			if ((flags & WAY_FLAG_NAME) == WAY_FLAG_NAME) {
				if ((flags & WAY_FLAG_NAMEHIGH) == WAY_FLAG_NAMEHIGH) {
					ds.writeInt(nameIdx);
				} else {
					ds.writeShort(nameIdx);
				}
			}
			if ((flags & WAY_FLAG_MAXSPEED) == WAY_FLAG_MAXSPEED) {
				ds.writeByte(maxspeed);
			}
			// must be below maxspeed as this is a combined flag and FpsMid relies it's below maxspeed
			if (flags2 != 0) {
				ds.writeByte(flags2);				
			}
			if ((flags2 & WAY_FLAG2_MAXSPEED_WINTER) == WAY_FLAG2_MAXSPEED_WINTER) {
				ds.writeByte(maxspeedwinter);
			}
			if ((flags & WAY_FLAG_LAYER) == WAY_FLAG_LAYER) {
				ds.writeByte(layer);
			}
			
			for (SubPath s:path.getSubPaths()) {
				if (longWays) {
					ds.writeShort(s.size());
				} else {
					ds.writeByte(s.size());
				}
				for (Node n : s.getNodes()) {
					ds.writeShort(n.renumberdId);
				}
// only for test integrity
//				System.out.println("   write magic code 0x59");
//				ds.writeByte(0x59);
			}
			if (config.enableEditingSupport) {
				if (id > Integer.MAX_VALUE) {
					System.out.println("WARNING: OSM-ID won't fit in 32 bits for way " + this);
					ds.writeInt(-1);
				} else
					ds.writeInt((int)id);
			}
		} else {
			ds.write(128); // flag that mark there is no way
		}		
	}

	public void add(Node n) {
		if (path == null) {
			path = new Path();
		}
		path.add(n);
	}
	
	public boolean containsNode(Node nSearch) {
		for (SubPath s:path.getSubPaths()) {
			for (Node n:s.getNodes()) {
				if (nSearch.id == n.id) {
					return true;
				}
			}
		}
		return false;
	}
	
	public ArrayList<RouteNode> getAllRouteNodesOnTheWay() {
		ArrayList<RouteNode> returnNodes = new ArrayList<RouteNode>();
		for (SubPath s:path.getSubPaths()) {
			for (Node n:s.getNodes()) {
				if (n.routeNode != null) {
					returnNodes.add(n.routeNode);
				}
			}
		}
		return returnNodes;
	}
	
	public void startNextSegment() {
		if (path == null) {
			path = new Path();
		}
		path.addNewSegment();
	}
	

	/**
	 * Replaces node1 with node2 in this way. 
	 * @param node1 Node to be replaced
	 * @param node2 Node by which to replace node1.
	 */
	public void replace(Node node1, Node node2) {
		path.replace(node1, node2);
	}

	/** replaceNodes lists nodes and by which nodes they have to be replaced.
	 * This method applies this list to this way.
	 * @param replaceNodes Hashmap of pairs of nodes
	 */
	public void replace(HashMap<Node, Node> replaceNodes) {
		path.replace(replaceNodes);
	}

	public List<SubPath> getSubPaths() {
		return path.getSubPaths();
	}
	
	public int getLineCount() {
		return path.getLineCount();
	}
	
	public Way split() {		
		if (isValid() == false) {
			System.out.println("Way before split is not valid");
		}
		Path split = path.split();
		if (split != null) {
			//If we split the way, the bounds are no longer valid
			this.clearBounds();
			Way newWay = new Way(this);			
			newWay.path = split;
			if (newWay.isValid() == false) {
				System.out.println("New Way after split is not valid");
			}
			if (isValid() == false) {
				System.out.println("Old Way after split is not valid");
			}
			return newWay;
		}
		return null;
	}

	public boolean isValid() {
		if (path == null) {
			return false;
		}
		path.clean();
		if (path.getPathCount() == 0) {
			return false;
		}
		return true;
	}
}
