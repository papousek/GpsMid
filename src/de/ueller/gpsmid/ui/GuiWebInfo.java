package de.ueller.gpsmid.ui;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiWebInfo extends List implements GpsMidDisplayable,
		CommandListener {

	private final static Logger mLogger = Logger.getInstance(GuiWebInfo.class,
			Logger.DEBUG);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);
	private final Command SELECT_CMD = new Command(Locale.get("guiwebinfo.Select")/*Select*/, Command.OK, 2);
	private GpsMidDisplayable mParent;
	private Position mPos;
	private String mPoiUrl;
	private String mPoiPhone;
	private int mNodeID;
	private Way actualWay;
	private Trace trace;

//#if polish.api.finland
	// FIXME handle the API key some other way
	private final static String reittiopasAuth = "";
	private final static String reittiopasCoordSpec = "&epsg_in=wgs84&epsg_out=wgs84";
	private final static String reittiopasUrl = "http://api.reittiopas.fi/hsl/prod/?" + reittiopasAuth + reittiopasCoordSpec;
//#endif

	// if longtap is true, instantiate as context menu which also has nearby POI search
	public GuiWebInfo(GpsMidDisplayable parent, Position pos, PaintContext pc, boolean longtap, String poiUrl, String poiPhone,
		int nodeID) {
		super(Locale.get("guiwebinfo.ContactWebOrPhone")/*Contact by web or phone*/, List.IMPLICIT);
		actualWay = pc.trace.actualWay;
		trace = pc.trace;
		mPoiUrl = poiUrl;
		mPoiPhone = poiPhone;
		mParent = parent;
		mPos = pos;
		mNodeID = nodeID;
		if (longtap) {
			this.append(Locale.get("guisearch.nearestpois")/*Nearest POIs*/, null);
			this.append(Locale.get("guiwaypoint.AsDestination")/*As destination*/, null);
			this.append(Locale.get("trace.CalculateRoute")/*Calculate route*/, null);
		}
		//#if polish.api.online
		//this.append("Wikipedia (Web)", null);
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WIKIPEDIA_RSS)) {
			this.append(Locale.get("guiwebinfo.WikipediaRSS")/*Wikipedia (RSS)*/, null);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEATHER)) {
			this.append(Locale.get("guiwebinfo.Weather")/*Weather*/, null);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_GEOHACK)) {
			this.append(Locale.get("guiwebinfo.GeoHack")/*GeoHack*/, null);
		}
//#if polish.api.finland
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_TOPOMAP)) {
			this.append(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map (Finland)*/, null);
		}
		this.append(Locale.get("guiwebinfo.ReittiopasAddress"), null);
		this.append(Locale.get("guiwebinfo.ReittiopasStop"), null);
//#endif
		//#endif
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
			//System.out.println("actualWay: " + actualWay + " urlIdx: " + actualWay.urlIdx + " url: " + trace.getUrl(actualWay.urlIdx));
			String url;
			if (mPoiUrl != null || ((actualWay != null) && ((url = trace.getUrl(actualWay.urlIdx)) != null))) {
				this.append(Locale.get("guiwebinfo.Website")/*Website*/, null);
			}
		}
		if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
			//System.out.println("actualWay: " + actualWay + " phoneIdx: " + actualWay.phoneIdx + " phone: " + trace.getUrl(actualWay.phoneIdx));
			String phone;
			if (mPoiPhone != null || ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null))) {
				this.append(Locale.get("guiwebinfo.Phone")/*Phone*/, null);
			}
		}
		//#if polish.api.bigsearch
		//#if polish.api.osm-editing
		if (mNodeID != -1 && Legend.enableEdits) {
			this.append(Locale.get("guiwebinfo.EditPOI")/*Edit POI*/, null);
		}		
		//#endif 
		//#endif 
		// FIXME add "search for name on the web" for POI names once the code to select POIS is in place
		this.addCommand(BACK_CMD);
		this.setCommandListener(this);
		this.setSelectCommand(SELECT_CMD);
	}

	public void show() {
		GpsMid.getInstance().show(this);

	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			mParent.show();
		}
		if (c == SELECT_CMD) {
			String site = getString(getSelectedIndex());
			if (site.equals(Locale.get("guisearch.nearestpois")/*Nearest POIs*/)) {
				try {
					GuiSearch guiSearch = new GuiSearch(trace, GuiSearch.ACTION_NEARBY_POI);
					guiSearch.show();
				} catch (Exception e) {
					mLogger.exception("Could not open GuiSearch for nearby POI search", e);
				}
			} else if (site.equals(Locale.get("guiwaypoint.AsDestination")/*As destination*/)) {
				RoutePositionMark pm1 = new RoutePositionMark(mPos.latitude, mPos.longitude);
				trace.setDestination(pm1);
				mParent.show();
			} else if (site.equals(Locale.get("trace.CalculateRoute")/*Calculate route*/)) {
				RoutePositionMark pm1 = new RoutePositionMark(mPos.latitude, mPos.longitude);
				trace.setDestination(pm1);
				trace.commandAction(Trace.ROUTING_START_CMD);
				mParent.show();
			//#if polish.api.bigsearch
			//#if polish.api.osm-editing
			} else if (site.equalsIgnoreCase(Locale.get("guiwebinfo.EditPOI")/*Edit POI*/)) {
					//System.out.println("Calling GuiOsmPoiDisplay: nodeID " + mNodeID);
					GuiOsmPoiDisplay guiNode = new GuiOsmPoiDisplay((int) mNodeID, null,
											mPos.latitude, mPos.longitude, mParent);
					guiNode.show();
					guiNode.refresh();
			//#endif 
			//#endif 
			} else {
				String url = getUrlForSite(site);
				openUrl(url);
				mParent.show();
			}
		}
	}

	public String getUrlForSite(String site) {
		String url = null;
		// checked url at 2010-01-09; free servers overloaded, can't test what's the difference
		// between full and non-full
		// http://ws.geonames.org/findNearbyWikipediaRSS?lat=47&lng=9&style=full
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.WikipediaRSS")/*Wikipedia (RSS)*/)) {
			String lang = "";
			if (! Configuration.getOnlineLangString().equals("en")) {
				lang = "lang=" + Configuration.getOnlineLangString() + "&";
			}
			url = "http://ws.geonames.org/findNearbyWikipediaRSS?" + lang + "lat="
				+ mPos.latitude * MoreMath.FAC_RADTODEC + "&lng="
				+ mPos.longitude * MoreMath.FAC_RADTODEC;
		}
		/*
		 * rss2html.com public service has closed down, 2010-08-01
		 *
		 if (site.equalsIgnoreCase("Wikipedia (Web)")) {
		 url = "http://www.rss2html.com/public/rss2html.php?TEMPLATE=template-1-2-1.htm&XMLFILE=http://ws.geonames.org/findNearbyWikipediaRSS?lat="
		 + mPos.latitude * MoreMath.FAC_RADTODEC + "%26lng="
		 + mPos.longitude * MoreMath.FAC_RADTODEC;
		 }
		*/

		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Weather")/*Weather*/)) {
			// weather underground doesn't seem to have a language switch
			// url working at 2010-01-09
			url = "http://m.wund.com/cgi-bin/findweather/getForecast?brand=mobile&query="
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "%2C"
				+ (mPos.longitude * MoreMath.FAC_RADTODEC);
		}
//#if polish.api.finland
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map*/)) {
			// url working at 2011-07-29
			url = "http://kansalaisen.karttapaikka.fi/kartanhaku/koordinaattihaku.html?feature=ktjraja&y="
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&x="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC)
				+ "&scale=10000&srsName=EPSG%3A4258&lang=fi";
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.ReittiopasAddress"))) {
			url = reittiopasUrl
				+ "&request=reverse_geocode&coordinate="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC) + ","
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&format=txt";
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.ReittiopasStop"))) {
			url = reittiopasUrl
				+ "&request=reverse_geocode&coordinate="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC) + ","
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&result_contains=stop" + "&format=txt";
		}
//#endif
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.GeoHack")/*GeoHack*/)) {
			int deglat, minlat;
			float deglatf, seclat;
			int deglon, minlon;
			float deglonf, seclon;
			deglatf = Math.abs((mPos.latitude * MoreMath.FAC_RADTODEC));
			deglat = (int)deglatf;
			minlat = (int) ((deglatf - deglat) * 60);
			seclat = ((deglatf - deglat-minlat/60)*60);
			deglonf = Math.abs((mPos.longitude * MoreMath.FAC_RADTODEC));
			deglon = (int)deglonf;
			minlon = (int) ((deglonf - deglon) * 60);
			seclon = ((deglonf - deglon-minlon/60)*60);
			String lang = "";
			// checked on 2010-01-09: url syntax has changed to:
			// fi: http://toolserver.org/~geohack/fi/60_12_12.185211_N_24_39_39.566917_E_
			// en: http://toolserver.org/~geohack/en/60_12_12.185211_N_24_39_39.566917_E_

			url = "http://toolserver.org/~geohack/" + Configuration.getOnlineLangString() + "/"
				+ deglat
				+ "_"
				+ minlat
				+ "_"
				+ seclat
				+ ((mPos.latitude < 0)?"_S_":"_N_")
				+ deglon
				+ "_"
				+ minlon
				+ "_"
				+ seclon
				+ ((mPos.longitude < 0)?"_W_":"_E_");
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Website")/*Website*/)) {
			if (mPoiUrl != null) {
				url = mPoiUrl;
			} else if (actualWay != null) {
				url = trace.getUrl(actualWay.urlIdx);
			}
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Phone")/*Phone*/)) {
			String phone;
			if (mPoiPhone != null) {
				url = "tel:" + mPoiPhone;
			} else if ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null)) {
				url = "tel:" + phone;
			}
		}
		return url;
	}

	public static String getStaticUrlForSite(String site) {
		String url = null;
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helptouch")/*Online help (touchscreen)*/)) {
			url = "https://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Touchscreen_Layout";
		}

		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helpwiki")/*Online help (Gpsmid wiki)*/)) {
			url = "https://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Main_Page";				
		}
		return url;
	}

	public static void openUrl(String url) {
		try {
			if (url != null) {
				// #debug info
				mLogger.info("Platform request for " + url);
				//#if polish.api.online
				GpsMid.getInstance().platformRequest(url);
				//#else
				GpsMid.getInstance().alert (Locale.get("guisearch.OpenUrlTitle"),
							    Locale.get("guisearch.OpenUrl") +  " " + url, Alert.FOREVER);
				//#endif
			}
		} catch (Exception e) {
			mLogger.exception("Could not open url " + url, e);
		}
	}
}
