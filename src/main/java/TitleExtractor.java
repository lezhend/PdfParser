import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by zhangshengxin on 8/10/17.
 */
public class TitleExtractor {

    static String[] patterns = new String[10];
    static String[] examples = new String[10];
    static {
        patterns[0] = "第[\u4E00-\u9FA5]+节\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[1] = "[一二三四五六七八九十]+\u3001(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）\\(\\)]+";
        patterns[2] = "\\([一二三四五六七八九]+\\)(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）\\(\\)]+";
        patterns[3] = "\\（[一二三四五六七八九]+\\）(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[4] = "[0-9]+\\u3001(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[5] = "\\（[0-9]+\\（(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[6] = "[0-9]+\\.(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[7] = "[0-9]+\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[8] = "\\([0-9]\\)\\.(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
        patterns[9] = "\\([a-z]\\)(\\s+)?[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";
//        patterns[10] = "[①-⑳]\\s+[\u4E00-\u9FA5\u3001\\“\\”\\（\\）]+";

        examples[0] = "第五节 公司基本情况、“”（）";
        examples[1] = "五、公司基本情况现状、“”（）";
        examples[2] = "(五)公司基本情况、“”（）";
        examples[3] = "(五) 公司基本情况、“”（）";
        examples[4] = "5、公司基本情况、“”（）";
        examples[5] = "(5) 公司基本情况、“”（）";
        examples[6] = "5. 公司基本情况、“”（）";
        examples[7] = "5 公司基本情况、“”（）";
        examples[8] = "(5). 公司基本情况、“”（）";
        examples[9] = "(e) 公司基本情况、“”（）";

    }

    public static void main(String[] args) throws IOException {
        File file = new File("/Users/zhangshengxin/Downloads/2015年年度报告.PDF");
        PDDocument pdDocument = PDDocument.load(file);
        extractTitles(pdDocument);

    }

    public static List<List<String>> extractTitles(PDDocument pdfFile) {

        List<List<String>> titles = null;
        try {
            int pages = pdfFile.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setParagraphStart("__&&__");
            stripper.setDropThreshold((float) 0.1);
            stripper.setSortByPosition(true);
            stripper.setStartPage(16);
            stripper.setEndPage(17);
            String content = stripper.getText(pdfFile);

            //remove the header and footer
            List<String> list = removeFooter(content);
            //extract title as a collections
            titles = extractTitle(list);
            //test print
            for (List<String> temp : titles) System.out.println(temp);

        } catch (Exception e) {
            System.out.println(e);
        }

        return titles;
    }

    public static List<List<String>> extractTitle(List<String> content) {
        List<List<String>> result = new ArrayList();
        for(int i = 0; i < patterns.length; i++) {
            result.add(new ArrayList<String>());
        }

        //If fit any pattern, save it
        for(String section: content) {
            System.out.println(section);
            for (int i = 0; i < patterns.length; i++) {
                if (Pattern.matches(patterns[i], section)) {
                    result.get(i).add(section);
                    break;
                }
            }
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
