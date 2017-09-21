import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by zhangshengxin on 9/7/17.
 */
public class DataStructuralization {
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


}
