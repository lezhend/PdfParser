import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import technology.tabula.*;
import technology.tabula.detectors.DetectionAlgorithm;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangshengxin on 8/30/17.
 */
public class PdfReportReader {
    private PDDocument pdDocumentforTabula = null;
    private PDDocument pdDocumentforPDFBox = null;

    private static final String PARAGRAPH_START_TAG = "<paragraph>";
    private static final String PARAGRAPH_END_TAG = "</paragraph>";
    private static final String TABLE_START_TAG = "<table>";
    private static final String TABLE_END_TAG = "</table>";


    public PdfReportReader(File file) throws IOException {
        this.pdDocumentforTabula = PDDocument.load(file);
        this.pdDocumentforPDFBox = PDDocument.load(file);
    }

    public List<String> extract() {
        List<List<PdfRegion>> regions = tableFilter(pdDocumentforTabula);
        List<String> content = getTextFromAreas(pdDocumentforPDFBox, regions);
        return content;
    }

    class PdfRegion {
        private Rectangle2D rect = null;
        private boolean isTable = false;
        private String tableContent = null;

        private PdfRegion(Rectangle2D rect, String tableContent, boolean isTable) {
            this.rect = rect;
            this.isTable = isTable;
            this.tableContent = tableContent;
        }

        private Rectangle2D getRect() {return this.rect;}
        private String getTableContent() {return this.tableContent;}
        private boolean isTable() {return isTable;}
    }

    private List<List<PdfRegion>> tableFilter(PDDocument pdDocument) {
        int pageNum = pdDocument.getNumberOfPages();
        ObjectExtractor extractor = null;
        List<List<PdfRegion>> regions = new ArrayList();
        DetectionAlgorithm detector = new NurminenDetectionAlgorithm();

        try {
            extractor = new ObjectExtractor(pdDocument);

            for (int pageIndex = 1; pageIndex <= pageNum; pageIndex++) {
                technology.tabula.Page rawPage = extractor.extract(pageIndex);
                technology.tabula.Page page = removeHeaderandFooter(rawPage);
                List<Rectangle> guesses = detector.detect(page);
                List<Table> curTables = new ArrayList();
                BasicExtractionAlgorithm basicExtractor = new BasicExtractionAlgorithm();

                //get all tables in single PDF page
                for (Rectangle guessRect : guesses) {
                    Page guess = page.getArea(guessRect);
                    curTables.addAll(basicExtractor.extract(guess));
                }

                double currentHeight = 0.0;
                double pageHeight = page.getHeight();
                double pageWidth = page.getWidth();
                double tableHeight = 0.0;
                List<PdfRegion> curRegions = new ArrayList();

                int tableIndex = 0;
                for(Table tb: curTables) {
                    // Get table content firstly

                    String wholeTableContenxt = "";
                    for(int i = 0; i < tb.getRows().size(); i++) {
                        String result = "";
                        for (int j = 0; j < tb.getCols().size(); j++) {
                            String test = tb.getCell(i, j).getText();
                            result += "; " + test;
                        }
                        wholeTableContenxt += " \n" + result;
                    }
                    double minHeightOfTable = tb.getCell(0,0).getMinY();
                    double maxHeightOfTable = tb.getCell(tb.getRows().size()-1,0).getMaxY();
                    // Find the minimum height of table by visiting all columns of the first row
                    for(int j = 0; j < tb.getCols().size(); j++){
                        if (tb.getCell(0,j).getMinY() == 0.0)
                            continue;
                        if (minHeightOfTable == 0.0 || minHeightOfTable > tb.getCell(0,j).getMinY())
                            minHeightOfTable = tb.getCell(0,j).getMinY();
                    }
                    // Find the maximum height of table by visiting all columns of the last row
                    for(int j = 0; j < tb.getCols().size(); j++) {
                        if (maxHeightOfTable < tb.getCell(tb.getRows().size() - 1, j).getMinY())
                            maxHeightOfTable = tb.getCell(tb.getRows().size() - 1, j).getMaxY();
                    }

                    Rectangle2D rect_text = new Rectangle2D.Double(0, currentHeight, pageWidth,minHeightOfTable - currentHeight);
                    Rectangle2D rect_table = new Rectangle2D.Double(0, minHeightOfTable, pageWidth,maxHeightOfTable - minHeightOfTable);
                    curRegions.add(new PdfRegion(rect_text, null, false));
                    curRegions.add(new PdfRegion(rect_table, wholeTableContenxt, true));
                    currentHeight = maxHeightOfTable;  // may set upper buffer here
                    tableIndex++;

                    // deal with the text area below the last table in current page
                    if(tableIndex == (curTables.size()))
                    {
                        Rectangle2D bottomTextArea = new Rectangle2D.Double(0,currentHeight,pageWidth,pageHeight - currentHeight);
                        curRegions.add(new PdfRegion(bottomTextArea, null, false));
                    }
                }

                // if no table in current page
                if(curTables.size() == 0) {
                    Rectangle2D rect = new Rectangle2D.Double(0, 0, pageWidth, pageHeight);
                    curRegions.add(new PdfRegion(rect, null, false));
                }
                //add all text and table area from current page
                regions.add(new ArrayList<PdfRegion>(curRegions));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return regions;
    }

    private List<String> getTextFromAreas(PDDocument pdDocument, List<List<PdfRegion>> regions) {
        List<String> content = new ArrayList();
        PDFTextStripperByArea stripperByArea = null;
        int pageNum = pdDocument.getNumberOfPages();

        try {

            // May modify here to read whole PDF file
            for(int pageIndex = 0; pageIndex < pageNum; pageIndex++) {
                List<PdfRegion> curRegions = regions.get(pageIndex);
                stripperByArea = new PDFTextStripperByArea();
                stripperByArea.setSortByPosition(true);
                stripperByArea.setWordSeparator(" ");

                StringBuilder contentBuilder = new StringBuilder();

                int maxTableIndex = curRegions.size() - 1;
                int curTableIndex = 0;
                for(PdfRegion rect: curRegions) {
                    stripperByArea.addRegion(Integer.toString(curTableIndex), rect.getRect());
                    curTableIndex++;
                }
                for(curTableIndex = 0; curTableIndex <= maxTableIndex; curTableIndex++) {
                    String text = "";
                    if(curRegions.get(curTableIndex).isTable()) {
                        // The space before \n is important
                        // Because later we will cut the whole Stringbuilder in terms of space
                        text = " \n" + TABLE_START_TAG + " \n" + curRegions.get(curTableIndex).getTableContent() + " \n" + TABLE_END_TAG + " \n";
                        contentBuilder.append(text);
                    }
                    else {
                        stripperByArea.extractRegions(pdDocument.getPage(pageIndex));
//                        System.out.println("pageIndex:" + pageIndex);
                        text = stripperByArea.getTextForRegion(Integer.toString(curTableIndex));
                        contentBuilder.append(text);
                    }

                }

                String curContent[] = contentBuilder.toString().trim().split(stripperByArea.getWordSeparator());

                StringBuilder tempsb = new StringBuilder();

                List<String> paragraphs = new ArrayList<String>();
                // Add page start tag
                //paragraphs.add("\n<page\tnum=" + (pageIndex + 1) + ">\n");
                for (String line : curContent){
                    boolean isNewparagraph = line.startsWith("\n");
                    // If this is start or end of table
                    if(line.contains(TABLE_START_TAG) || line.contains(TABLE_END_TAG)) {
                        paragraphs.add(tempsb.toString()  + "\n");
                        paragraphs.add(line + "\n");
                        tempsb.delete(0, tempsb.length());
                        continue;
                    }

                    // If not a start of new paragraph, keep append to current sb
                    if(!isNewparagraph){
                        tempsb.append(line.replaceAll("\n", ""));
                    }
                    // If we find new paragraph, firstly add current sb to list, then clear sb
                    else {

                        paragraphs.add(tempsb.toString()  + "\n");
                        tempsb.delete(0, tempsb.length());
                        tempsb.append(line.replaceAll("\n", ""));
                    }
                }
                content.addAll(paragraphs);
            }

        } catch (IOException e) {
            StackTraceElement l = e.getStackTrace()[e.getStackTrace().length-1];
            System.out.println(
                    l.getClassName()+"/"+l.getMethodName()+":"+l.getLineNumber());
            System.out.println(e);
        }

        return content;
    }


    private technology.tabula.Page removeHeaderandFooter(technology.tabula.Page page) {
        float pageHeight = (float)page.getHeight();
        float pageWidth = (float)page.getWidth();
        float bottom = (float)(pageHeight * 0.955); // 0.955 is the largest gate value to ignore the page footer on tested sample PDF file
        float top = (float)(pageHeight * 0.01);
        Rectangle rect = new Rectangle(top, 0, pageWidth, bottom - top);

        technology.tabula.Page pageBody = page.getArea(rect);

        return pageBody;
    }
}