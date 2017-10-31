import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.List;


/**
 * Created by zhangshengxin on 9/21/17.
 */
public class SampleParse {
    public static void main(String[] args) throws IOException {
        if(args.length != 3 && args.length != 2) {
            System.err.println("please give only input & output & validation gate value.");
            System.err.println("Usage: java -jar <jar_file> <input_file> <output_directory> <gate_value>.");
            System.err.println("A resonable gate value should be from 0 to 1.x, considering we may add some extra character when parsing PDF with tabula.");
            System.err.println("You may leave gate value default to be 0.75.");
            System.exit(3);
        }

        double gateValue = 0.65;
        String inputPath = args[0];
        String outputPath = args[1];

        if(args.length == 3){
            gateValue = Double.parseDouble(args[2]);
            System.out.println("Default validation gate value is 0.75.");
        }

        if(outputPath.charAt(outputPath.length() - 1) != '/')
            outputPath += "/";

        parseFile(inputPath, outputPath, gateValue);
    }

    public static boolean parseFile(String inputPath, String outputPath, double gateValue) throws IOException {
        File file = new File(inputPath);

        PdfReportReader pdfReportReader = new PdfReportReader(file);
        PlainReader plainReader = new PlainReader(file);

        String plainResult = plainReader.extract();
        List<String> resultList = pdfReportReader.extract();
        StringBuilder sb = new StringBuilder();
        for(String line: resultList)
            sb.append(line);
        String tabulResult = sb.toString();

        // Check if tabula result is valid by compare with plain result.
        boolean isvalid = resultIsValid(plainResult, tabulResult, gateValue);

        // Save different result depends on whether result with tabula is valid
        String outputFileName = "";
        String result = "";
        if(!isvalid) {
            // Get file name without extension
            outputFileName = "plain_" + inputPath.substring(inputPath.lastIndexOf("/")+1, inputPath.lastIndexOf("."));
            result = plainResult;
            System.out.println("Tabula fails to tell by PDF file, using plain PDF parser. File name: " + outputFileName + ".txt");
        }
        else {
            // Get file name without extension
            outputFileName = inputPath.substring(inputPath.lastIndexOf("/")+1, inputPath.lastIndexOf("."));
            result = tabulResult;
        }
        File outputFile = new File(outputPath + outputFileName + ".txt");
        if(!outputFile.getParentFile().exists()) {
            System.out.println("creating directory");
            outputFile.getParentFile().mkdirs();
        }

        if (outputFile.createNewFile()){
            System.out.println("File " + outputFile + " is created!");
        }else{
            System.out.println("File already exists, overwrite it.");
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
        out.write(result);
        out.flush();
        out.close();
        return true;
    }

    public static boolean resultIsValid(String plainResult, String result, double gateValue) {
        double len1 = result.length();
        double len2 = plainResult.length();
        double rate = len1/len2;
        System.out.println("the parse rate is: " + rate);
        return rate > gateValue;
    }

}
