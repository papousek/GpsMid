package de.ueller.osmToGpsMid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;



public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1){
			FileInputStream fr;
			try {
				Configuration c=new Configuration(args[0],args[1]);
				fr = new FileInputStream(args[0]);
				OxParser parser = new OxParser(fr,c);
				System.out.println("read Nodes " + parser.nodes.size());
				System.out.println("read Ways  " + parser.ways.size());
				CreateGpsMidData cd=new CreateGpsMidData(parser,args[1]);
				new SplitLongWays(parser);
				cd.exportMapToMid();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
