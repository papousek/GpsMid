/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.name.Name;
import de.ueller.osmToGpsMid.model.name.Names;

/**
 * @author hmueller
 *
 */
public class SearchList {
	Names names;

	public SearchList(Names names) {
		super();
		this.names = names;
	}

	public void createSearchList(String path){
		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			String lastStr=null;
			String lastFid="";
			int curPos=0;
			for (Name mapName : names.getCanons()) {
				String string=mapName.getCanonFileName();
				int eq=names.getEqualCount(string,lastStr);
				if (! lastFid.equals(mapName.getCanonFileId())){
					if (ds != null) ds.close();
					lastFid=mapName.getCanonFileId();
					String fileName = path+"/s"+lastFid+".d";
//					System.out.println("open "+fileName);
					fo = new FileOutputStream(fileName);
					ds = new DataOutputStream(fo);
					curPos=0;
					eq=0;
					lastStr=null;
				}
				String wrString=string.substring(eq);
				
				int delta = eq-curPos;
				if (delta <0){
					delta = -delta;
					delta += 0x80;
				}
				
					long l=0;
					if (wrString.length() > 0)
						l = Long.parseLong(wrString);
					if (l < Byte.MAX_VALUE){
//						System.out.println("byte   " + (delta)  + " " + (byte) delta + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta);
						ds.writeByte((int) l);
					} else if (l < Short.MAX_VALUE){
//						System.out.println("short  " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta | 0x20);
						ds.writeShort((int) l);
					} else if (l < Integer.MAX_VALUE){					
//						System.out.println("int    " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta | 0x40);
						ds.writeInt((int) l);
					} else {
//						System.out.println("long   " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta| 0x60);
						ds.writeLong(l);
					}
				int nameIdx = mapName.getIndex();
				if (nameIdx >= Short.MAX_VALUE) {
					ds.writeInt(nameIdx | 0x80000000);
				} else {
					ds.writeShort(nameIdx);
				}
				for (Entity e : mapName.getEntitys()){
					Node center=null;
					if (e instanceof Node) {
						Node n = (Node) e;
						ds.writeByte(-1*n.getType(Configuration.getConfiguration()));
						center=n;
//						System.out.println("entryType " + n.getNameType() + " idx=" + mapName.getIndex());
					}
					if (e instanceof Way) {
						Way w = (Way) e;
						ds.writeByte(w.getNameType());
//						System.out.println("entryType " + w.getNameType() + " idx=" + mapName.getIndex());
						center=w.getMidPoint();
					}
					ArrayList<Entity> isIn=new ArrayList<Entity>();
					Entity nb=e.nearBy;
					while (nb != null && !isIn.contains(nb)){
						if (nb.getName() != null)
							isIn.add(nb);
						nb=nb.nearBy;
					}
//					
					/*
					 * Don't know how to handle actual is_in entries.
					 * These names won't necessarily be in our name database
					String in=e.tags.get("is_in");
//					if (in != null){
//						ds.writeByte(isIn.size()+1);
//						ds.writeShort(names.getNameIdx(in));
//					} else {
						ds.writeByte(isIn.size());
//					}
					*/
					ds.writeByte(isIn.size());
					for (Entity e1 : isIn){
						int isinIdx = names.getNameIdx(e1.getName());
						if (isinIdx < 0) {
							System.out.println("Invalid isin (" + e1 + ") for Entety " + e + " with Name \"" + e1.getName() + "\"");
						}
						if (isinIdx >= Short.MAX_VALUE) {
							ds.writeInt(isinIdx | 0x80000000);
						} else {				  
							ds.writeShort(isinIdx);
						}
					}
					ds.writeFloat(MyMath.degToRad(center.lat));
					ds.writeFloat(MyMath.degToRad(center.lon));
				}
				ds.writeByte(0);
//				ds.writeUTF(string.substring(eq));
				curPos=eq;
				lastStr=string;
			}
			if (ds != null) ds.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createNameList(String path) {
//		names1 = getNames1();

		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			FileOutputStream foi = new FileOutputStream(path+"/names-idx.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			String lastStr=null;
			fo = new FileOutputStream(path+"/names-0.dat");
			ds = new DataOutputStream(fo);
			int curPos=0;
			int idx=0;
			short fnr=1;
			short fcount=0;
			for (Name mapName : names.getNames()) {
				String string=mapName.getName();
				int eq=names.getEqualCount(string,lastStr);
				if ((eq==0 && fcount>100) || (fcount > 150 && eq < 2)){
					dsi.writeInt(idx);
					if (ds != null) ds.close();
					fo = new FileOutputStream(path+"/names-"+fnr+".dat");
					ds = new DataOutputStream(fo);
//					System.out.println("wrote names " + fnr + " with "+ fcount + " names");
					fnr++;
					curPos=0;
					eq=0;
					fcount=0;
					lastStr=null;
				}
				ds.writeByte(eq-curPos);
				ds.writeUTF(string.substring(eq));
//				ds.writeShort(getWayNameIndex(mapName.getIs_in(), null));
//				System.out.println("" + (eq-curPos) + "'" +string.substring(eq)+"' '"+string);
				curPos=eq;
				lastStr=string;
				idx++;
				fcount++;
//				ds.writeUTF(string);
			}
			dsi.writeInt(idx);
			ds.close();
			dsi.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
