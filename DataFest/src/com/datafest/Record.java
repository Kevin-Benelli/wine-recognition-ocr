package com.datafest;

public class Record implements Cloneable {
	//meta info
	private String pageId; //name of the file
	private String xy; //position in the image where the record begins
	private int lines; //the number of lines the record consists of
	private String rawText; //raw record text obtained by the matcher
	
	private String type; //wine, beer, spirit, etc.
	private String wineType; //still or sparkling
	private String color; //white, red, rose
	private String country;
	private String name;
	private String section; //section header
	private int year; //year of the wine
	private String bottleType; //standard, magnum, half bottle, etc.
	private float perPrice; //price per bottle
	private float casePrice; //price per case
	private long id; //wine id (the number before the name)
	private String comment; //any other text that we didn't extract
	private String cleanedText; //cleaned text (dots and line breaks removed)
	
	public Record() {
		comment = "";
	}
	
	public Object clone()throws CloneNotSupportedException{  
		return (Record)super.clone();  
	}
	
	public String getPageId() {
		return pageId;
	}
	public void setPageId(String pageId) {
		this.pageId = pageId;
	}
	public String getXy() {
		return xy;
	}
	public void setXy(String xy) {
		this.xy = xy;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getWineType() {
		return wineType;
	}
	public void setWineType(String wineType) {
		this.wineType = wineType;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSection() {
		return section;
	}
	public void setSection(String section) {
		this.section = section;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public String getBottleType() {
		return bottleType;
	}
	public void setBottleType(String bottleType) {
		this.bottleType = bottleType;
	}
	public float getPerPrice() {
		return perPrice;
	}
	public void setPerPrice(float perPrice) {
		this.perPrice = perPrice;
	}
	public float getCasePrice() {
		return casePrice;
	}
	public void setCasePrice(float casePrice) {
		this.casePrice = casePrice;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getRawText() {
		return rawText;
	}
	public void setRawText(String rawText) {
		this.rawText = rawText;
	}
	public String getCleanedText() {
		return cleanedText;
	}
	public void setCleanedText(String cleanedText) {
		this.cleanedText = cleanedText;
	}

	public int getLines() {
		return lines;
	}

	public void setLines(int lines) {
		this.lines = lines;
	}

}
