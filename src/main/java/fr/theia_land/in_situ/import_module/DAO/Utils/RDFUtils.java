/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.DAO.Utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author coussotc
 */
@Component
public class RDFUtils {

    @Value("${sparql.endpoint.url}")
    private String sparqlUrl;

    public boolean existSkosCategoryConcept(String uri) {
        String queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
                + "SELECT *\n"
                + "FROM <https://w3id.org/ozcar-theia/>\n"
                + "WHERE {\n"
                + "   <https://w3id.org/ozcar-theia/variableCategoriesGroup> skos:member <" + uri + ">\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.createServiceRequest(sparqlUrl, query);) {

            ResultSet rs = qExec.execSelect();
            return rs.hasNext();
        }
    }

    public boolean hasSkosNarrower(String uri) {
        String queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
                + "SELECT *\n"
                + "FROM <https://w3id.org/ozcar-theia/>\n"
                + "WHERE {\n"
                + "   <" + uri + "> skos:narrower ?o\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.createServiceRequest(sparqlUrl, query);) {

            ResultSet rs = qExec.execSelect();
            return rs.hasNext();
        }
    }

    public List<String> getTheiaCategoryLeafs() {
        List<String> uris = new ArrayList<>();
        String queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
                + "SELECT ?s\n"
                + "FROM <https://w3id.org/ozcar-theia/>\n"
                + "WHERE {\n"
                + "  <https://w3id.org/ozcar-theia/variableCategoriesGroup> skos:member ?s .\n"
                + "  MINUS {?s skos:narrower ?o}\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.createServiceRequest(sparqlUrl, query);) {
            ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                uris.add(rs.next().get("s").toString());
            }
        }
        return uris;
    }
}
