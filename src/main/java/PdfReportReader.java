import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Rectangle;
import technology.tabula.Table;
import technology.tabula.detectors.DetectionAlgorithm;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.awt.geom.Rectangle2D;
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


    public PdfReportReader(PDDocument pdDocumentforTabula, PDDocument pdDocumentforPDFBox) {
           this.pdDocumentforTabula = pdDocumentforTabula;
           this.pdDocumentforPDFBox = pdDocumentforPDFBox;
    }

    public List<String> extract() {

        // Temporary: if page num > 4, then return null
        if(pdDocumentforTabula.getNumberOfPages() > 4) {
            return null;
        }
        List<List<Rectangle2D>> regions = tableFilter(pdDocumentforTabula);
        List<String> content = getTextFromAreas(pdDocumentforPDFBox, regions);
        return content;
    }

    private List<List<Rectangle2D>> tableFilter(PDDocument pdDocument) {
        int pageNum = pdDocument.getNumberOfPages();
        ObjectExtractor extractor = null;
        List<List<Rectangle2D>> regions = new ArrayList();
        DetectionAlgorithm detector = new NurminenDetectionAlgorithm();

        try {
            extractor = new ObjectExtractor(pdDocument);

            //may modify here to read whole PDF file
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
                List<Rectangle2D> curRegions = new ArrayList();

                int tableIndex = 0;
                for(Table tb: curTables) {

                    double minHeightOfTable = tb.getCell(0,0).getMinY();
                    double maxHeightOfTable = tb.getCell(tb.getRows().size()-1,0).getMaxY();
                    // Find the minimum height of table by visiting all columns of the first row
                    for(int j = 0; j < tb.getCols().size(); j++){
                        if (tb.getCell(0,j).getMinY() == 0.0)
                            continue;
                        if (minHeightOfTable == 0.0 || minHeightOfTable > tb.getCell(0,j).getMinY())
                            minHeightOfTable = tb.getCell(0,j).getMinY();
                    }
//                    Find the maximum height of table by visiting all columns of the last row
//                    for(int j = 0; j < tb.getCols().size(); j++){
//                        if (maxHeightOfTable < tb.getCell(0,j).getMinY())
//                            maxHeightOfTable = tb.getCell(tb.getRows().size() - 1, j).getMaxY();
//                    }


                    tableHeight = minHeightOfTable - currentHeight;  // may set bottom buffer at here
                    Rectangle2D rect = new Rectangle2D.Double(0,currentHeight,pageWidth,minHeightOfTable - currentHeight);
                    curRegions.add(rect);
                    currentHeight = maxHeightOfTable;  // may set upper buffer here
                    tableIndex++;

                    // deal with the text area below the last table in current page
                    if(tableIndex == (curTables.size()))
                    {
                        rect = new Rectangle2D.Double(0,currentHeight,pageWidth,pageHeight - currentHeight);
                        curRegions.add(rect);
                    }
                }

                // if no table in current page
                if(curTables.size() == 0) {
                    Rectangle2D rect = new Rectangle2D.Double(0, 0, pageWidth, pageHeight);
                    curRegions.add(rect);
                }
                //add all text area from current page
                regions.add(new ArrayList<Rectangle2D>(curRegions));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return regions;
    }

    private List<String> getTextFromAreas(PDDocument pdDocument, List<List<Rectangle2D>> regions) {
        List<String> content = new ArrayList();
        PDFTextStripperByArea stripperByArea = null;
        int pageNum = pdDocument.getNumberOfPages();

        try {

            // May modify here to read whole PDF file
            for(int pageIndex = 0; pageIndex < pageNum; pageIndex++) {
                List<Rectangle2D> curRegions = regions.get(pageIndex);
                stripperByArea = new PDFTextStripperByArea();
                stripperByArea.setSortByPosition(false);

                StringBuilder contentBuilder = new StringBuilder();

                // Add page start tag
//                contentBuilder.append("\n<page\tnum=" + (pageIndex + 1) + ">\n");

                int maxTableIndex = curRegions.size() - 1;
                int curTableIndex = 0;
                for(Rectangle2D rect: curRegions) {
                    stripperByArea.addRegion(Integer.toString(curTableIndex), rect);
                    curTableIndex++;
                }
                for(curTableIndex = 0; curTableIndex <= maxTableIndex; curTableIndex++) {
                    stripperByArea.extractRegions(pdDocument.getPage(pageIndex));
                    String text = stripperByArea.getTextForRegion(Integer.toString(curTableIndex));
                    contentBuilder.append(text);
                }

                // Add page end tag
//                contentBuilder.append("\n</page\tnum=" + (pageIndex + 1) + ">\n");

                String curContent[] = contentBuilder.toString().trim().split(stripperByArea.getWordSeparator());

                //Initialize sb with a prefix of START_TAG
                StringBuilder tempsb = new StringBuilder();
                tempsb.append(PARAGRAPH_START_TAG);

                List<String> paragraphs = new ArrayList<String>();
                // Add page start tag
                paragraphs.add("\n<page\tnum=" + (pageIndex + 1) + ">\n");
                for (String line : curContent){
                    boolean isNewparagraph = line.startsWith("\n");
                    // If not a start of new paragraph, keep append to current sb
                    if(!isNewparagraph){
                        tempsb.append(line.replaceAll("\n", ""));
                    }
                    // If we find new paragraph, firstly add current sb to list, then clear sb
                    else {
                        // Adding end & start tag
                        paragraphs.add(tempsb.toString() + PARAGRAPH_END_TAG);
                        tempsb.delete(0, tempsb.length());
                        tempsb.append(PARAGRAPH_START_TAG + line.replaceAll("\n", ""));
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
        float bottom = (float)(pageHeight * 0.97);
        float top = (float)(pageHeight * 0.03);
        Rectangle rect = new Rectangle(top, 0, pageWidth, bottom - top);

        technology.tabula.Page pageBody = page.getArea(rect);

        return pageBody;
    }

}