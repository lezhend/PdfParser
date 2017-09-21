import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.ContentHandler;

/**
 * Created by zhangshengxin on 9/11/17.
 */
public class TikaTest {
    private static String sourceBucketName = "fir-cninfo-report-bucket";
    private static String targetBucketName = "fir-cninfo-report-processed-bucket";
    private static String accessKey = "AKIAJCLW5DVNKWPPOP4A";
    private static String secretKey = "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";

    public static void main(final String[] args) throws IOException, TikaException {

//        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
//        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
//                .withRegion(Regions.US_WEST_2)
//                .build();
//
//        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(sourceBucketName).withMaxKeys(10);
//        ListObjectsV2Result result;


        try {
            TikaInputStream pdfStream = TikaInputStream.get(new File("/Users/zhangshengxin/Documents/report/44ac30-28b9-44d7-ac87-b29bd2844fdc.pdf"));

            Metadata metadata = new Metadata();
            WriteOutContentHandler handler =  new WriteOutContentHandler();
            ParseContext parseContext = new ParseContext();
            Parser parser = TikaConfig.getDefaultConfig().getParser();
            parser.parse(pdfStream, handler, metadata, parseContext);

            String pdfParagraphCount = metadata.get(TikaCoreProperties.DESCRIPTION);
            System.out.println(pdfParagraphCount);
        } catch (SAXException e) {
            e.printStackTrace();
        }

//        File file = new File("/Users/zhangshengxin/Downloads/00ed0b-5a59-4cfc-bbf9-1016b07b74e2.pdf.txt");
//        FileInputStream in = new FileInputStream(file);
//        Reader reader = new InputStreamReader(in);
//        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
//        StringBuilder sb = new StringBuilder();
//        char[] array = new char[50000];
//        reader.read(array);
//
//        System.out.println(array);

    }


}
