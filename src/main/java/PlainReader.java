import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/**
 * @author ShengxinZhang
 * @data 2017/08/30
 */
public class PlainReader {
    PDDocument pdDocument = null;

    /**
     * Initial to load PDF file.
     * @param file
     * @throws IOException
     */

    public PlainReader(File file) throws IOException {
        this.pdDocument = PDDocument.load(file);
    }

    /**
     * Using naive textStripper in PdfBox to extract text.
     * Regardless of table.
     * @return a string stands for text in whole PDF file.
     * @throws IOException
     */
    public String extract() throws IOException {
        String result = "";
        PDFTextStripper pdfTextStripper= new PDFTextStripper();
        pdfTextStripper.setSortByPosition(true);
        pdfTextStripper.setWordSeparator(" ");
        result = pdfTextStripper.getText(this.pdDocument);
        return result;
    }
}
