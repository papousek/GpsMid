package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.C;


public class QueueDataReader extends QueueReader implements Runnable {
    //#debug error
	private final static Logger logger=Logger.getInstance(QueueDataReader.class,Logger.ERROR);

	private final Trace trace;
	public QueueDataReader(Trace trace){
		super("DataReader");
		this.trace = trace;
		
	}
		
	public void readData(Tile t, Object notifyReady) throws IOException{
		logger.info("Reading tile: " + t);
		SingleTile tt=(SingleTile) t;
		InputStream is=GpsMid.getInstance().getConfig().getMapResource("/t"+tt.zl+tt.fileId+".d");
		if (is == null){
		    //#debug error
				logger.error("file inputStream"+"/t"+tt.zl+tt.fileId+".d"+" not found" );
			tt.state=0;
			return;
		}
//		logger.info("open DataInputStream");
		DataInputStream ds=new DataInputStream(is);
		if (ds == null){
//			logger.error("file DataImputStream "+url+" not found" );
			tt.state=0;
			return;
		}
//		end open data from JAR
//		logger.info("read Magic code");
		if (ds.readByte()!=0x54){
//			logger.error("not a MapMid-file");
			throwError( "not a MapMid-file", tt);
		}
		tt.centerLat = ds.readFloat();
		tt.centerLon = ds.readFloat();
		logger.info("Center coordinates of tile: " + tt.centerLat + "/" + tt.centerLon);
		int nodeCount=ds.readShort();
		short[] radlat = new short[nodeCount];
		short[] radlon = new short[nodeCount];
		int iNodeCount=ds.readShort();
		//#debug error
		  logger.trace("nodes total :"+nodeCount + "  interestNode :" + iNodeCount);
		int[] nameIdx=new int[iNodeCount];
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		byte[] type = new byte[iNodeCount];
		byte flag=0;
		logger.info("About to read nodes");
		try {
			for (int i=0; i< nodeCount;i++){
				//#debug error
				//	logger.info("read coord :"+i+"("+nodeCount+")");
				flag=ds.readByte();
				if ((flag & C.NODE_MASK_ROUTENODELINK) > 0){
					ds.readShort();
				}
								
				radlat[i] = ds.readShort();
				radlon[i] = ds.readShort();
				
				if ((flag & C.NODE_MASK_NAME) > 0){
					if ((flag & C.NODE_MASK_NAMEHIGH) > 0) {
						nameIdx[i]=ds.readInt();
					} else {
						nameIdx[i]=ds.readShort();
					}
				} 
				if ((flag & C.NODE_MASK_TYPE) > 0){
					type[i]=ds.readByte();
				}
			}
			tt.nameIdx=nameIdx;
			tt.nodeLat=radlat;
			tt.nodeLon=radlon;
			tt.type=type;
		} catch (RuntimeException e) {
			//#debug error
			logger.trace("RuntimeExeption :"+e.getMessage());
			//#debug error
			e.printStackTrace();
			throwError(e, "reading Nodes", tt);
		}
		logger.info("read nodes");
		if (ds.readByte()!=0x55){
			//#debug
			logger.error("Reading Nodes whent wrong / Start of Ways not found");
			throwError("Nodes not OK", tt);
		}
		int wayCount=ds.readByte();
//		logger.trace("reading " + wayCount + " ways");
		if (wayCount < 0) {
			wayCount+=256;
		}
//		logger.trace("reading " + wayCount + " ways");
		int lastread=0;
		try {
			tt.ways = new Way[wayCount];
			for (int i=0; i< wayCount;i++){				
				byte flags=ds.readByte();
				if (flags != 128){
//				showAlert("create Way " + i);
					Way w=new Way(ds,flags,tt);
					tt.ways[i]=w;
				}
				lastread=i;
			}
		} catch (RuntimeException e) {
			throwError(e,"Ways(last ok index " + lastread,tt);
		}
		if (ds.readByte() != 0x56){
			throwError("Ways not OK", tt);
		} else {
//			logger.info("ready");
		}
		ds.close();

		tt.dataReady();
		trace.newDataReady();
		if (notifyReady != null) {			
			synchronized(notifyReady) {
				notifyReady.notifyAll();
			}
		}
		logger.info("DataReader ready "+ tt.fileId + " " + tt.nodeLat.length + " Nodes " + tt.ways.length + " Ways" );

//		}

	}
	private void throwError(String string, SingleTile tt) throws IOException {
		throw new IOException("MapMid-file corrupt: " + string + " zl=" + tt.zl + " fid=" + tt.fileId);
		
	}
	private void throwError(RuntimeException e, String string, SingleTile tt) throws IOException {
		throw new IOException("MapMid-file corrupt: " + string + " zl=" + tt.zl + " fid=" + tt.fileId + " :" + e.getMessage());
	}
	public String toString(){
		int loop;
		StringBuffer ret=new StringBuffer();
		SingleTile tt;
		ret.append("\nliving ");
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(SingleTile) livingQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		ret.append("\nrequest ");
		for (loop=0; loop < requestQueue.size(); loop++){
			tt=(SingleTile) requestQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		return ret.toString();
	}

}
