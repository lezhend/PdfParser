import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.List;


/**
 * @author ShengxinZhang
 * @date 2017/09/21
 */
public class SampleParse {
    /**
     This function could only be used in Linux environment.
     */
    public static void main(String[] args) throws IOException {
        final int lenOfParamFull = 3;
        final int lenOfParamNoGateValue = 2;
        final char charSlash = '/';
        if (args.length != lenOfParamFull &&
                args.length != lenOfParamNoGateValue) {
            System.err.println("please give only input & output & " +
                    "validation gate value.");
            System.err.println("Usage: java -jar <jar_file> <input_file> " +
                    "<output_directory> <gate_value>.");
            System.err.println("A resonable gate value should be from 0" +
                    " to 1.x, considering we may add some extra character " +
                    "when parsing PDF with tabula.");
            System.err.println("You may leave gate value default to be 0.75.");
            System.exit(1);
        }

        double gateValue = 0.75;
        String inputPath = args[0];
        String outputPath = args[1];

        if (args.length == lenOfParamFull){
            gateValue = Double.parseDouble(args[2]);
            System.out.println("Default validation gate value is 0.75.");
        }

        // if the path Parameter does not end with '/', add '/' at the end of it
        if (outputPath.charAt(outputPath.length() - 1) != charSlash) {
            outputPath += "/";
        }

        parseFile(inputPath, outputPath, gateValue);
    }

    /**
     * 1. Parse the pdf file with 2 methods
     * 2. Get the more accruate result from the tableExtract result and plain one
     *
     * @param inputPath
     * @param outputPath
     * @param gateValue
     * @return
     * @throws IOException
     */
    public static boolean parseFile(String inputPath, String outputPath,
                                    double gateValue) throws IOException {
        File file = new File(inputPath);

        // Initial 2 pdf converters
        // PdfReportReader has tabula functions and PlainReader has not.
        PdfReportReader pdfReportReader = new PdfReportReader(file);
        PlainReader plainReader = new PlainReader(file);
        List<String> tabulaResultList = pdfReportReader.extractResult();
        String plainResult = plainReader.extract();

        // Init the output varibles
        String tabulaResult = null;
        String outputFileName = "";
        String outputResult = "";

        StringBuilder sb = new StringBuilder();
        for (String line: tabulaResultList) {
            sb.append(line);
        }
        tabulaResult = sb.toString();

        /**
         * Check if tabula result is valid by compare with plain result.
         * Definition: Vaild result means
         * the str length of plain result / the str length of pdfReportReader
         * is higher than gate value.
         */
        boolean isValid = resultIsValid(plainResult, tabulaResult, gateValue);

        // Save different result depends on whether result with tabula is valid
        if (!isValid) {
            // Get file name to filter the .pdf extension
            outputFileName = "plain_" + inputPath.substring(
                    inputPath.lastIndexOf("/") + 1,
                    inputPath.lastIndexOf("."));
            outputResult = plainResult;
            System.out.println("Tabula fails to detect tables, " +
                    "using plain PDF parser. File name: "
                    + outputFileName + ".txt");
        }
        else {
            // Get file name to filter the .pdf extension
            outputFileName = inputPath.substring(
                    inputPath.lastIndexOf("/") + 1,
                    inputPath.lastIndexOf("."));
            outputResult = tabulaResult;
        }

        File outputFile = new File(outputPath + outputFileName + ".txt");
        if (!outputFile.getParentFile().exists()) {
            System.out.println("creating directory");
            outputFile.getParentFile().mkdirs();
        }
        if (outputFile.createNewFile()) {
            System.out.println("File " + outputFile + " is created!");
        }
        else {
            System.out.println("File already exists, overwrite it.");
        }
        // Write the outputResult into the file
        BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
        out.write(outputResult);
        out.flush();
        out.close();
        return true;
    }

    /**
     * Check whether the result of tableExtraction is valid1
     *
     * @param plainResult
     * @param tabulaResult
     * @param gateValue
     * @return
     */
    public static boolean resultIsValid(String plainResult, String tabulaResult,
                                        double gateValue) {
        double lenOftabulaResult = tabulaResult.length();
        double lenOfplainResult = plainResult.length();
        double rate = lenOftabulaResult/lenOfplainResult;
        System.out.println("the parse rate is: " + rate);
        return rate > gateValue;
    }

}
