/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.DAO;

import fr.theia_land.in_situ.import_module.DAO.Utils.MongoDbUtils;
import java.util.List;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author coussotc
 */
@Component
public class MongoDBImport {

    @Autowired
    private MongoDbUtils mongoDbUtils;

    public void importWorkflow(List<Document> documents, String producerId) {
        mongoDbUtils.insertDocumentsFromDocumentList(documents, "observations", producerId);
        mongoDbUtils.enrichObservationsWithVariableAssociationsCollection(producerId);
        mongoDbUtils.groupDocumentsByVariableAtGivenLocationAndInsertInOtherCollection("observations", "observationsLite", producerId);
        mongoDbUtils.groupDocumentsByLocationAndInsertInOtherCollection("observationsLite", "mapItems", producerId);
        mongoDbUtils.storeAssociation("variableAssociations", producerId);
    }
}
