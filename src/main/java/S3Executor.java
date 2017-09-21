import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by zhangshengxin on 8/28/17.
 */
public class S3Executor {

    // give the source and target s3 bucket here
    private static final String sourceBucketName = "fir-analyst-report";
    private static final String targetBucketName = "fir-analyst-report-parsed-update";
    private static final String accessKey = "AKIAJCLW5DVNKWPPOP4A";
    private static final String secretKey = "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";

    public static void main(String[] args) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.US_WEST_2)
                .build();

        try {
            final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(sourceBucketName).withMaxKeys(10);
            ListObjectsV2Result result;

            do {
                result = s3client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    long size = objectSummary.getSize();

                    // Name of the source file and result file
                    String sourcekey = objectSummary.getKey();
                    String targetKey = sourcekey.substring(0, sourcekey.indexOf(".")) + ".txt";

                    // May contain other format file, pass them
                    if(!sourcekey.endsWith(".pdf"))
                        continue;

                    System.out.println("Now dealing with: ");
                    System.out.println(" - " + sourcekey + "  " + "(size = " + size + ")");


                    // Since tabula will automatically close the inputstream after judge the table area, we need two clone
                    // of pdf file: one for tabula, one for PDFBox.
                    S3Object s3objectforTabula = s3client.getObject(new GetObjectRequest(sourceBucketName, sourcekey));
                    S3Object s3objectforPdfBox = s3client.getObject(new GetObjectRequest(sourceBucketName, sourcekey));

                    InputStream objectDataforTabula = s3objectforTabula.getObjectContent();
                    InputStream objectDataforPdfBox = s3objectforPdfBox.getObjectContent();
                    try {
                        //do something here, get parsed content from PDF file
                        PDDocument pdfFileforPdfBox = PDDocument.load(objectDataforTabula);
                        PDDocument pdfFileforTabula = PDDocument.load(objectDataforPdfBox);
                        PdfReportReader pdfReportReader = new PdfReportReader(pdfFileforTabula, pdfFileforPdfBox);

                        // If return null, then we know its page number is not 2,
                        // Currently we only need tow pages file
                        List<String> parsedData = pdfReportReader.extract();
                        if(parsedData == null) {
                            pdfFileforTabula.close();
                            pdfFileforPdfBox.close();
                            objectDataforTabula.close();
                            objectDataforPdfBox.close();
                            continue;
                        }

                        StringBuilder sb = new StringBuilder();
                        for(String str: parsedData) sb.append(str + "\n");
                        String content = sb.toString().trim();

                        byte[] bytesContent = content.getBytes("UTF-8");
                        InputStream parsedDataStream = new ByteArrayInputStream(bytesContent);

                        //get final length for inputstream, which is necessary for put stream object onto S3
                        long contentLength = Long.valueOf(bytesContent.length);
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(contentLength);

                        System.out.println("writing file: " + targetKey);
                        s3client.putObject(new PutObjectRequest(targetBucketName, targetKey, parsedDataStream, metadata));

                        // close all the file access
                        pdfFileforTabula.close();
                        pdfFileforPdfBox.close();
                        parsedDataStream.close();
                        objectDataforTabula.close();
                        objectDataforPdfBox.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        continue;
                    }

                }
                System.out.println("Next Continuation Token : " + result.getNextContinuationToken());
                req.setContinuationToken(result.getNextContinuationToken());
            } while(result.isTruncated() == true );

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}
