package org.openconcerto.modules.ocr.parser;

import org.openconcerto.modules.ocr.InvoiceOCR;
import org.openconcerto.modules.ocr.OCRPage;

public interface InvoiceParser {
	public boolean parse(OCRPage page);

	public boolean isValid();

	public boolean needModePage();

	public void reset();

	public InvoiceOCR getInvoice();

}
