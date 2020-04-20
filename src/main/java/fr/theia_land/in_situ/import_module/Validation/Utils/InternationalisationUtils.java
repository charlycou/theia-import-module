/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.model.POJO.ImportConfig;
import static fr.theia_land.in_situ.import_module.DAO.ObservationDocumentsCreation.listJsonFiles;
import static fr.theia_land.in_situ.import_module.DAO.ObservationDocumentsCreation.readJsonFile;
import fr.theia_land.in_situ.import_module.Exceptions.ScanRMetadataNotFoundException;
import static fr.theia_land.in_situ.import_module.Validation.Utils.EnrichmentUtils.setFundingsUsingScanR;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author coussotc
 */
@Component
public class InternationalisationUtils {

    @Value("${json.schema.url}")
    private String jsonSchemaUrl;

    /**
     * recursive call browsing the json in order to update fields that are subject to internationalisation pattern
     *
     * @param jsonToUpdate the json document to update
     * @param jsonInAnotherlang the json used to update the json to update (i.e. json whith fields in another language)
     * @param configJson the json configuration file that describing the fields subject to internationalisation pattern
     * @param fieldName the name of the field to update
     * @param language the langaguge of the fields in ISO
     * @param importConfig Import config object
     * @return a JsonElement instance of JsonObject witht the multi language fields upadated
     */
    public static JsonElement updateMultiLangFields(JsonElement jsonToUpdate, JsonElement jsonInAnotherlang,
            JsonObject configJson, String fieldName, String language, ImportConfig importConfig) {

        String type = null;

        type = configJson.get("type").getAsString();

        switch (type) {
            case "object":

                JsonObject properties = configJson.getAsJsonObject("properties");
                for (String key : properties.keySet()) {
                    if (jsonInAnotherlang.getAsJsonObject().has(key)) {
                        JsonElement tmpObject = jsonToUpdate.getAsJsonObject().get(key);
                        tmpObject = updateMultiLangFields(jsonToUpdate.getAsJsonObject().get(key),
                                jsonInAnotherlang.getAsJsonObject().get(key),
                                configJson.getAsJsonObject("properties").getAsJsonObject(key),
                                key, language, importConfig);
                        jsonToUpdate.getAsJsonObject().remove(key);
                        jsonToUpdate.getAsJsonObject().add(key, tmpObject);
                    }
                }
                break;

            case "array":
                for (int i = 0; i < jsonInAnotherlang.getAsJsonArray().size(); i++) {
                    JsonElement tmpArray = jsonToUpdate.getAsJsonArray().get(i);
                    tmpArray = updateMultiLangFields(jsonToUpdate.getAsJsonArray().get(i),
                            jsonInAnotherlang.getAsJsonArray().get(i),
                            configJson.getAsJsonObject("items"), null, language, importConfig);
                    jsonToUpdate.getAsJsonArray().set(i, tmpArray);

                }
                break;

            case "string":
                jsonToUpdate = updateOneMultiLangField(jsonToUpdate, jsonInAnotherlang.getAsString(),
                        language, importConfig);

        }
        return jsonToUpdate;
    }

    private static JsonArray updateOneMultiLangField(JsonElement fieldToUpdate, String valueToBeAdded, String lang,
            ImportConfig importConfig) {

        /**
         * The JsonObject to be added for internationalisation
         */
        JsonObject objectToBeAdded = new JsonObject();
        objectToBeAdded.addProperty("lang", lang);
        objectToBeAdded.addProperty("text", valueToBeAdded);

        /**
         * If the field to update is already an JsonArray, it has already been updated with a lang fields
         */
        if (fieldToUpdate.isJsonArray()) {
            JsonArray fieldToUpdateArray = fieldToUpdate.getAsJsonArray();

            /**
             * if the field for the language already exist in the array (should not occur) nothing happens
             */
            try {
                fieldToUpdateArray.forEach(item -> {
                    JsonObject multiLangObject = item.getAsJsonObject();
                    if (multiLangObject.get("lang").getAsString().equals(lang)) {
                        throw new IllegalStateException();
                    }
                });
            } catch (IllegalStateException ex) {
                return fieldToUpdateArray;
            }
            /**
             * One field for another language already exist, the object is updated to add a new fields Fisrt the objects
             * already present are checked to be coherent with ImportConfig object
             */
            fieldToUpdateArray.forEach(item -> {
                JsonObject multiLangObject = item.getAsJsonObject();
                if (!Arrays.asList(importConfig.getLanguages()).stream().anyMatch(t -> {
                    return multiLangObject.get("lang").getAsString().equals(t);
                })) {
                    throw new IllegalStateException("Wrong language object found in a internaionalised array according to "
                            + "the ImportConfig object");
                }
            });
            /**
             * Adding the jsonObject to be added to the Array of internationalised element
             */
            fieldToUpdateArray.add(objectToBeAdded);
            return fieldToUpdateArray;

            /**
             * if the filed to update is a String. The Json Array needs to be created
             */
        } else if (fieldToUpdate.isJsonPrimitive()) {
            JsonArray fieldToUpdateArray = new JsonArray();
            fieldToUpdateArray.add(objectToBeAdded);
            return fieldToUpdateArray;
        } else {
            throw new IllegalStateException("The field to update for internationalisation is neither an JsonArray nor a String object.");
        }
    }

    /**
     *
     * @param folderPath String - absolute path of the folder containing the json files in different language
     * @param importConfig ImportConfig - ImportConfig object describing the method of import
     * @return JsonObject - with the internationlaisationpattern of all the file in different language present in the
     * folder
     * @throws IOException - if the url of the json configuration file is not found
     * @throws fr.theia_land.in_situ.import_module.Exceptions.ScanRMetadataNotFoundException
     */
    public JsonObject setInternationalisation(String folderPath, ImportConfig importConfig) throws IOException, ScanRMetadataNotFoundException {
        /**
         * list the previously validated json files of the folder.
         */
        List<File> jsonFiles = listJsonFiles(folderPath);

        /**
         * Create a new jsonObject for multiLang pattern
         */
        JsonElement internationalisedJson = new JsonObject();
//        JsonElement tmp = new JsonObject();

        /**
         * Load the json config file
         */
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        InputStream in = new URL(jsonSchemaUrl + "configInternationalisation.json").openStream();
        byte[] encoded = IOUtils.toByteArray(in);
        JsonObject configJson = gson.fromJson(new String(encoded, StandardCharsets.UTF_8), JsonElement.class
        ).getAsJsonObject();

        for (int i = 0; i < importConfig.getLanguages().length; i++) {

            /**
             * Read one json file sent by the producer to create a JsonObject
             */
            String lang = importConfig.getLanguages()[i];
            /**
             * folder = path to the folder where the files to be validated before to be imported are located
             * /data/producerId/tmp/{date}/unzip
             */
            String producerId = new File(new File(new File(new File(folderPath).getParent()).getParent()).getParent()).getName();
            String regex = producerId + "_" + lang + "\\.json";
            List<File> jsonListTmp = jsonFiles.stream().filter((File p) -> {
                Pattern jsonFileNamePattern = Pattern.compile(regex);
                Matcher m = jsonFileNamePattern.matcher(p.getName());
                return m.find();
            }).collect(Collectors.toList());
            if (jsonListTmp.size() != 1) {
                throw new IllegalStateException();
            }
            JsonObject json = readJsonFile(jsonListTmp.get(0));

            /**
             * Prefix producer name using OZCAR-RI
             */
            String[] producerIds = {"AGRH", "AURA", "BVET", "CATC", "CRYO", "DRAI", "ERUN", "HPLU", "HYBA", "KARS", "MSEC", "OBSE", "OHGE", "OHMC", "OMER", "OPEA", "ORAC", "OSRC", "REAL", "TOUR", "YZER"};
            if (Arrays.stream(producerIds).anyMatch(json.getAsJsonObject("producer").get("producerId").getAsString()::equals)) {
                //+ jsonObject.getAsJsonObject("producer").get("name").getAsString()
                json.getAsJsonObject("producer").addProperty("name", "OZCAR-RI " + json.getAsJsonObject("producer").get("name").getAsString());
            }
            /**
             * Populate fundings using scanR API
             */
            JsonArray fundings = setFundingsUsingScanR(json.getAsJsonObject("producer").getAsJsonArray("fundings"));
            JsonObject producer = json.getAsJsonObject("producer");
            producer.remove("fundings");
            producer.add("fundings", fundings);

            /**
             * Populate contact.organisation using scanR API
             */
            producer.add("contacts", EnrichmentUtils.setContactsUsingScanR(producer.getAsJsonArray("contacts")));

            /**
             * Update producer metdata with newly enricghed metadata
             */
            json.remove("producer");
            json.add("producer", producer);

            /**
             * Populate dataset contacts using scanR API
             */
            JsonArray datasets = json.getAsJsonArray("datasets");
            for (int j = 0; j < datasets.size(); j++) {
                datasets.get(j).getAsJsonObject().getAsJsonObject("metadata")
                        .add("contacts", EnrichmentUtils.setContactsUsingScanR(datasets.get(j).getAsJsonObject().getAsJsonObject("metadata").getAsJsonArray("contacts")));
            }

            /**
             * Initialisation of the first internationalised observation document is created using the first json object
             */
            if (i == 0) {
                internationalisedJson = json.deepCopy();
            }

            internationalisedJson = updateMultiLangFields(internationalisedJson, json, configJson, null, lang,
                    importConfig);

        }

        return internationalisedJson.getAsJsonObject();
    }
}
