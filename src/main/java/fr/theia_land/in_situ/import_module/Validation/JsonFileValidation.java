/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation;

import fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidIdsException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonFileNameException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonInternationalisationException;
import fr.theia_land.in_situ.import_module.Exceptions.JsonFileNotFoundException;
import fr.theia_land.in_situ.import_module.Exceptions.TheiaCategoriesException;
import static fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils.readFile;
import static fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils.removeEmptyStringAndNullField;
import static fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils.removeMultiLangKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author coussotc
 */
@Component
public class JsonFileValidation {

    @Value("${json.schema.url}")
    private String jsonSchemaUrl;

    private static final Logger logger = LoggerFactory.getLogger(JsonFileValidation.class);

    /**
     * Check if there is JSON files in the folder validate the JSON file names
     *
     * @param folderPath the folder containing the JSON file
     * @throws InvalidJsonFileNameException if the name of a JSON file is wrong
     * @throws JsonFileNotFoundException if the folder does not exist
     */
    public static void validateJsonFileNames(String folderPath) throws InvalidJsonFileNameException, JsonFileNotFoundException {

        /**
         * folder = path to the folder where the files to be validated before to be imported are located
         * /data/producerId/tmp/{date}/unzip
         */
        String producerId = new File(new File(new File(new File(folderPath).getParent()).getParent()).getParent()).getName();

        List<File> jsonFiles = ValidationUtils.listFiles(folderPath, "json");
        /**
         * Check for json file presence
         */
        if (jsonFiles.isEmpty()) {
            throw new JsonFileNotFoundException();
        }

        /**
         * check the json file name
         */
        String regexJsonFileName = producerId + "_(en|fr|es)\\.json";
        Pattern jsonFileNamePattern = Pattern.compile(regexJsonFileName);
        for (File jsonFile : jsonFiles) {
            Matcher m = jsonFileNamePattern.matcher(jsonFile.getName());
            if (!m.find()) {
                throw new InvalidJsonFileNameException(jsonFile.getName());
            }
        }
    }

    /**
     * Validate the fields of the JSON file using a JSON schema
     *
     * @param jsonPath aboslute path of the file to validate
     * @throws IOException if tthe JSON file does not exist
     * @throws InvalidJsonException if the format of the JSON file is not in accordance with the JSON schema
     */
    public void validateJsonFileFormat(String jsonPath) throws IOException, InvalidJsonException {
        String urlToRootSchema = jsonSchemaUrl + "root_schema.json";

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String jsonString = readFile(jsonPath, StandardCharsets.UTF_8);

//        JsonFileValidation.validateJsonFileFormat(
//                gson.toJson(removeEmptyStringAndNullField(
//                        gson.fromJson(jsonString, JsonElement.class).getAsJsonObject())));
        String validationOutput = SchemaValidator.validateSchema(gson.toJson(removeEmptyStringAndNullField(
                gson.fromJson(jsonString, JsonElement.class).getAsJsonObject())), urlToRootSchema);
        if (!"success".equals(validationOutput)) {
            logger.error("The file " + Paths.get(jsonPath).getFileName() + " is not valid.\n");
            throw new InvalidJsonException(validationOutput);
        } else {
            logger.debug("The file " + Paths.get(jsonPath).getFileName() + " is valid.\n");
        }
    }

    /**
     * Check that the ids of the json file correspond to the parent folder name as specified in the documentation
     *
     * @param jsonObject JsonObject representation of the json file to be validated
     * @param producerId Stirng the id of the producer
     * @throws IOException
     */
    public static void validateJsonFileIds(JsonObject jsonObject, String producerId) throws IOException {

        String regexdatasetId = producerId + "_DAT_(.*)";
        String regexObservationId = producerId + "_OBS_(.*)";
        Pattern patternProducerId = Pattern.compile(producerId);
        Pattern patterndatasetId = Pattern.compile(regexdatasetId);
        Pattern patternObservationId = Pattern.compile(regexObservationId);
        try {
            Matcher matcherProducer = patternProducerId.matcher(
                    jsonObject.getAsJsonObject("producer").get("producerId").getAsString());
            if (!matcherProducer.find()) {
                throw new InvalidIdsException(jsonObject.getAsJsonObject("producer").get("producerId").getAsString());
            }
            jsonObject.getAsJsonArray("datasets").forEach(dataset -> {
                if (!patterndatasetId.matcher(
                        dataset.getAsJsonObject().get("datasetId").getAsString()).find()) {
                    try {
                        throw new InvalidIdsException(dataset.getAsJsonObject().get("datasetId").getAsString());
                    } catch (InvalidIdsException ex) {
                        logger.error(ex.getMessage());
                        System.exit(1);
                    }
                }
                dataset.getAsJsonObject().getAsJsonArray("observations").forEach(obs -> {
                    if (!patternObservationId.matcher(obs.getAsJsonObject().get("observationId").getAsString()).find()) {
                        try {
                            throw new InvalidIdsException(obs.getAsJsonObject().get("observationId").getAsString());
                        } catch (InvalidIdsException ex) {
                            logger.error(ex.getMessage());
                            System.exit(1);
                        }
                    }

                });
            }
            );
        } catch (InvalidIdsException ex) {
            logger.error(ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Check that key/value pair between internationalised json files are coherent
     *
     * @param jsonFiles Internationalised josn files
     * @throws IOException
     * @throws InvalidJsonInternationalisationException
     */
    public void validateJsonInternationalisation(List<File> jsonFiles) throws IOException, InvalidJsonInternationalisationException {
        /**
         * Load the json config file
         */
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        InputStream in = new URL(jsonSchemaUrl+"configInternationalisation.json").openStream();
        byte[] encoded = IOUtils.toByteArray(in);
        JsonObject configJson = gson.fromJson(new String(encoded, StandardCharsets.UTF_8), JsonElement.class).getAsJsonObject();

        List<JsonElement> jsonObjectList = new ArrayList<>();
        for (File j : jsonFiles) {
            jsonObjectList.add(gson.fromJson(
                    ValidationUtils.readFile(j.getAbsolutePath(), StandardCharsets.UTF_8), JsonElement.class).getAsJsonObject());
        }
        /**
         * Remove all key/value pair that is subject to internationalisation
         */
        for (JsonElement j : jsonObjectList) {
            j = removeMultiLangKey(j, configJson);
        }

        /**
         * We check if the json file are equals after the removal of the fields subject to internationalisation
         */
        for (int i = 1; i < jsonObjectList.size(); i++) {
            Object diff = ValidationUtils.jsonsEqual(jsonObjectList.get(i - 1), jsonObjectList.get(i));
            if (diff != null && !diff.toString().equalsIgnoreCase("{}")) {
                if (diff instanceof JsonObject) {
                    throw new InvalidJsonInternationalisationException(gson.toJson(diff),
                            jsonFiles.get(i - 1).getName(), jsonFiles.get(i).getName());
                } else {
                    throw new InvalidJsonInternationalisationException(diff.toString(), jsonFiles.get(i - 1).getName(), jsonFiles.get(i).getName());
                }
            }
        }
    }

    public static void validateTheiaCategories(JsonArray categories, List<String> categoryLeafs) throws TheiaCategoriesException {
        for (JsonElement je : categories) {
            String uri = je.getAsString();
            if (!categoryLeafs.contains(uri)) {
                throw new TheiaCategoriesException(uri);
            }
        }
    }
}
