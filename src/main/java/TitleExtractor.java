import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by zhangshengxin on 8/10/17.
 */
public class TitleExtractor {

    static String[] patterns = new String[10];
    static {
        patterns[0] = "第[\u4E00-\u9FA5]+节\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[1] = "[一二三四五六七八九十]+\u3001(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[2] = "\\([一二三四五六七八九]+\\)\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[3] = "[0-9]+\\u3001\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[4] = "\\([0-9]+\\)\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[5] = "[0-9]+\\.\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[6] = "[0-9]+\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[7] = "\\([0-9]\\)\\.\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[8] = "\\([a-z]\\)\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[9] = "[①-⑳]\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";

    }

    public static void main(String[] args) {

        File pdfFile = new File("/Users/zhangshengxin/Documents/国泰君安：2016年半年度报告.PDF");
        PDDocument document = null;

        try
        {
            document = PDDocument.load(pdfFile);
            int pages = document.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setShouldSeparateByBeads(true);
            stripper.setParagraphStart("__&&__");
            stripper.setDropThreshold((float)2);
            stripper.setSortByPosition(true);
            stripper.setStartPage(3);
            stripper.setEndPage(pages);
            String content = stripper.getText(document);

            //remove the header and footer
            List<String> list = removeFooter(content);
            //extract title as a collections
            List<String> titles = extractTitle(list);
            System.out.println(titles);

        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public static List<String> extractTitle(List<String> content) {
        List<String> result = new ArrayList<String>();

        //If fit any pattern, save it
        for(String section: content) {
            boolean isTitle = false;
            for (int i = 0; i < 10; i++) {
                if (Pattern.matches(patterns[i], section)) {
                    isTitle = true; break;
                }
            }
            if (isTitle) result.add(section);
        }
        return result;
    }

    private static List<String> removeFooter(String content) {
        StringTokenizer stringTokenizer = new StringTokenizer(content, "__&&__");
        List<String> list = new ArrayList<String>();

        while(stringTokenizer.hasMoreTokens()) {
            String cur = stringTokenizer.nextToken().trim();
            boolean isMatch = cur.contains("/ 172") || cur.contains("2016 年半年度报告");
            if(isMatch) continue;
            list.add(cur);
        }

        return list;
    }

}
