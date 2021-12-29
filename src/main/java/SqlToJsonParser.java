import com.maniaqz.configuration.*;
import com.maniaqz.core.Parser;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;

public class SqlToJsonParser {

    /**
     * Main method, can be used for executable
     *
     * @param args [0] - input file name
     *             [1] - output file name
     */
    public static void main(String[] args) {
        String inputFileName;
        String outputFileName;

        if ((args != null) && (args.length == 2)) {
            inputFileName = args[0];
            outputFileName = args[1];
        } else {
            inputFileName = ParserConfig.INPUT_FILE_NAME;
            outputFileName = ParserConfig.OUTPUT_FILE_NAME;
        }

        SqlToJsonParser sqlToJsonParser = new SqlToJsonParser();
        Parser parser = new Parser();
        try {
            String sqlString = sqlToJsonParser.readSqlFromFile(inputFileName);
            if (sqlString == null)
                throw new Exception(String.format("Input file [%s] doesn't  exist", inputFileName));

            JSONObject json = parser.parse(sqlString);

            if (json == null)
                throw new Exception(String.format("Input file [%s] has wrong SQL syntax", inputFileName));

            String jsonString = json.toString(4); //4 - количество отступов после каждого уровня
            System.out.println("Input SQL:\n\n" + sqlString);
            System.out.println("Output Json:\n\n" + jsonString);

            sqlToJsonParser.saveFile(jsonString, outputFileName);
            System.out.println(String.format("Parse was successful, result was saved to [%s]", outputFileName));
        } catch (Exception e) {
            sqlToJsonParser.saveFile(ParserConfig.WRONG_SQL_SYNTAX, outputFileName);
            System.out.println(e.getMessage());
        }

    }

    /**
     * Reads SQL from file with specified name
     *
     * @param fileName input file name from config, or Main [0] arg
     * @return sql from file (null if file doesn't exist)
     */
    private String readSqlFromFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                sb.append(line.trim()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Saves Json String to specified file
     *
     * @param jsonString parsed Json object
     * @param fileName   output file name from config, or Main [1] arg
     */
    private void saveFile(String jsonString, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(jsonString.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
