package org.openconcerto.modules.ocr;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HOCRParser {
    public HOCRParser() {
    }

    public List<OCRLine> parse(String htmlOcr) {
        Document doc = Jsoup.parse(htmlOcr);
        List<OCRLine> result = new ArrayList<OCRLine>();
        Elements lines = doc.select(".ocr_line");
        int size = lines.size();
        
        for (int i = 0; i < size; i++) {
            Element e = lines.get(i);
            String title = e.attr("title");
            String[] r = title.split(" ");
            result.add(new OCRLine(e.text().trim(), Integer.parseInt(r[1].replace(";","")), Integer.parseInt(r[2].replace(";","")), Integer.parseInt(r[3].replace(";","")), Integer.parseInt(r[4].replace(";",""))));
        }
		return result;
	}
}
