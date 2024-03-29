package de.ueller.gpsmid.data;

import de.ueller.gpsmid.mapdata.Entity;
import de.ueller.gpsmid.mapdata.Way;

/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net
 * See Copying
 */


public class RoutePositionMark extends PositionMark {
	/** the entity (e.g. way) of the route position mark */
	public Entity entity;
	/** the travel mode with which this PositionMark's entity has been set as closest way */ 
	public byte entityTravelModeNr = -1;
	/** the latitude coordinates of the way corresponding to this RoutePositionMark
		this is used to find matching routeNodes
	*/
	public float[] nodeLat;
	/** the longitude coordinates of the way corresponding to this RoutePositionMark
		this is used to find matching routeNodes
	*/
	public float[] nodeLon;
	/** the nameIdx of the RoutePositionMark, mainly used to recognize the destination way for highlighting */
	public int nameIdx = -1;

	public RoutePositionMark(float lat, float lon) {
		super(lat, lon);
	}

	public RoutePositionMark(PositionMark pm, int nameIdx) {
		super(pm.lat, pm.lon);
		this.displayName = pm.displayName;
		this.nameIdx = nameIdx;
	}

	
	public void setEntity(Way w, float lat[], float lon[]) {
		entity = w;
		nodeLat = lat;
		nodeLon = lon;
		entityTravelModeNr = (byte) Configuration.getTravelModeNr();
	}


}
