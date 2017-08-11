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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PdfReader  {

    public static void main(String[] args){
        String filePath = "/home/yibo/Desktop/pdf/half.PDF";
        File pdfFile = new File(filePath);
        //PDDocument document;
        //PDDocument document = PDDocument.load(pdfFile);
        tableFilter(pdfFile, 15);

    }

    public static void tableFilter(File pdfFile, int pageNumber){
        try
        {
            PDDocument document = PDDocument.load(pdfFile);
            int pageNumberForPDFbox = pageNumber - 1;

            // Extract tables by Tabula
            ObjectExtractor extractor = new ObjectExtractor(document);
            technology.tabula.Page page = extractor.extract(pageNumber);
            DetectionAlgorithm detector = new NurminenDetectionAlgorithm();
            List<Rectangle> guesses = detector.detect(page);
            List<Table> tables = new ArrayList<Table>();
            BasicExtractionAlgorithm basicExtractor = new BasicExtractionAlgorithm();
            for (Rectangle guessRect : guesses) {
                Page guess = page.getArea(guessRect);
                tables.addAll(basicExtractor.extract(guess));
            }
            double currentHeight = 0.0;
            double pageHeight = page.getHeight();
            double pageWidth = page.getWidth();
            double tableHeight = 0.0;
            int tableIndex = 1;
            for(Table tb : tables){
                double minHeightOfTable = tb.getCell(0,0).getMinY();
                double maxHeightOfTable = tb.getCell(tb.getRows().size()-1,tb.getCols().size()-1).getMaxY();
                // Find the minimum height of table by visiting all columns of the first row
                for(int j = 0; j < tb.getCols().size(); j++){
                    if (tb.getCell(0,j).getMinY() == 0.0)
                        continue;
                    if (minHeightOfTable < tb.getCell(0,j).getMinY())
                        minHeightOfTable = tb.getCell(0,j).getMinY();
                }
                tableHeight = minHeightOfTable - currentHeight - 15;  // Set bottom buffer as 15
                Rectangle2D rect = new Rectangle2D.Double(0,currentHeight,pageWidth,tableHeight);
                getTextFromArea(pdfFile, pageNumberForPDFbox, tableIndex, rect);
                currentHeight = maxHeightOfTable + 20 ;  // Set upper buffer as 20
                tableIndex += 1;

                if(tableIndex == (tables.size() + 1))
                {
                    tableHeight = pageHeight - currentHeight;
                    rect = new Rectangle2D.Double(0,currentHeight,pageWidth,tableHeight);
                    getTextFromArea(pdfFile, pageNumberForPDFbox, tableIndex, rect);
                }

            }
        }
        catch(Exception e)
        {
            StackTraceElement l = e.getStackTrace()[e.getStackTrace().length-1];
            System.out.println(
                    l.getClassName()+"/"+l.getMethodName()+":"+l.getLineNumber());
            System.out.println(e);

        }
    }

    public static void getTextFromArea(File pdfFile, int pageNumberForPDFbox, int tableIndex, Rectangle2D rect) {
        PDFTextStripperByArea stripperByArea = null;
        try {
            stripperByArea = new PDFTextStripperByArea();
            stripperByArea.setSortByPosition(true);
            stripperByArea.addRegion(Integer.toString(tableIndex), rect);
            PDDocument document = PDDocument.load(pdfFile);
            stripperByArea.extractRegions(document.getPage(pageNumberForPDFbox));
            String contentsOfAllPage = stripperByArea.getTextForRegion(Integer.toString(tableIndex));
            String contents[] = contentsOfAllPage.trim().split(stripperByArea.getWordSeparator());
            int index = 0;
            String output = "";
            List<String> paragraphs = new ArrayList<String>();
            for (String line : contents){
                index += 1;
                boolean paragraphFlag = line.startsWith("\n");
                if(paragraphFlag == false){
                    output += line.replaceAll("\n", "");
                }
                else {
                    paragraphs.add(output);
                    output = "";
                    output += line.replaceAll("\n", "");
                }
            }
            for(String paragraph : paragraphs){
                System.out.println(paragraph);
                System.out.println("\n");
            }
        } catch (IOException e) {
            StackTraceElement l = e.getStackTrace()[e.getStackTrace().length-1];
            System.out.println(
                    l.getClassName()+"/"+l.getMethodName()+":"+l.getLineNumber());
            System.out.println(e);
        }

    }
}