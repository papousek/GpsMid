package de.ueller.osmToGpsMid.model;


public class Line extends Entity {

	public final Node	from;
	public final Node	to;

	public Line(Node from, Node to, long id) {
		this.from = from;
		this.to = to;
		this.id = id;
	}

	public Line(long id) {
		this.id=id;
		from=to=null;
	}
	
	public boolean isValid(){
		if (from==null) return false;
		if (to==null) return false;
		return true;
	}

}
