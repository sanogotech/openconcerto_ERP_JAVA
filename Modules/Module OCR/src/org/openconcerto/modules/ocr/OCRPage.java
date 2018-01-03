package org.openconcerto.modules.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class OCRPage {
	private File fileImage;
	private BufferedImage image;
	private final List<OCRLine> lines;

	private int pageNumber;

	public OCRPage(File fileImage, List<OCRLine> lines) throws IOException {
		this.fileImage = fileImage;
		this.lines = lines;
	}

	public BufferedImage getImage() throws IOException {
		if(this.image == null){
			this.image = ImageIO.read(this.fileImage);
		}
		return this.image;
	}

	public void clearImage() {
		this.image = null;
	}
	
	public List<OCRLine> getLines() {
		return this.lines;
	}

	public File getFileImage(){
	    return this.fileImage;
	}
	
    public int getPageNumber() {
        return this.pageNumber;
    }
    
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}
	
    public void setFileImage(File fileImage){
        this.fileImage = fileImage;
    }

	public boolean contains(String str) {
		final String s = str.toLowerCase();
		for (OCRLine line : this.lines) {
			if (line.getText().toLowerCase().contains(s))
				return true;
		}
		return false;
	}
}
