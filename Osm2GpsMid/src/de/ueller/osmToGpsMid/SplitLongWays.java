/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * 
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;
import java.util.LinkedList;

import de.ueller.osmToGpsMid.area.DebugViewer;
import de.ueller.osmToGpsMid.area.Triangle;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Way;


public class SplitLongWays {
	OsmParser parser;
	LinkedList<Way> added=new LinkedList<Way>();
	LinkedList<Way> deleted=new LinkedList<Way>();
	
	
	public SplitLongWays(OsmParser parser) {
		super();
		this.parser = parser;
		for (Way way : parser.getWays()) {
			testAndSplit(way);
		}
		for (Way w : added) {			
			parser.addWay(w);
		}
		added=null;
	}

	private void testAndSplit(Way way) {
//		if (nonCont && way.getSegmentCount() == 1) return;
// if w way is an Area, it's now also splitable
//		if ( way.isArea()) return;
		Bounds b=way.getBounds();
		if ((b.maxLat-b.minLat) > 0.09f 
				|| (b.maxLon-b.minLon) > 0.09f ){
			Way newWay=way.split();
			if (newWay != null){
				added.add(newWay);
//				if (way.isArea()){
//					DebugViewer v=DebugViewer.getInstanz(new ArrayList<Triangle>(way.triangles));
//					v.alt=new ArrayList<Triangle>(newWay.triangles);
//					v.recalcView();
//					v.repaint();
//				}
				testAndSplit(way);
				testAndSplit(newWay);
			} 
		}
	}
}