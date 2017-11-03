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
 * @author ShengxinZhang
 * @date 2017/08/30
 */
public class PdfReportReader {
    private PDDocument pdDocumentforTabula = null;
    private PDDocument pdDocumentforPDFBox = null;

    private static final String TABLE_START_TAG = "<table>";
    private static final String TABLE_END_TAG = "</table>";

    /**
     * Initial to read file.
     * Since Tabula will automatically close a PDF file after detect tables.
     * We need open the same file twice.
     *
     * @param file
     * @throws IOException
     */
    public PdfReportReader(File file) throws IOException {
        this.pdDocumentforTabula = PDDocument.load(file);
        this.pdDocumentforPDFBox = PDDocument.load(file);
    }

    /**
     * The upper level method to travers all the pages in one PDF file.
     * 1. Traverse all pages, detect table regions, text regions and size of each page.
     * 2. Using the result in step 1 to extract & generate final text result.
     *
     * @return a list of String, each string is a line in pure .txt file.
     */

    public List<String> extractResult() {
        int pageNum = pdDocumentforTabula.getNumberOfPages();
        List<List<PdfRegion>> regions = new ArrayList();
        ObjectExtractor extractor = null;
        try {
            extractor = new ObjectExtractor(pdDocumentforTabula);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int pageIndex = 1; pageIndex <= pageNum; pageIndex++) {
            // Get tables in current page
            PdfPageInfo pdfPageInfo =
                    singlePageTableExtractor(pageIndex, extractor);

            // Get text & table Regions based on tables and page size detected by Tabula
            double pageHeight = pdfPageInfo.getPageHeight();
            double pageWidth = pdfPageInfo.getPageWidth();
            List<Table> currentPageTables = pdfPageInfo.getTables();
            List<PdfRegion> currentPagePdfRegion =
                    singlePagePdfRegionExtractor(currentPageTables, pageHeight, pageWidth);
            regions.add(new ArrayList<PdfRegion>(currentPagePdfRegion));
        }
        List<String> content = getTextFromRegions(pdDocumentforPDFBox, regions);
        return content;
    }

    /**
     * This function is to extract all tables gueesed by tabula in one page.
     * @param pageIndex
     * @param extractor
     * @return
     */
    private PdfPageInfo singlePageTableExtractor(int pageIndex, ObjectExtractor extractor) {
        DetectionAlgorithm detector = new NurminenDetectionAlgorithm();
        technology.tabula.Page rawPage = extractor.extract(pageIndex);
        technology.tabula.Page page = removeHeaderandFooter(rawPage);
        List<Rectangle> guesses = detector.detect(page);
        List<Table> singlePagetables = new ArrayList();
        BasicExtractionAlgorithm basicExtractor = new BasicExtractionAlgorithm();
        // Get all tables in single PDF page
        for (Rectangle guessRect : guesses) {
            Page guess = page.getArea(guessRect);
            singlePagetables.addAll(basicExtractor.extract(guess));
        }

        // Get width & height of this page
        double pageHeight = page.getHeight();
        double pageWidth = page.getWidth();
        PdfPageInfo pdfPageInfo =
                new PdfPageInfo(singlePagetables, pageHeight, pageWidth);
        return pdfPageInfo;
    }

    /**
     * This function is to extract structured text in given table detected by Tabula.
     * @param table
     * @return
     */

    private String extractTableContext(Table table) {
        String tableContent = "";
        for(int i = 0; i < table.getRows().size(); i++) {
            String result = "";
            for (int j = 0; j < table.getCols().size(); j++) {
                String test = table.getCell(i, j).getText();
                result += "; " + test;
            }
            tableContent += " \n" + result;
        }
        return tableContent;
    }

    /**
     * This function is to extract all text regions & table regions
     * in one pages based on the guessed tables by tabula.
     * @param singlePagetables
     * @param pageHeight
     * @param pageWidth
     * @return
     */
    private List<PdfRegion> singlePagePdfRegionExtractor(List<Table> singlePagetables,
                                                         double pageHeight, double pageWidth) {
        List<PdfRegion> singlePagePdfRegions = new ArrayList();
        int tableIndex = 0;
        double currentHeight = 0.0;
        for(Table tb: singlePagetables) {
            // Get table content firstly
            String currentTableContenxt = extractTableContext(tb);

            double minHeightOfTable = tb.getCell(0,0).getMinY();
            double maxHeightOfTable = tb.getCell(tb.getRows().size()-1,0).getMaxY();
            // Find the minimum height of table by visiting all columns of the first row
            for(int j = 0; j < tb.getCols().size(); j++){
                if (tb.getCell(0,j).getMinY() == 0.0) {
                    continue;
                }
                if (minHeightOfTable == 0.0 || minHeightOfTable > tb.getCell(0,j).getMinY()) {
                    minHeightOfTable = tb.getCell(0,j).getMinY();
                }
            }
            // Find the maximum height of table by visiting all columns of the last row
            for(int j = 0; j < tb.getCols().size(); j++) {
                if (maxHeightOfTable < tb.getCell(tb.getRows().size() - 1, j).getMinY()) {
                    maxHeightOfTable = tb.getCell(tb.getRows().size() - 1, j).getMaxY();
                }
            }

            Rectangle2D rectText = new Rectangle2D.Double(0,
                    currentHeight, pageWidth,minHeightOfTable - currentHeight);
            Rectangle2D rectTable = new Rectangle2D.Double(0,
                    minHeightOfTable, pageWidth,maxHeightOfTable - minHeightOfTable);
            PdfRegion tmpTextRegion = new PdfRegion(rectText, null, false);
            PdfRegion tmpTableRegion = new PdfRegion(rectTable, currentTableContenxt, true);

            singlePagePdfRegions.add(tmpTextRegion);
            singlePagePdfRegions.add(tmpTableRegion);
            // may set upper buffer here
            currentHeight = maxHeightOfTable;
            tableIndex++;
            // deal with the text area below the last table in current page
            if(tableIndex == (singlePagetables.size()))
            {
                Rectangle2D bottomTextArea = new Rectangle2D.Double(0,currentHeight,
                        pageWidth,pageHeight - currentHeight);
                singlePagePdfRegions.add(new PdfRegion(bottomTextArea, null, false));
            }
        }

        // if no table in current page
        if(singlePagetables.size() == 0) {
            Rectangle2D rect = new Rectangle2D.Double(0, 0, pageWidth, pageHeight);
            singlePagePdfRegions.add(new PdfRegion(rect, null, false));
        }
        return singlePagePdfRegions;
    }

    /**
     * Extract table & pure text content from whole PDF file based on the
     * PdfRegions generated in last step.
     *
     * @param pdDocument
     * @param regions
     * @return a list of String, each string is a line in pure .txt file.
     */

    private List<String> getTextFromRegions(PDDocument pdDocument, List<List<PdfRegion>> regions) {
        List<String> content = new ArrayList();
        PDFTextStripperByArea stripperByArea = null;
        int pageNum = pdDocument.getNumberOfPages();
        try {
            for(int pageIndex = 0; pageIndex < pageNum; pageIndex++) {
                List<PdfRegion> curRegions = regions.get(pageIndex);
                stripperByArea = new PDFTextStripperByArea();
                stripperByArea.setSortByPosition(true);
                stripperByArea.setWordSeparator(" ");
                StringBuilder contentBuilder = new StringBuilder();

                int maxRegionIndex = curRegions.size() - 1;
                int curTableIndex = 0;
                // Add all regions to the stripper and tag reagions with number.
                for(PdfRegion rect: curRegions) {
                    stripperByArea.addRegion(Integer.toString(curTableIndex), rect.getRect());
                    curTableIndex++;
                }
                // Deal with regions added in last step.
                for(curTableIndex = 0; curTableIndex <= maxRegionIndex; curTableIndex++) {
                    String text = "";
                    // if dealing with a table region, we just fetch the table content we extracted earlier.
                    if(curRegions.get(curTableIndex).isTable()) {
                        // The space before \n is necessary.
                        // Because later we will cut the whole Stringbuilder in terms of space
                        text = " \n" + TABLE_START_TAG + " \n" +
                                curRegions.get(curTableIndex).getTableContent() +
                                " \n" + TABLE_END_TAG + " \n";
                        contentBuilder.append(text);
                    }
                    // if dealing with pure text region, extract text from it.
                    else {
                        stripperByArea.extractRegions(pdDocument.getPage(pageIndex));
                        text = stripperByArea.getTextForRegion(Integer.toString(curTableIndex));
                        contentBuilder.append(text);
                    }
                }
                String[] curContent = contentBuilder.toString().trim()
                        .split(stripperByArea.getWordSeparator());
                StringBuilder tempsb = new StringBuilder();
                List<String> paragraphs = new ArrayList<String>();

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

    /**
     * PDF files may have header & footer which contain repeated or useless info.
     * Using this function to remove header & footer.
     * @param page
     * @return
     */
    private technology.tabula.Page removeHeaderandFooter(technology.tabula.Page page) {
        float pageHeight = (float)page.getHeight();
        float pageWidth = (float)page.getWidth();
        /*
           In my case, 0.955 is the largest gate value to ignore the page footer on tested sample PDF file.
           However, this is not a reliable method to remove header & footer.
           Since the height of header & footer varies from different PDF.
         */
        float bottom = (float)(pageHeight * 0.955);
        float top = (float)(pageHeight * 0.01);
        Rectangle rect = new Rectangle(top, 0, pageWidth, bottom - top);
        technology.tabula.Page pageBody = page.getArea(rect);
        return pageBody;
    }
}