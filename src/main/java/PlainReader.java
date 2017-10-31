import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/**
 * Created by zhangshengxin on 10/22/17.
 */
public class PlainReader {
    PDDocument pdDocument = null;

    public PlainReader(File file) throws IOException {
        this.pdDocument = PDDocument.load(file);
    }

    public String extract() throws IOException {
        String result = "";
        PDFTextStripper pdfTextStripper= new PDFTextStripper();
        pdfTextStripper.setSortByPosition(true);
        pdfTextStripper.setWordSeparator(" ");
        result = pdfTextStripper.getText(this.pdDocument);
        return result;
    }
}
