import technology.tabula.Table;

import java.util.List;

/**
 * @author ShengxinZhang
 * @data 2017/11/02
 * This class include
 * 1. tables in one page.
 * 2. height of one page.
 * 3. width of one page.
 */
public class PdfPageInfo {
    private List<Table> tables = null;
    private double pageHeight = 0.0;
    private double pageWidth = 0.0;

    public PdfPageInfo(List<Table> tables, double pageHeight, double pageWidth) {
        this.tables = tables;
        this.pageHeight = pageHeight;
        this.pageWidth = pageWidth;
    }

    public List<Table> getTables() {
        return tables;
    }

    public double getPageHeight() {
        return pageHeight;
    }

    public double getPageWidth() {
        return pageWidth;
    }
}
