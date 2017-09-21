import java.io.File;
import java.io.IOException;


import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;


import javax.imageio.ImageIO;

/**
 * Created by zhangshengxin on 8/14/17.
 */
public class ImageExtractor {

    public static void extractImages(PDDocument pdfFile) {
        try {
            pdfFile = PDDocument.load(new File("/Users/zhangshengxin/Downloads/test.pdf"));

            PDPageTree list = pdfFile.getPages();
            for (PDPage page : list) {
                PDResources pdResources = page.getResources();

                for (COSName c : pdResources.getXObjectNames()) {
                    System.out.println(c.getName());
                    PDXObject o = pdResources.getXObject(c);
                    if (o instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        File file = new File("/Users/zhangshengxin/Desktop/extractedImage/" + System.nanoTime() + ".JPEG");
                        ImageIO.write(((PDImageXObject)o).getImage(), "JPEG", file);
                    }
                }break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
