/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gpsmid.tile;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.routing.Connection;
import de.ueller.gpsmid.routing.RouteNode;
import de.ueller.gpsmid.routing.RouteTileRet;
import de.ueller.gpsmid.routing.TurnRestriction;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;


public class RouteContainerTile extends RouteBaseTile {
	//#debug error
	private final static Logger logger = Logger.getInstance(RouteContainerTile.class, Logger.INFO);

	RouteBaseTile t1;
	RouteBaseTile t2;
	
	public RouteContainerTile(DataInputStream dis, int deep, byte zl) throws IOException {
    	//#debug
       	logger.debug("start " + deep);
    	minLat = dis.readFloat();
    	minLon = dis.readFloat();
    	maxLat = dis.readFloat();
    	maxLon = dis.readFloat();
    	minId = dis.readInt();
    	maxId = dis.readInt();
    	//#debug
    	logger.debug("start left " + deep);
    	t1 = (RouteBaseTile) readTile(dis, deep + 1, zl);
    	//#debug
       	logger.debug("start right " + deep);
       	t2 = (RouteBaseTile) readTile(dis, deep + 1, zl);
        //#debug
    	logger.debug("ready " + deep + ":readed ContainerTile");
    }
    
    public Tile readTile(DataInputStream dis, int deep, byte zl) throws IOException {
    	byte t = dis.readByte();
    	switch (t) {
    	case Tile.TYPE_EMPTY:
    		//#debug
    		logger.debug("r ET " + zl + " " + deep);
    		return null;
    	case Tile.TYPE_ROUTEFILE:
    		//#debug
    		logger.debug("r RFT " + zl + " " + deep);
    		return new RouteFileTile(dis, deep, zl);
    	case Tile.TYPE_ROUTEDATA:
    		// RouteData Tile
    		//#debug
    		logger.debug("r RT " + zl + " " + deep);
    		return new RouteTile(dis, deep, zl);
    	case Tile.TYPE_ROUTECONTAINER:
    		// RouteData Tile
    		//#debug
    		logger.debug("r RC " + zl + " " + deep);
    		return new RouteContainerTile(dis, deep, zl);
    	default:
    		//#debug error
		logger.error(Locale.get("routecontainertile.wrongTileType")/*wrongTileType type=*/ + t);
    	throw new IOException("wrong TileType");
    	}
    }

	public void paint(PaintContext pc, byte layer) {
		if (pc == null) {
			return;
		}
		if (contain(pc)) {
			if (t1 != null) {
				t1.paint(pc, layer);
			}
			if (t2 != null) {
				t2.paint(pc, layer);
			}
		}
	}
	
	public boolean cleanup(int level) {
		if (level > 0 && permanent) {
			return false;
		}
		lastUse++;
		if (t1 != null) {
			t1.cleanup(level);
		}
		if (t2 != null) {
			t2.cleanup(level);
		}
		return true;
	}

	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id) {
			RouteNode n = null;
			if (t1 != null) {
				//#debug error
//				logger.debug("search left for " + id);
				n = t1.getRouteNode(id);
			}
			if (n == null && t2 != null) {
				//#debug error
//				logger.debug("search right for " + id);
				n = t2.getRouteNode(id);
			}
			return n;
		} else {
			//#debug error
//			logger.debug("" + id + " not in tile");
		  return null;
		}
	}

	public RouteNode getRouteNode(RouteNode best, float lat, float lon) {
		if (contain(lat, lon, RouteBaseTile.bestRouteNodeSearchEpsilon)) {
			if (t1 != null) {
				best = t1.getRouteNode(best, lat, lon);
			}
			if (t2 != null) {
				best = t2.getRouteNode(best, lat, lon);
			}
		}
		return best;
	}

	public Connection[] getConnections(int id, RouteBaseTile tile, boolean bestTime) {
		if (minId <= id && maxId >= id) {
			lastUse = 0;
			Connection[] n = null;
			if (t1 != null) {
				n = t1.getConnections(id, tile, bestTime);
			}
			if (n == null && t2 != null) {
				n = t2.getConnections(id, tile, bestTime);
			}
			return n;
		} else {
		  return null;
		}
	}

	public RouteNode getRouteNode(float lat, float lon) {
		RouteNode ret = null;
		if (contain(lat, lon, 0.03f)) {
			if (t1 != null) {
				ret = t1.getRouteNode(lat, lon);
			}
			if (ret == null && t2 != null) {
				ret = t2.getRouteNode(lat, lon);
			}
		}
		return ret;
	}
	
	public TurnRestriction getTurnRestrictions(int rnId) {
		if (minId <= rnId && maxId >= rnId) {
			TurnRestriction turn = null;
			if (t1 != null) {
				//#debug error
//				logger.debug("tr: search left for " + id);
				turn = t1.getTurnRestrictions(rnId);
			}
			if (turn == null && t2 != null) {
				//#debug error
//				logger.debug("tr: search right for " + id);
				turn = t2.getTurnRestrictions(rnId);
			}
			return turn;
		} else {
			//#debug error
//			logger.debug("" + id + " not in tile (tr)");
		  return null;
		}
	}
	
	public RouteNode getRouteNode(float lat, float lon, RouteTileRet retTile) {
		RouteNode ret = null;
		// FIXME: do we need an epsilon at all here?
		if (contain(lat, lon, 0.0000005f)) {
			if (t1 != null) {
				ret = t1.getRouteNode(lat, lon, retTile);
			}
			if (ret == null && t2 != null) {
				ret = t2.getRouteNode(lat, lon, retTile);
			}
		}
		if (ret != null) {
			this.permanent = true;
		}
		return ret;
	}
	
}
