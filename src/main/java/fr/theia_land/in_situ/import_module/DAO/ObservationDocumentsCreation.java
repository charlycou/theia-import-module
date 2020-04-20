/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.DAO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils;
import fr.theia_land.in_situ.import_module.model.POJO.ImportConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 *
 * @author coussotc
 */
public class ObservationDocumentsCreation {

    private static final Logger logger = LogManager.getLogger(ObservationDocumentsCreation.class);

    /**
     * list all json files of a folder
     *
     * @param folder path of the folder containing the json files
     * @return the list of the json files of the folder
     */
    public static ArrayList<File> listJsonFiles(String folder) {
        File[] files = new File(folder).listFiles();
        ArrayList<File> jsonFiles = new ArrayList<>();
        for (File f : files) {
            if (FilenameUtils.getExtension(f.getName()).equals("json")) {
                jsonFiles.add(f);
            }
        }
        return jsonFiles;
    }

    /**
     * Read a json File and return the parsed json object
     *
     * @param file File a json file
     * @return JsonObject a json object
     */
    public static JsonObject readJsonFile(File file) throws IOException {
        JsonObject json = new JsonObject();
        String jsonString = ValidationUtils.readFile(file.getPath(), StandardCharsets.UTF_8);
        Gson gson = new Gson();
        json = gson.fromJson(jsonString, JsonObject.class);
        return json;
    }

    /**
     * Creation of the observation document to be insterted into MongoDB. The information a denormalised in order to
     * have all the information in one document. In one BSON Document : producer information + dataet information +
     * observation information
     *
     * @param json a JSON object describing the information of a dataset
     * @param importConfig Import config object
     * @return A list of BSON Document ready to be imported in the mongodb database
     */
    public static List<Document> createObservationDocuments(JsonObject json, ImportConfig importConfig) {
        List<Document> observationDocuments = new ArrayList();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        /**
         * Producer information is stored
         */
        JsonObject producerJson = json.getAsJsonObject("producer");

        JsonArray datasetsJson = json.getAsJsonArray("datasets");

        for (int i = 0; i < datasetsJson.size(); i++) {

            /**
             * One dataset information is stored
             */
            JsonObject datasetTmpJson = datasetsJson.get(i).getAsJsonObject();
            JsonArray observationsTmpJson = datasetTmpJson.getAsJsonArray("observations");
            //remove observation array of the dataset object
            datasetTmpJson.remove("observations");

            /**
             * List of LocalDateTime to perform min and max operation
             */
            List<LocalDateTime> dateBegs = new ArrayList<>();
            List<LocalDateTime> dateEnds = new ArrayList<>();
            for (int j = 0; j < observationsTmpJson.size(); j++) {

                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                dateBegs.add(LocalDateTime.parse(
                        observationsTmpJson.get(j).getAsJsonObject().getAsJsonObject("temporalExtent").get("dateBeg").getAsString(),
                        format));
                dateEnds.add(LocalDateTime.parse(
                        observationsTmpJson.get(j).getAsJsonObject().getAsJsonObject("temporalExtent").get("dateEnd").getAsString(),
                        format));

            }
            /**
             * Class conversion headache to obtain the desired ISO8601 format
             */
            SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String[] temporalExtentTmp = new String[2];
            temporalExtentTmp[0] = formater.format(java.sql.Timestamp.valueOf(dateBegs.stream().min(LocalDateTime::compareTo).get()).getTime());
            temporalExtentTmp[1] = formater.format(java.sql.Timestamp.valueOf(dateEnds.stream().max(LocalDateTime::compareTo).get()).getTime());

            /**
             * Update dataset information with calculated temporalExtent
             */
            JsonObject temporalExtent = new JsonObject();
            temporalExtent.addProperty("dateBeg", temporalExtentTmp[0]);
            temporalExtent.addProperty("dateEnd", temporalExtentTmp[1]);
            JsonObject metadata = datasetTmpJson.get("metadata").getAsJsonObject();
            metadata.add("temporalExtent", temporalExtent);
            datasetTmpJson.remove("metadata");
            datasetTmpJson.add("metadata", metadata);

            /**
             * Add all the information in one BSON document
             */
            for (int j = 0; j < observationsTmpJson.size(); j++) {
                JsonObject observationDocument = new JsonObject();
                observationDocument.addProperty("documentId", observationsTmpJson.get(j).getAsJsonObject().get("observationId").getAsString());
                observationDocument.addProperty("version", json.get("version").getAsString());
                observationDocument.add("observation", observationsTmpJson.get(j).getAsJsonObject());
                observationDocument.add("dataset", datasetTmpJson);
                observationDocument.add("producer", producerJson);

                /**
                 * Link the datafile or not according to the ImportConfig object
                 */
                String observationId = observationsTmpJson.get(j).getAsJsonObject().get("observationId").getAsString();
                JsonArray datafileName = observationsTmpJson.get(j).getAsJsonObject().getAsJsonObject("result")
                        .getAsJsonObject("dataFile").getAsJsonArray("name");

                Iterator<JsonElement> it = datafileName.iterator();
                while (it.hasNext()) {
                    JsonElement jsonElement = it.next();
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (!importConfig.isLinkTowardDatafile(jsonObject.get("lang").getAsString(), observationId)) {
                        it.remove();
                    }
                }
                observationDocuments.add(Document.parse(gson.toJson(observationDocument)));
            }
        }
        return observationDocuments;
    }
}
