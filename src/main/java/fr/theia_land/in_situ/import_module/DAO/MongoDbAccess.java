///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package fr.theia_land.in_situ.import_module.DAO;
//
//import com.mongodb.Block;
//import com.mongodb.MongoClient;
//import com.mongodb.MongoClientOptions;
//import com.mongodb.MongoCredential;
//import com.mongodb.ServerAddress;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//import static com.mongodb.client.model.Filters.eq;
//import com.mongodb.client.model.IndexOptions;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Properties;
//import org.bson.Document;
//
///**
// * Class to manage database connection to the database. The MongoClient Class handles connection pooling and we don't
// * need to explicitly restore connection at the end of operation.
// *
// * @author coussotc
// */
//public class MongoDbAccess {
//
//    /**
//     * Method to get the connection to the replicaSet mongoDB database.
//     *
//     * @param propertiesFilePath String - path to reach the properties file containing connection credentials
//     * @return MongoClient - object used to connect the database
//     */
//    public static MongoClient getConnection(String propertiesFilePath) {
//        /**
//         * Load the properties for conneciton credentials
//         */
//        Properties props = new Properties();
//        try {
//            props.load(MongoDbAccess.class.getResourceAsStream(propertiesFilePath));
//        } catch (IOException ex) {
//            ex.getMessage();
//        }
//
//        /**
//         * Set the database connection credential
//         */
//        MongoCredential credential = MongoCredential.createCredential(props.getProperty("user"), props.getProperty("database"),
//                props.getProperty("password").toCharArray());
//        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
//        optionsBuilder.connectionsPerHost(40);
//        optionsBuilder.connectTimeout(30);
//        optionsBuilder.socketTimeout(60000);
//
//        MongoClientOptions options = optionsBuilder.build();
//
//        /**
//         * Instantiate the MongoClient object using connection credentials
//         */
//        MongoClient mongoClient = null;
//        try {
//            MongoClient mongo = new MongoClient(Arrays.asList(
//                    new ServerAddress(props.getProperty("ip1")),
//                    new ServerAddress(props.getProperty("ip2")),
//                    new ServerAddress(props.getProperty("ip3")),
//                    new ServerAddress(props.getProperty("ip4"))),
//                    credential,
//                    options);
//            mongoClient = mongo;
//        } catch (Exception e) {
//            e.getMessage();
//            System.exit(0);
//        }
//        return mongoClient;
//    }
//
//    /**
//     * Connect to a Mongodb local docker instance. For ev purpose.
//     *
//     * @return MongoClient - object used to connect the databse
//     */
//    public static MongoClient getConnectionOneInstance() {
//
//        /**
//         * Load the properties for conneciton credentials
//         */
//        Properties props = new Properties();
//        try {
//            System.out.println("Working Directory = "
//                    + System.getProperty("user.dir"));
//            props.load(MongoDbAccess.class.getResourceAsStream("../../../../../datasource_docker.properties"));
//        } catch (IOException ex) {
//            ex.getMessage();
//        }
//
//        /**
//         * Set the database connection credential
//         */
//        MongoCredential credential = MongoCredential.createCredential(props.getProperty("user"), props.getProperty("database"),
//                props.getProperty("password").toCharArray());
//
//        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
//        optionsBuilder.connectionsPerHost(40);
//        optionsBuilder.connectTimeout(30);
//        optionsBuilder.socketTimeout(60000);
//
//        MongoClientOptions options = optionsBuilder.build();
//
//        /**
//         * Instantiate the MongoClient object using connection credentials
//         */
//        MongoClient mongoClient = null;
//        try {
//            MongoClient mongo = new MongoClient(
//                    new ServerAddress("192.168.51.51", 27017),
//                    credential,
//                    options);
//            mongoClient = mongo;
//        } catch (Exception e) {
//            e.getMessage();
//            System.exit(0);
//        }
//        return mongoClient;
//    }
//
//    /**
//     * Insert valid JSON file into MongoDB collection
//     *
//     * @param file The .json file containing the JSON to insert in the collection
//     * @param mongoClient MongoClient object used to get the database and the collection to be used
//     * @param databaseName String - name of the database to be used
//     * @param collectionName String - name of the collection to be used
//     */
//    public static void insertDocumentsFromJsonFile(File file, MongoClient mongoClient, String databaseName, String collectionName) {
//        MongoDatabase database = mongoClient.getDatabase(databaseName);
//        MongoCollection<Document> collection = database.getCollection(collectionName);
//
//        //Read each line of the json file and store them in a list of Document object. Each file is one observation document.
//        List<Document> observationDocuments = new ArrayList<>();
//        try (BufferedReader br = new BufferedReader(new FileReader(file.getPath()));) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                observationDocuments.add(Document.parse(line));
//            }
//        } catch (IOException ex) {
//            ex.getMessage();
//        }
//        /**
//         * Insert all the Document object into the collection.
//         */
//        collection.insertMany(observationDocuments);
//    }
//
//    /**
//     * Insert a List of Document object intoa MongoDB collection
//     *
//     * @param documents List of Document object to be inserted in the database
//     * @param mongoClient MongoClient -- object used to connect the database
//     * @param databaseName String - the database name used to store the list of Document
//     * @param collectionName String - the collection name used to store the list of document
//     * @param producerId String - the producerId of the prodcuer inserting the Document. it is used to remove the
//     * relative document previously inserted before update.
//     */
//    public static void insertDocumentsFromDocumentList(List<Document> documents, MongoClient mongoClient, String databaseName, String collectionName, String producerId) {
//        MongoDatabase database = mongoClient.getDatabase(databaseName);
//        MongoCollection<Document> collection = database.getCollection(collectionName);
//        deleteDocumentsUsingProducerId(collection, producerId);
//        collection.insertMany(documents);
//    }
//
//    /**
//     * Delete all document in a MongoDB collection for a given producerId
//     *
//     * @param collection MongoCollection - the collection where the documents need to be removed
//     * @param producerId String - the producerId used to identify the Document to be removed
//     */
//    public static void deleteDocumentsUsingProducerId(MongoCollection<Document> collection, String producerId) {
//        collection.deleteMany(eq("producer.producerId", producerId));
//    }
//
//    /**
//     * Group the Document of a collection by variable at a given location for a given dataset. The resulting document
//     * are inserted in a new collection
//     *
//     * @param mongoClient MongoClient -- object used to connect the database
//     * @param databaseName String - the database name used to store the list of Document
//     * @param inputCollectionName String - the collection name from which document are grouped
//     * @param outputCollectionName String - the collection name used to store resulting document of the grouping
//     * operation
//     * @param producerId String - the producerId of the prodcuer inserting the Document. it is used to remove the
//     * relative document previously inserted before update.
//     */
//    public static void groupDocumentsByVariableAtGivenLocationAndInsertInOtherCollection(MongoClient mongoClient, String databaseName,
//            String inputCollectionName, String outputCollectionName, String producerId) {
//
//        Block<Document> printBlock = new Block<Document>() {
//            @Override
//            public void apply(final Document document) {
//            }
//        };
//
//        MongoDatabase database = mongoClient.getDatabase(databaseName);
//        MongoCollection<Document> collectionObservations = database.getCollection(inputCollectionName);
//        MongoCollection<Document> collectionObservationsLite = database.getCollection(outputCollectionName);
//
//        /**
//         * Remove documents of "observationLit" collection for a given producer
//         */
//        deleteDocumentsUsingProducerId(collectionObservationsLite, producerId);
//        /**
//         * Group operation
//         */
//
//        collectionObservations.aggregate(Arrays.asList(
//                new Document("$group",
//                        new Document("_id",
//                                new Document("producerId", "$producer.producerId")
//                                        .append("name", "$producer.name")
//                                        //.append("prodTitle", "$producer.title")
//                                        .append("fundings", "$producer.fundings")
//                                        .append("datasetId", "$dataset.datasetId")
//                                        .append("title", "$dataset.metadata.title")
//                                        .append("description", "$dataset.metadata.description")
//                                        .append("inspireTheme", "$dataset.metadata.inspireTheme")
//                                        .append("keywords", "$dataset.metadata.keywords")
//                                        .append("portalSearchCriteria", "$dataset.metadata.portalSearchCriteria")
//                                        .append("featureOfInterest", "$observation.featureOfInterest")
//                                        .append("observedPropertyName", "$observation.observedProperty.name")
//                                        .append("observedPropertyUnit", "$observation.observedProperty.unit")
//                        ).append("documentId",
//                                new Document("$push", "$documentId"))
//                                //                             .append("temporalExtent", new Document("$push", "$observation.temporalExtent"))),
//                                .append("temporalExtent",
//                                        new Document("$push",
//                                                new Document("dateBeg",
//                                                        new Document("$toDate", "$observation.temporalExtent.dateBeg")
//                                                )
//                                                        .append("dateEnd",
//                                                                new Document("$toDate", "$observation.temporalExtent.dateEnd"))
//                                        )
//                                )
//                ),
//                new Document("$project",
//                        new Document("documentId", 1)
//                                .append("observation.temporalExtent", "$temporalExtent")
//                                .append("producer.producerId", "$_id.producerId")
//                                .append("producer.name", "$_id.name")
//                                .append("producer.title", "$_id.prodTitle")
//                                .append("producer.fundings", "$_id.fundings")
//                                .append("dataset.datasetId", "$_id.datasetId")
//                                .append("dataset.metadata.title", "$_id.title")
//                                .append("dataset.metadata.description", "$_id.description")
//                                .append("dataset.metadata.inspireTheme", "$_id.inspireTheme")
//                                .append("dataset.metadata.keywords", "$_id.keywords")
//                                .append("dataset.metadata.portalSearchCriteria", "$_id.portalSearchCriteria")
//                                .append("observation.featureOfInterest", "$_id.featureOfInterest")
//                                .append("observation.observedProperty.name", "$_id.observedPropertyName")
//                                .append("observation.observedProperty.unit", "$_id.observedPropertyUnit")
//                                .append("_id", 0)
//                ),
//                new Document("$out", outputCollectionName)
//        )).allowDiskUse(Boolean.TRUE).forEach(printBlock);
//        /**
//         * Text indexes are inserted in the collection. if The index are already created nothing happens.
//         */
//        insertIndexes(collectionObservationsLite);
//    }
//
//    public static void groupDocumentsByLocationAndInsertInOtherCollection(MongoClient mongoClient, String databaseName,
//            String inputCollectionName, String outputCollectionName, String producerId) {
//        Block<Document> printBlock = new Block<Document>() {
//            @Override
//            public void apply(final Document document) {
//                //System.out.println(document);
//            }
//        };
//
//        MongoDatabase database = mongoClient.getDatabase(databaseName);
//        MongoCollection<Document> collectionObservationsLite = database.getCollection(inputCollectionName);
//        MongoCollection<Document> collectionMapItems = database.getCollection(outputCollectionName);
//        /**
//         * Remove documents of "observationLit" collection for a given producer
//         */
//        deleteDocumentsUsingProducerId(collectionMapItems, producerId);
//        /**
//         * Group operation
//         */
//        collectionObservationsLite.aggregate(Arrays.asList(
//                new Document("$group",
//                        new Document("_id",
//                                new Document("producerId", "$producer.producerId")
//                                        .append("datasetId", "$dataset.datasetId")
//                                        .append("samplingFeature", "$observation.featureOfInterest.samplingFeature")
//                        ).append("documentId",
//                                new Document("$push", "$documentId"))),
//                new Document("$project",
//                        new Document("documentId", 1)
//                                .append("producerId", "$_id.producerId")
//                                .append("samplingFeature", "$_id.samplingFeature")
//                                .append("_id", 0)),
//                new Document("$out", outputCollectionName)
//        ))
//                .allowDiskUse(Boolean.TRUE).forEach(printBlock);;
//    }
//
//    /**
//     * Create text indexes for a given collection
//     *
//     * @param collection the collection in which indexes will be inserted.
//     */
//    public static void insertIndexes(MongoCollection<Document> collection) {
//
//        Document textIndexes = new Document("dataset.metadata.description.text", "text")
//                .append("dataset.metadata.inspireTheme", "text")
//                .append("dataset.metadata.keywords.keyword.text", "text")
//                .append("dataset.metadata.title.text", "text")
//                .append("observation.featureOfInterest.samplingFeature.name.text", "text")
//                .append("observations.observedProperty.description.text", "text")
//                .append("observations.observedProperty.gcmdKeywords.term", "text")
//                .append("observations.observedProperty.gcmdKeywords.topic", "text")
//                .append("observations.observedProperty.gcmdKeywords.variableLevel1", "text")
//                .append("observations.observedProperty.gcmdKeywords.variableLevel2", "text")
//                .append("observations.observedProperty.gcmdKeywords.variableLevel3", "text")
//                .append("observations.observedProperty.name.text", "text")
//                .append("producer.description.text", "text")
//                .append("producer.fundings.acronym", "text")
//                .append("producer.fundings.name.text", "text")
//                .append("producer.name.text", "text")
//                .append("producer.title.text", "text");
//
//        IndexOptions io = new IndexOptions();
//        io.name("text_index_observations_lite");
//        io.weights(new Document("dataset.metadata.description.text", 1)
//                .append("dataset.metadata.inspireTheme", 5)
//                .append("dataset.metadata.keywords.keyword.text", 5)
//                //                .append("dataset.metadata.objective.text", "text")
//                .append("dataset.metadata.title.text", 2)
//                .append("observations.featureOfInterest.samplingFeature.name.text", 5)
//                .append("observations.observedProperty.description.text", 1)
//                .append("observations.observedProperty.gcmdKeywords.term", 2)
//                .append("observations.observedProperty.gcmdKeywords.topic", 2)
//                .append("observations.observedProperty.gcmdKeywords.variableLevel1", 1)
//                .append("observations.observedProperty.gcmdKeywords.variableLevel2", 1)
//                .append("observations.observedProperty.gcmdKeywords.variableLevel3", 1)
//                .append("observations.observedProperty.name.text", 20)
//                .append("observations.observedProperty.theiaVariable.prefLabel.text", 20)
//                //.append("producer.description.text", 1)
//                .append("producer.fundings.acronym", 2)
//                .append("producer.fundings.name.text", 2)
//                .append("producer.name.text", 20)
//                .append("producer.title.text", 5));
//
//        collection.createIndex(textIndexes, io);
//    }
//}
