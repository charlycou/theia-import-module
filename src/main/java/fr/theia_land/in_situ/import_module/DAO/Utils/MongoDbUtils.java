/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.DAO.Utils;

import com.mongodb.client.result.UpdateResult;
import fr.theia_land.in_situ.import_module.CustomConfig.GenericAggregationOperation;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 *
 * @author coussotc
 */
@Component
public class MongoDbUtils {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Delete all document in a MongoDB collection for a given producerId
     *
     * @param producerId String - the producerId used to identify the Document to be removed
     * @param collectionName
     */
    public void deleteDocumentsUsingProducerId(String producerId, String collectionName) {
        Query query = Query.query(where("producer.producerId").is(producerId));
        mongoTemplate.remove(query, collectionName);
    }

    /**
     * Insert observations documents for a given producer in a collection using a list of documents. The existing
     * document of the given producer are deleted before the insert.
     *
     * @param documents List\<Document\> containing the document to be inserted
     * @param collectionName String the name of the collection
     * @param producerId String the id of the producer
     */
    public void insertDocumentsFromDocumentList(List<Document> documents, String collectionName, String producerId) {
        deleteDocumentsUsingProducerId(producerId, collectionName);
        mongoTemplate.insert(documents, collectionName);
    }

    /**
     * Group observations from a collection using producerId, datasetId, and location. Store the result in a new
     * collection
     *
     * @param inputCollectionName name of the input collection
     * @param outputCollectionName name of the output collection
     * @param producerId producerId
     */
    public void groupDocumentsByLocationAndInsertInOtherCollection(
            String inputCollectionName, String outputCollectionName, String producerId) {

        //deleteDocumentsUsingProducerId(producerId, outputCollectionName);
        mongoTemplate.remove(Query.query(where("producerId").is(producerId)), outputCollectionName);
        MatchOperation m1 = Aggregation.match(where("producer.producerId").is(producerId));

        UnwindOperation u1 = Aggregation.unwind("observations");

        ProjectionOperation p1 = Aggregation.project()
                .and("producer.producerId").as("producerId")
                .and("dataset.datasetId").as("datasetId")
                .and("observations.featureOfInterest.samplingFeature").as("samplingFeature")
                .and("observations.observationId").as("observationId");

        //UnwindOperation u1 = Aggregation.unwind("documentIds");
        GroupOperation g1 = group(
                "producerId",
                "datasetId",
                "samplingFeature"
        ).push("observationId").as("observationIds");

        ProjectionOperation p2 = Aggregation.project("observationIds")
                .and("_id.producerId").as("producerId")
                .and("_id.datasetId").as("datasetId")
                .and("_id.samplingFeature").as("samplingFeature")
                .andExclude("_id");

        //OutOperation o1 = Aggregation.out(outputCollectionName);
        AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();
        //List<AggregationOperation> aggs = Arrays.asList(m1, p1, u1, g1, p2);
        List<AggregationOperation> aggs = Arrays.asList(m1, u1, p1, g1, p2);
        List<Document> docs = mongoTemplate.aggregate(Aggregation.newAggregation(aggs).withOptions(options), inputCollectionName, Document.class).getMappedResults();
        mongoTemplate.insert(docs, outputCollectionName);

    }

    /**
     * Group the Document of a collection by variable at a given location for a given dataset. The resulting document
     * are inserted in a new collection
     *
     * @param inputCollectionName String - the collection name from which document are grouped
     * @param outputCollectionName String - the collection name used to store resulting document of the grouping
     * operation
     * @param producerId String - the producerId of the prodcuer inserting the Document. it is used to remove the
     * relative document previously inserted before update.
     */
    public void groupDocumentsByVariableAtGivenLocationAndInsertInOtherCollection(
            String inputCollectionName, String outputCollectionName, String producerId) {

        deleteDocumentsUsingProducerId(producerId, outputCollectionName);

        /**
         * All observation matching the producer ID
         */
        MatchOperation m1 = Aggregation.match(where("producer.producerId").is(producerId));

        /**
         * Project the fields used in group operation
         */
        ProjectionOperation p1 = Aggregation.project()
                //.and("documentId").as("observation.documentId")
                .and("observation.observationId").as("observation.observationId")
                .and(DateOperators.DateFromString.fromStringOf("observation.temporalExtent.dateBeg")).as("observation.temporalExtent.dateBeg")
                .and(DateOperators.DateFromString.fromStringOf("observation.temporalExtent.dateEnd")).as("observation.temporalExtent.dateEnd")
                .and("observation.observedProperty").as("observation.observedProperty")
                .and("observation.featureOfInterest").as("observation.featureOfInterest")
                .and("observation.dataType").as("observation.dataType")
                .and("observation.timeSerie").as("observation.timeSerie")
                .and("producer").as("producer")
                .and("dataset").as("dataset")
                .and("observation.featureOfInterest.samplingFeature").as("samplingFeature")
                //                .and("observation.temporalExtent").as("temporalExtent")
                //                .and("observation.observedProperty").as("observedProperty")
                .and("observation.observedProperty.theiaVariable.uri").as("uri")
                .and(ArrayOperators.Filter.filter("observation.observedProperty.name").as("item").by(ComparisonOperators.Eq.valueOf("item.lang").equalToValue("en"))).as("producerVariableNameEn");

        /**
         * Unwind the producerVariableNameEn array that contain only one element
         */
        UnwindOperation u1 = Aggregation.unwind("producerVariableNameEn");
        /**
         * Group all the observation by producer, dataset, sampling feature and theiaVariable or producerVariableNameEn
         * when theiaVariable does not exist. The documentId of the observations grouped are push in an array The
         * observedPorperty of the observation grouped are pushed in an array in order to keep the information
         * theiaVariable / theiaCategories for each observation
         *
         * This group operation is complex and not supported by Spring data mongodb. AggregationOperation.class is
         * extended to support Aggregation operation building using Document.class
         */

        String groupJson = "{\n"
                + "	\"_id\": {\n"
                + "		\"producerId\": \"$producer.producerId\",\n"
                + "		\"datasetId\": \"$dataset.datasetId\",\n"
                + "		\"theiaVariableUri\": {\n"
                + "			\"$cond\": [{\n"
                + "				\"$not\": [\"$uri\"]\n"
                + "			}, null, \"$uri\"]\n"
                + "		},\n"
                + "		\"producerVariableNameEn\": {\n"
                + "			\"$cond\": [{\n"
                + "				\"$not\": [\"$uri\"]\n"
                + "			}, \"$producerVariableNameEn.text\", null]\n"
                + "		},\n"
                + "		\"samplingFeature\": \"$samplingFeature\"\n"
                + "	},\n"
                //                + "	\"documentIds\": {\n"
                //                + "		\"$push\": \"$documentId\"\n"
                //                + "	},\n"
                //                + "	\"observedProperties\": {\n"
                //                + "		\"$push\": \"$observedProperty\"\n"
                //                + "	},\n"
                //                + "	\"samplingFeature\": {\n"
                //                + "		\"$first\": \"$samplingFeature\"\n"
                //                + "	},\n"
                + "	\"producer\": {\n"
                + "		\"$first\": \"$producer\"\n"
                + "	},\n"
                + "	\"dataset\": {\n"
                + "		\"$first\": \"$dataset\"\n"
                + "	},\n"
                + "	\"observations\": {\n"
                + "		\"$push\": \"$observation\"\n"
                + "	}\n"
                //                + "	\"temporalExtents\": {\n"
                //                + "		\"$push\": \"$temporalExtent\"\n"
                //                + "	}\n"
                + "}";
        AggregationOperation g1 = new GenericAggregationOperation("$group", groupJson);

        /**
         * Project the result of the group operation before to be inserted in collection
         */
        String projectJson = "{\n"
                //                + "	\"documentIds\": 1,\n"
                + "	\"producer.producerId\": 1,\n"
                + "	\"producer.name\": 1,\n"
                + "	\"producer.title\": 1,\n"
                + "	\"producer.fundings\": 1,\n"
                + "	\"dataset.datasetId\": 1,\n"
                + "	\"dataset.metadata.portalSearchCriteria\": 1,\n"
                + "	\"dataset.metadata.title\": 1,\n"
                + "	\"dataset.metadata.keywords\": 1,\n"
                + "	\"dataset.metadata.description\": 1,\n"
                + "     \"observations\": 1,\n"
                //                + "	\"observation.observedProperties\": \"$observedProperties\",\n"
                //                + "	\"observation.temporalExtents\": \"$temporalExtents\",\n"
                //                + "	\"observation.featureOfInterest.samplingFeature\": \"$samplingFeature\",\n"
                + "	\"_id\": 0\n"
                + "}";
        AggregationOperation p2 = new GenericAggregationOperation("$project", projectJson);

        /**
         * Insert the result of the aggregation pipeline in the collection
         */
        AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();
        List<Document> docs = mongoTemplate.aggregate(Aggregation.newAggregation(m1, p1, u1, g1, p2).withOptions(options), inputCollectionName, Document.class).getMappedResults();
        mongoTemplate.insert(docs, outputCollectionName);

        insertIndexes(outputCollectionName);

    }

    /**
     * Store the different variable association made for a given producer in a collection. For a given producer, an
     * associaiton is concidered different if for an identical variable name the categories are different.
     *
     * @param outputCollectionName String - the name of the output collection (variableAssociationss)
     * @param producerId String - the producerId
     */
    public void storeAssociation(String outputCollectionName, String producerId) {
        MatchOperation m1 = Aggregation.match(where("producer.producerId").is(producerId));
        MatchOperation m2 = Aggregation.match(where("observation.observedProperty.theiaVariable").exists(true));
        ProjectionOperation p1 = Aggregation.project()
                .and("producer").as("producer")
                .and("observation.observedProperty.theiaCategories").as("theiaCategories")
                .and(ArrayOperators.Filter.filter("observation.observedProperty.name").as("item").by(ComparisonOperators.Eq.valueOf("item.lang").equalToValue("en"))).as("producerVariableName")
                .and("observation.observedProperty.theiaVariable").as("theiaVariable");

        UnwindOperation u1 = Aggregation.unwind("producerVariableName");

        ProjectionOperation p2 = Aggregation.project()
                .and("producer").as("producer")
                .and("theiaCategories").as("theiaCategories")
                .and("theiaVariable").as("theiaVariable")
                .and("producerVariableName.text").as("producerVariableNameEn");

        GroupOperation g1 = Aggregation.group("producer.producerId", "theiaCategories", "theiaVariable.uri")
                .first("theiaVariable").as("theiaVariable")
                .first("producer.producerId").as("producerId")
                .push("producerVariableNameEn").as("producerVariableNameEn")
                .first("theiaCategories").as("theiaCategories")
                .addToSet(true).as("isActive");

        UnwindOperation u2 = Aggregation.unwind("isActive");
        UnwindOperation u3 = Aggregation.unwind("producerVariableNameEn");
        ProjectionOperation p3 = Aggregation.project().andExclude("_id");

        // OutOperation o1 = Aggregation.out(outputCollectionName);
        List<Document> associations = mongoTemplate.aggregate(Aggregation.newAggregation(m1, m2, p1, u1, p2, g1, u2, u3, p3), "observations", Document.class).getMappedResults();
        refreshAssociationSubmited(outputCollectionName, producerId, associations);
    }

    
    public void refreshAssociationSubmited(String collectionName, String producerId, List<Document> associations) {
        Update up1 = Update.update("isActive", false);
        Query query = Query.query(where("producerId").is(producerId));
        mongoTemplate.updateMulti(query, up1, collectionName);
        mongoTemplate.insert(associations, collectionName);
        
        MatchOperation m1 = Aggregation.match(Criteria.where("producerId").is(producerId));

        GroupOperation g1 = Aggregation.group("producerId", "theiaCategories", "theiaVariable.uri", "producerVariableNameEn")
                .first("theiaVariable").as("theiaVariable")
                .first("producerId").as("producerId")
                .first("theiaCategories").as("theiaCategories")
                .first("producerVariableNameEn").as("producerVariableNameEn")
                .addToSet("isActive").as("isActiveArray");

        Cond condOperation = ConditionalOperators.when(Criteria.where("isActiveArray").ne(new Boolean[]{false}))
                .then(true)
                .otherwise(false);

        ProjectionOperation p1 = Aggregation.project()
                .and("theiaVariable").as("theiaVariable")
                .and("producerId").as("producerId")
                .and("theiaCategories").as("theiaCategories")
                .and("producerVariableNameEn").as("producerVariableNameEn")
                .andExclude("_id")
                .and(condOperation).as("isActive");

        List<Document> docs = mongoTemplate.aggregate(Aggregation.newAggregation(m1, g1, p1), "variableAssociations", Document.class).getMappedResults();

        mongoTemplate.remove(query, collectionName);
        mongoTemplate.insert(docs, collectionName);

    }

    public void enrichObservationsWithVariableAssociationsCollection(String producerId) {
        MatchOperation m1 = Aggregation.match(where("producer.producerId").is(producerId));

        String projectJson = "{\n"
                + "	\"documentId\": 1,\n"
                + "     \"producerId\":\"$producer.producerId\"\n"
                + "	\"theiaCategories\": \"$observation.observedProperty.theiaCategories\",\n"
                + "	\"producerVariableNameEn\": {\n"
                + "		\"$filter\": {\n"
                + "			\"input\": \"$observation.observedProperty.name\",\n"
                + "			\"as\": \"name\",\n"
                + "			\"cond\": {\n"
                + "				\"$eq\": [\"$$name.lang\", \"en\"]\n"
                + "			}\n"
                + "		}\n"
                + "	}\n"
                + "}";

        AggregationOperation p1 = new GenericAggregationOperation("$project", projectJson);

        UnwindOperation u1 = Aggregation.unwind("producerVariableNameEn");

        String lookupJson = "{\n"
                + "	\"from\": \"variableAssociations\",\n"
                + "	\"let\": {\n"
                + "		\"observations_producerVariableNameEn\": \"$producerVariableNameEn\",\n"
                + "		\"observations_theiaCategories\": \"$theiaCategories\"\n"
                + "             \"observations_producerId\":\"$producerId\"\n"
                + "	},\n"
                + "	\"pipeline\": [{\n"
                + "			\"$match\": {\n"
                + "				\"$expr\": {\n"
                + "					\"$and\": [{\n"
                + "							\"$eq\": [\"$producerVariableNameEn\", \"$$observations_producerVariableNameEn.text\"]\n"
                + "						},\n"
                + "						{\n"
                + "							\"$eq\": [\"$producerId\", \"$$observations_producerId\"]\n"
                + "						},\n"
                + "						{\n"
                + "							\"$eq\": [\"$theiaCategories\", \"$$observations_theiaCategories\"]\n"
                + "						}\n"
                + "					]\n"
                + "				}\n"
                + "			}\n"
                + "		},\n"
                + "		{\n"
                + "			\"$project\": {\n"
                + "				\"theiaVariable\": 1\n"
                + "			}\n"
                + "		}\n"
                + "	],\n"
                + "	\"as\": \"theiaVariables\"\n"
                + "}";

        AggregationOperation l1 = new GenericAggregationOperation("$lookup", lookupJson);

        UnwindOperation u2 = Aggregation.unwind("theiaVariables");
        ProjectionOperation p2 = Aggregation.project("documentId").and("theiaVariables.theiaVariable").as("theiaVariable");

        List<Document> observations = mongoTemplate.aggregate(Aggregation.newAggregation(m1, p1, u1, l1, u2, p2), "observations", Document.class).getMappedResults();

        observations.forEach(observation -> {
            Query query = Query.query(where("documentId").is(observation.getString("documentId")));
            Update update = Update.update("observation.observedProperty.theiaVariable", observation.get("theiaVariable"));
            UpdateResult result = mongoTemplate.updateFirst(query, update, "observations");
            System.out.println(result);
        });
    }

    /**
     * Create text indexes for a given collection
     *
     * @param collection the collection in which indexes will be inserted.
     */
    public void insertIndexes(String outputCollectionName) {

        TextIndexDefinition textIndex = new TextIndexDefinitionBuilder().named("observationLite-text-search-index")
                .onField("dataset.metadata.description.text", 1F)
                .onField("dataset.metadata.inspireTheme", 5F)
                .onField("dataset.metadata.keywords.keyword.text", 5F)
                .onField("dataset.metadata.title.text", 2F)
                .onField("observations.featureOfInterest.samplingFeature.name.text", 5F)
                .onField("observations.observedProperty.description.text", 1F)
                .onField("observations.observedProperty.gcmdKeywords.term", 2F)
                .onField("observations.observedProperty.gcmdKeywords.topic", 2F)
                .onField("observations.observedProperty.gcmdKeywords.variableLevel1", 1F)
                .onField("observations.observedProperty.gcmdKeywords.variableLevel2", 1F)
                .onField("observations.observedProperty.gcmdKeywords.variableLevel3", 1F)
                .onField("observations.observedProperty.name.text", 20F)
                .onField("observations.observedProperty.theiaVariable.prefLabel.text", 20F)
                .onField("producer.fundings.acronym", 2F)
                .onField("producer.fundings.name.text", 2F)
                .onField("producer.name.text", 20F)
                .onField("producer.title.text", 5F)
                .build();
        mongoTemplate.indexOps(outputCollectionName).ensureIndex(textIndex);
    }

}
