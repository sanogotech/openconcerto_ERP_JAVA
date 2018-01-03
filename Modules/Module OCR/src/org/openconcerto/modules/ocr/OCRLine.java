package org.openconcerto.modules.ocr;

public class OCRLine {
	private final String text;
	private final int xMin, yMin, xMax, yMax;

	public OCRLine(String text, int xMin, int yMin, int xMax, int yMax) {
		super();
		this.text = text;
		this.xMin = xMin;
		this.yMin = yMin;
		this.xMax = xMax;
		this.yMax = yMax;
	}

	public String getText() {
		return this.text;
	}

	public int getxMin() {
		return this.xMin;
	}

	public int getyMin() {
		return this.yMin;
	}

	public int getxMax() {
		return this.xMax;
	}

	public int getyMax() {
		return this.yMax;
	}

}
