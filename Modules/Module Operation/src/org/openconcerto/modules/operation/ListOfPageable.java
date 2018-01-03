package org.openconcerto.modules.operation;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.util.List;

public class ListOfPageable implements Pageable {
    private List<Pageable> list;
    private int numberOfPages = 0;

    public ListOfPageable(List<Pageable> pageables) {
        this.list = pageables;
        for (Pageable pageable : this.list) {
            this.numberOfPages += pageable.getNumberOfPages();
        }
        System.err.println("ListOfPageable.ListOfPageable() " + pageables.size() + " pageable -> " + this.numberOfPages + " pages");
    }

    @Override
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        int c = 0;
        for (Pageable pageable : this.list) {
            int s = pageable.getNumberOfPages();
            if (pageIndex < c + s) {
                return pageable.getPrintable(pageIndex - c);
            } else {
                c += s;
            }
        }
        return null;
    }

    @Override
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        final PageFormat format = new PageFormat();
        format.setPaper(new A4());
        return format;
    }

    @Override
    public int getNumberOfPages() {
        return numberOfPages;
    }

}
