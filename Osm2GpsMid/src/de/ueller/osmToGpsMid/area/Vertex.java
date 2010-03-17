package de.ueller.osmToGpsMid.area;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Node;

public class Vertex {
//	private float	lat;
//	private float	lon;
//	private long	id;
	private Vertex next;
	private Vertex prev;
	private Outline outline;
	private Node node;

	public Vertex(float node_lat, float node_lon, long id,Outline outline) {
		node=new Node(node_lat,node_lon,id);
		this.outline=outline;
	}
	
	public Vertex(float node_lat, float node_lon, long id) {
		node=new Node(node_lat,node_lon,id);
	}
	public Vertex(Node n,Outline outline) {
		node =n;
		this.outline=outline;
	}
	public Node getNode(){
		return node;
	}
	public void setOutline(Outline o){
		outline=o;
	}
	public Outline getOutline(){
		return outline;
	}

	public float getLat() {
		return node.lat;
	}

	public void setLat(float lat) {
		node.lat = lat;
	}

	public float getLon() {
		return node.lon;
	}

	public float getX() {
		return node.lon;
	}

	public float getY() {
		return node.lat;
	}

	public void setLon(float lon) {
		node.lon = lon;
	}

	public long getId() {
		return node.id;
	}

	public void setId(long id) {
		node.id = id;
	}


	public Vertex getNext() {
		return next;
	}


	public void setNext(Vertex next) {
		this.next = next;
	}


	public Vertex getPrev() {
		return prev;
	}


	public void setPrev(Vertex prev) {
		this.prev = prev;
	}

	public float cross(float lat,float lon){
		return node.lat*lon-node.lon*lat;
	}
	public float cross(Vertex other){
		return node.lat*other.getLon()-node.lon*other.getLat();
	}

	public boolean partOf(Outline o){
		return (outline == o);
	}

	public Vertex minus(Vertex other){
		return new Vertex(node.lat-other.getLat(),node.lon-other.getLon(),node.id,null);
	}
//	
//	public float mult(Node other){
//		return lat*other.getLat() + lon*other.getLon();
//	}
//	
//	public float module(Node other){
//		return (float) Math.sqrt(this.mult(this));
//	}

	public String toString() {
		return String.format(" %d(%1.10f/%1.10f) ", node.id,node.lat,node.lon);
	}


	protected Vertex clone() {
		return new Vertex(node,outline);
	}
	
	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
			bound.extend(node.lat, node.lon);
	}

}
