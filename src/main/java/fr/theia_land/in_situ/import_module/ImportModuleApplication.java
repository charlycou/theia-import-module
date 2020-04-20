package fr.theia_land.in_situ.import_module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.MalformedJsonException;
import fr.theia_land.in_situ.import_module.DAO.MongoDBImport;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonFileNameException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonInternationalisationException;
import fr.theia_land.in_situ.import_module.Exceptions.JsonFileNotFoundException;
import fr.theia_land.in_situ.import_module.Exceptions.ZipFileNotFoundException;
import fr.theia_land.in_situ.import_module.Validation.DataFileValidation;
import fr.theia_land.in_situ.import_module.Validation.Utils.InternationalisationUtils;
import fr.theia_land.in_situ.import_module.Validation.JsonFileValidation;
import fr.theia_land.in_situ.import_module.Validation.Utils.ValidationUtils;
import fr.theia_land.in_situ.import_module.model.POJO.ImportConfig;
import fr.theia_land.in_situ.import_module.DAO.ObservationDocumentsCreation;
import fr.theia_land.in_situ.import_module.DAO.Utils.RDFUtils;
import fr.theia_land.in_situ.import_module.Exceptions.TheiaCategoriesException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.bson.Document;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class ImportModuleApplication implements CommandLineRunner {

    @Autowired
    private MongoDBImport mongoDBImport;

    @Autowired
    private RDFUtils rdfUtils;
    
    @Autowired
    private JsonFileValidation jsonFileValidation;
    
    @Autowired
    private InternationalisationUtils internationalisationUtils;

    private static final Logger logger = LoggerFactory.getLogger(ImportModuleApplication.class);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ImportModuleApplication.class);
        Properties properties = new Properties();
        //properties.put("logging.path", new File(args[0]).getParent());
        properties.put("logging.file", new File(args[0]).getParent() + "/validation-json.log");
        application.setDefaultProperties(properties);
        application.run(args);
    }

    @Override
    public void run(String... args) throws Exception {

        /**
         * args[0] = path to the folder where the files to be validated before to be imported are located
         * /data/producerId/tmp/{date}/unzip
         */
        String producerId = new File(new File(new File(new File(args[0]).getParent()).getParent()).getParent()).getName();

        /**
         * list all the json files of the folder (****_en.json + ****_fr.json ...) list all the zip files of the folder
         */
        List<File> jsonFiles = ValidationUtils.listFiles(args[0], "json");
        List<File> zipFiles = ValidationUtils.listFiles(args[0], "zip");
        Map<String, Map<String, Boolean>> linkedToDataFile = new HashMap<>();

        try {
            if (zipFiles.isEmpty()) {
                throw new ZipFileNotFoundException();
            }

            jsonFileValidation.validateJsonFileNames(args[0]);

        } catch (JsonFileNotFoundException | InvalidJsonFileNameException ex) {
            logger.error(ex.getMessage());
            System.exit(1);
        } catch (ZipFileNotFoundException ex1) {
            String[] logTrace = ex1.getMessage().split("\n");
            for (String logLine : logTrace) {
                logger.warn(logLine);
            }
        }

        /**
         * Validation of the JSON files
         */
        for (File jsonFile : jsonFiles) {
            /**
             * Get the language of the Json File
             */
            String lang = ValidationUtils.getLangFromJsonFile(jsonFile);
            /**
             * Check the json format using JSON schema
             */
            try {
                jsonFileValidation.validateJsonFileFormat(jsonFile.getAbsolutePath());
            } catch (JSONException | MalformedJsonException ex1) {
                logger.error(ex1.getMessage() + "\n"
                        + "Not a valid JSON according to RFC 8259. You can go to https://jsonlint.com/ to validate your JSON.\n"
                );
            } catch (InvalidJsonException ex2) {
                String[] logTrace = ex2.getMessage().split("\n");
                for (String logLine : logTrace) {
                    logger.error(logLine);
                }
                System.exit(1);
            }

            /**
             * JsonObject representation of the json file
             */
            String jsonString = ValidationUtils.readFile(jsonFile.getAbsolutePath(), StandardCharsets.UTF_8);
            JsonObject jsonObject = gson.fromJson(jsonString, JsonElement.class).getAsJsonObject();
            /**
             * Check that the ids of the json file correspond to the parent folder name as specified in the
             * documentation
             */
            jsonFileValidation.validateJsonFileIds(jsonObject, producerId);

            /**
             * if the validation of the JSON file is a success, then we check that the file name contained in the JSON
             * object are equals to those contained in the zip file
             */
            /**
             * Validation of the ZIP files
             */
            if (!zipFiles.isEmpty()) {
                try {
                    linkedToDataFile.put(lang, DataFileValidation.validateZipFiles(jsonObject, zipFiles));
                    ZipFile test = new ZipFile(zipFiles.get(0));
                } catch (IOException ex) {
                    logger.error(ex.getMessage());
                    System.exit(1);
                }
            } else {
                Map<String, Boolean> tmp = new LinkedHashMap<>();
                jsonObject.getAsJsonArray("datasets").forEach(dataset
                        -> dataset.getAsJsonObject().getAsJsonArray("observations").forEach(obs -> tmp.put(obs.getAsJsonObject()
                        .get("observationId").getAsString(), Boolean.FALSE))
                );
                linkedToDataFile.put(lang, tmp);
            }
        }

        /**
         * Creation of the ImportConfig object in order to know what importation pattern follow
         */
        ImportConfig importConfig = new ImportConfig(linkedToDataFile, jsonFiles);

//        /**
//         * Creation of the config file resuming what datafiles are present in the .json
//         */
//        File file = new File(new File(args[0]).getParent() + "/files_linked.json");
//        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
//            out.write(gson.toJson(importConfig));
//        };
        /**
         * if there is several files due to internationalisation we check that the fields that are not subjected to
         * internationalisation are identicals between the files
         */
        if (importConfig.getLanguages().length > 1) {
            try {
                jsonFileValidation.validateJsonInternationalisation(jsonFiles);
            } catch (InvalidJsonInternationalisationException ex) {
                String[] logTrace = ex.getMessage().split("\n");
                for (String logLine : logTrace) {
                    logger.error(logLine);
                }
                System.exit(1);
            }
        }

        /**
         * Creation of the MongoDB Observation documents according to the importConfig object
         */
        JsonObject internationalisedJson = internationalisationUtils.setInternationalisation(args[0], importConfig);
        JsonElement internationalisedJsonWithoutEmptyString = ValidationUtils.removeEmptyStringAndNullField(internationalisedJson);
        List<Document> observationDocuments = ObservationDocumentsCreation.createObservationDocuments(internationalisedJsonWithoutEmptyString.getAsJsonObject(), importConfig);

        /**
         * Check the Theia categories of each observations to see if it correponds to a category leaf
         */
        List<String> leafUris = rdfUtils.getTheiaCategoryLeafs();
        observationDocuments.forEach(observationDocument -> {
            JsonObject observationJson = gson.fromJson(observationDocument.toJson(), JsonObject.class);
            try {
                jsonFileValidation.validateTheiaCategories(observationJson.getAsJsonObject("observation").getAsJsonObject("observedProperty").getAsJsonArray("theiaCategories"), leafUris);
            } catch (TheiaCategoriesException ex) {
                logger.error(ex.getMessage());
                System.exit(1);
            }
        });
        /**
         * Import of the newly created documents in MongoDB "theia-in-situ" database
         */
        mongoDBImport.importWorkflow(observationDocuments, producerId);

        /**
         * Connection to the MongoDB database
         */
//        MongoClient mongoClient;
//        if (args.length > 1 && args[1].equals("docker")) {
//            mongoClient = MongoDbAccess.getConnectionOneInstance();
//        } else {
//            mongoClient = MongoDbAccess.getConnection("../../../../../datasource.properties");
//        }
        /**
         * Remove all the document for the given producer and Import of the BSON Document list in the MongoDB database
         */
//        MongoDbAccess.insertDocumentsFromDocumentList(observationDocuments, mongoClient, "theia-in-situ", "observations",
//                producerId);
//        /**
//         * Remove all the document for the given producer and Group the document for by variable for a given dataset of
//         * a given producer at a given location and insert the group in the "observationLit" collection
//         */
//        MongoDbAccess.groupDocumentsByVariableAtGivenLocationAndInsertInOtherCollection(mongoClient, "theia-in-situ",
//                "observations", "observationsLite", producerId);
//
//        MongoDbAccess.groupDocumentsByLocationAndInsertInOtherCollection(mongoClient, "theia-in-situ",
//                "observationsLite", "mapItems", producerId);
        /**
         * Create the files_linked.json file on /data/producerId/tmp/{date}/ folder
         */
        String files_linked = ValidationUtils.listLinkedFiles(args[0]);

        try (Writer out = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                                new File(new File(args[0]).getParent() + "/files_linked.json")),
                        StandardCharsets.UTF_8))) {
            out.write(files_linked);
        }
        logger.info("validation succesfull");
    }
}
