import java.awt.geom.Rectangle2D;

/**
 * @author ShengxinZhang
 * @data 2017/11/02
 */
public class PdfRegion {
    private Rectangle2D rect = null;
    private boolean isTable = false;
    private String tableContent = null;

    public PdfRegion(Rectangle2D rect, String tableContent, boolean isTable) {
        this.rect = rect;
        this.isTable = isTable;
        this.tableContent = tableContent;
    }

    public Rectangle2D getRect() {return this.rect;}
    public String getTableContent() {return this.tableContent;}
    public boolean isTable() {return isTable;}
}
