/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidTextFileFormatException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidTextFileNameException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidZipException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidZipFileNameException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author coussotc
 */
public class DataFileValidation {

    private static final Logger logger = LogManager.getLogger(DataFileValidation.class);

    /**
     * Validate the format of text file zip entries using REGEX
     *
     * @param zipFile The Zip file containing the text file
     * @param txtFile the text file containing the data
     * @param insr
     * @throws fr.theia_land.in_situ.import_module.Exceptions.InvalidTextFileFormatException if the validation failed
     */
    public static void validateTextFileFormat(ZipFile zipFile, ZipEntry txtFile, InputStreamReader insr) throws InvalidTextFileFormatException, IOException {

        String[] regexFirstLines = {
            "#Date_of_extraction;[0-9]{4}-(1|0)[0-9]{1}-[0-3]{1}[0-9]{1}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z;$",
            "^#Observation_ID;([^;]+;)$",
            "^#Dataset_title;([^;]+;)$",
            "^#Variable_name;([^;]+;)$",
            "date_begin;date_end;latitude;longitude;altitude;value;qualityFlags;((.*)?;)*"
        };

        String regexDataLine = "([0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))T(20|21|22|23|[01]\\d|\\d)((:[0-5]\\d){1,2})((:[0-5]\\d){1,2})Z)?;"
                + "[0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))T(20|21|22|23|[01]\\d|\\d)((:[0-5]\\d){1,2})((:[0-5]\\d){1,2})Z;"
                + "[-]?(([1-8]?[0-9])(\\.[0-9]{1,6})?|90(\\.[0-9]{1,6})?);"
                + "[-]?((([1-9]?[0-9]|1[0-7][0-9])(\\.[0-9]{1,6})?)|180(\\.[0-9]{1,6})?);"
                + "([-]?[0-9]{1,4}(\\.[0-9]{1,6})?)?;"
                + "((.*)?;){2,}";
        try (
                InputStream in = zipFile.getInputStream(txtFile);
                //InputStreamReader insr = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String line = null;
//            if (!insr.getEncoding().equals("UTF8")) {
//                throw new InvalidEncodingException(txtFile.getName(), zipFile.getName(), insr.getEncoding());
//            }
            int i;
            for (i = 0; i < 5; i++) {

                line = reader.readLine();
                Pattern firstLinesPattern = Pattern.compile(regexFirstLines[i]);
                Matcher m = firstLinesPattern.matcher(line);
                if (!m.find()) {
                    throw new InvalidTextFileFormatException(line, i, txtFile.getName(), zipFile.getName());
                }
            }

            Pattern dataLinesPattern = Pattern.compile(regexDataLine);
            Matcher m;

            while ((line = reader.readLine()) != null) {
                i++;
                m = dataLinesPattern.matcher(line);
                if (!m.find()) {
                    throw new InvalidTextFileFormatException(line, i, txtFile.getName(), zipFile.getName());
                }
            }
            logger.info(txtFile.getName() + " in ZIP " + zipFile.getName() + " is valid.");
            in.close();
            reader.close();
        }
    }

    /**
     * Validate the zip file names according to the dataset names found in a valid JSON file. The text file entries of
     * the ZIP files are then validated using DataFileValidation.validateTextFileFormat method
     *
     * @param json a valid JSON object
     * @param zipFiles the list of ZIP files (data files)
     * @return true if all the zip files and there entries are validated
     * @throws IOException if the ZIP file is not in the correct encoding
     */
    public static Map validateZipFiles(JsonObject json, List<File> zipFiles)
            throws IOException {

        Map<String, Boolean> obsLinkToData = new LinkedHashMap<>();
        String observationId = null;
        String datafileName = null;

        JsonArray datasets = json.getAsJsonArray("datasets");
        /**
         * Verify that the zip file name is the same than in the JSON file for all dataset
         */
        for (int i = 0; i < datasets.size(); i++) {

            JsonObject dataset = datasets.get(i).getAsJsonObject();
            File currentZip;
            try {
                try {
                    currentZip = zipFiles.stream().filter(
                            zip -> (FilenameUtils.getBaseName(
                                    zip.getName()) == null ? dataset.get("datasetId").getAsString() == null
                            : FilenameUtils.getBaseName(zip.getName()).equals(dataset.get("datasetId").getAsString()))).findFirst().get();
                } catch (NoSuchElementException ex) {
                    throw new InvalidZipFileNameException(dataset.get("datasetId").getAsString() + ".zip");
                }
                /**
                 * Since the zip file name is valid we verify that the text files inside are also valid
                 */
                ZipFile zipFile = null;

                try {
                    zipFile = new ZipFile(currentZip);

                } catch (ZipException ex) {
                    throw new InvalidZipException(currentZip.getName());
                }

                JsonArray observations = dataset.getAsJsonArray("observations");

                for (int j = 0; j < observations.size(); j++) {
                    try {

                        JsonObject observation = observations.get(j).getAsJsonObject();
                        observationId = observation.get("observationId").getAsString();
                        datafileName = observation.getAsJsonObject("result").getAsJsonObject("dataFile").get("name").getAsString();
                        /**
                         * Verfify that the observationId corresponds to a textFile in the zip of the dataset
                         */
                        ZipEntry txtFile = zipFile.getEntry(observationId + ".txt");

                        if (txtFile != null && txtFile.getName().equals(datafileName)) {
                            /**
                             * Since the text file name is valid we verify the format of the text file
                             */
                            try (InputStreamReader insr = new InputStreamReader(zipFile.getInputStream(txtFile))) {
                                DataFileValidation.validateTextFileFormat(zipFile, txtFile, insr);
                                obsLinkToData.put(observationId, Boolean.TRUE);

                                insr.close();
                            } catch (InvalidTextFileFormatException ex2) {
                                String[] logTrace = (ex2.getMessage() + "The following lines have not been checked.\n").split("\n");
                                for (String logLine : logTrace) {
                                    logger.warn(logLine);
                                }
                                obsLinkToData.put(observationId, Boolean.FALSE);
                            }

                        } else {
                            throw new InvalidTextFileNameException(zipFile.getName(), observation.get("observationId").getAsString() + ".txt");
                        }

                    } catch (InvalidTextFileNameException ex) {
                        String[] logTrace = ex.getMessage().split("\n");
                        for (String logLine : logTrace) {
                            logger.warn(logLine);
                        }
                        obsLinkToData.put(observationId, Boolean.FALSE);

                    }
                }

                zipFile.close();
                //ZipFile test = new ZipFile(zipFiles.get(0));

            } catch (InvalidZipException | InvalidZipFileNameException ex1) {
                String[] logTrace = ex1.getMessage().split("\n");
                for (String logLine : logTrace) {
                    logger.warn(logLine);
                }
                dataset.getAsJsonArray("observations").forEach(item -> {
                    JsonObject obs = (JsonObject) item;
                    obsLinkToData.put(obs.get("observationId").getAsString(), Boolean.FALSE);
                });
            }
        }
        return obsLinkToData;
    }
}
