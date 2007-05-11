/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;

import de.ueller.osmToGpsMid.model.Bounds;

/**
 * @author hmueller
 *
 */
public class Configuration {
		private final String file;
		private ResourceBundle rb;
		private ResourceBundle vb;
		private String tmp=null;
		private final String planet;
		private boolean highway_only=false;
		public boolean useHighway=true;
		public boolean useRailway=true;
		public boolean useRiver=true;
		public boolean useCycleway=true;
		public boolean useAmenity=true;
		public boolean useLanduse=true;
		public boolean useNatural=true;
		public boolean useLeisure=true;
		public boolean useWaterway=true;

//		private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//				.getBundle(BUNDLE_NAME);

		public Configuration(String planet,String file) {
			this.planet = planet;
			this.file = file;
			try {
				InputStream cf;
				try {
					cf = new FileInputStream(file+".properties");
				} catch (FileNotFoundException e) {
					System.out.println(file + ".properties not found, try bundled version");
					cf=getClass().getResourceAsStream("/"+file+".properties");
					if (cf == null){
						throw new IOException(file + " is not a valid region");
					}
				}
				rb= new PropertyResourceBundle(cf);
				vb=new PropertyResourceBundle(getClass().getResourceAsStream("/version.properties"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public  String getString(String key) {
			try {
				
				return rb.getString(key);
			} catch (MissingResourceException e) {
				return '!' + key + '!';
			}
		}
		public float getFloat(String key){
//			try {
				return Float.parseFloat(getString(key));
//			} catch (Exception e) {
//				System.err.println(getString(key) + " not a number");
//				return Float.NaN;
//			}
		}
		public String getName(){
			return getString("bundle.name");
		}
		
		public InputStream getJarFile(){
			return getClass().getResourceAsStream("/"+vb.getString("app")
			+"-"+getVersion()
			+".jar");
		}
		public String getJarFileName(){
			return vb.getString("app")
			+"-"+getVersion()
			+".jar";
		}
		public String getTempDir(){
			return getTempBaseDir()+"/"+"map";
		}
		public String getTempBaseDir(){
			if (tmp==null){
				tmp="temp"+ new Random(System.currentTimeMillis()).nextLong();
			}
			return tmp;
//			return getString("tmp.dir");
		}
		public File getPlanet(){
			return new File(planet);
		}
		public Bounds[] getBounds(){
			int i;
			i=0;
			try {
				while (i<10000){
					getFloat("region."+(i+1)+".lat.min");
					i++;
				}
			} catch (RuntimeException e) {
				System.out.println("found " + i + " bounds");
			}
			Bounds[] ret=new Bounds[i];
			for (int l=0;l < i;l++){
				ret[l]=new Bounds();
				ret[l].extend(getFloat("region."+(l+1)+".lat.min"),
						getFloat("region."+(l+1)+".lon.min"));
				ret[l].extend(getFloat("region."+(l+1)+".lat.max"),
						getFloat("region."+(l+1)+".lon.max"));
			}
			return ret;
		}

		/**
		 * @return
		 */
		public String getVersion() {
			return vb.getString("version");
		}

		public boolean isHighway_only() {
			return highway_only;
		}
}
