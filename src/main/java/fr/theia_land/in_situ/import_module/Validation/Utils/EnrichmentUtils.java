/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation.Utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.Exceptions.Iso3116Exception;
import fr.theia_land.in_situ.import_module.Exceptions.ScanRMetadataNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author coussotc
 */
public class EnrichmentUtils {

    private static final Logger logger = LogManager.getLogger(EnrichmentUtils.class);

    public static JsonObject getScanRMetadata(String idScanR) throws MalformedURLException, IOException {
        URL url = new URL("https://scanr.enseignementsup-recherche.gouv.fr/api/structures/" + idScanR);
        //URL url = new URL("https://www.google.com");
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }

        Gson gson = new Gson();
        JsonObject json = gson.fromJson(content.toString(), JsonObject.class);
        JsonObject funding = new JsonObject();
        JsonObject structure = new JsonObject();
        structure.add("label", json.getAsJsonObject("structure").get("label"));
        structure.add("acronym", json.getAsJsonObject("structure").get("acronym"));
        structure.add("type", json.getAsJsonObject("structure").getAsJsonObject("type").get("label"));
        structure.add("institutions", json.getAsJsonObject("structure").get("institutions"));

        funding.add("id", json.get("id"));
        funding.add("structure", structure);

        return funding;
    }

    public static JsonObject enrichmentUsingScanR(JsonObject orga) throws ScanRMetadataNotFoundException, Iso3116Exception {
        if (orga.has("idScanR") && !"".equals(orga.get("idScanR").getAsString())) {
            JsonObject infoScanR = new JsonObject();
            try {
                infoScanR = getScanRMetadata(orga.get("idScanR").getAsString());
                orga.remove("iso3166");
                orga.addProperty("iso3166", "fr");
                orga.remove("name");
                orga.add("name", infoScanR.getAsJsonObject("structure").get("label"));
                orga.remove("acronym");
                orga.add("acronym", infoScanR.getAsJsonObject("structure").get("acronym"));
                orga.remove("type");
                if (infoScanR.getAsJsonObject("structure").get("type").isJsonPrimitive() && !infoScanR.getAsJsonObject("structure").get("type").getAsString().equals("Structure federative")) {
                    String type = infoScanR.getAsJsonObject("structure").get("type").getAsString();
                    switch (type) {
                        case "EPST":
                            orga.addProperty("type", "French research institutes");
                            break;
                        case "EPIC":
                            orga.addProperty("type", "French research institutes");
                            break;
                        case "Autre établissement de l'Etat":
                            orga.addProperty("type", "French research institutes");
                            break;
                        case "Enseignement supérieur":
                            orga.addProperty("type", "French universities and schools ");
                            break;
                        default:
                            orga.addProperty("type", "French research institutes");
                            break;
                    }
                } else if (infoScanR.getAsJsonObject("structure").get("institutions").isJsonArray()) {
                    int i = 0;
                    String type = null;
                    while (type == null && i < infoScanR.getAsJsonObject("structure").getAsJsonArray("institutions").size()) {
                        if (!infoScanR.getAsJsonObject("structure").getAsJsonArray("institutions").get(i).getAsJsonObject().get("code").isJsonNull()
                                && !infoScanR.getAsJsonObject("structure").getAsJsonArray("institutions").get(i).getAsJsonObject().getAsJsonObject("code").get("type").isJsonNull()) {
                            type = infoScanR.getAsJsonObject("structure").getAsJsonArray("institutions").get(i).getAsJsonObject().getAsJsonObject("code").get("type").getAsString();
                            switch (type) {
                                case "UMS":
                                    orga.addProperty("type", "Federative structure");
                                    break;
                                case "FR":
                                    orga.addProperty("type", "Federative structure");
                                    break;
                                case "UMR":
                                    orga.addProperty("type", "Research unit");
                                    break;
                                case "UR":
                                    orga.addProperty("type", "Research unit");
                                    break;
                                default:
                                    type = null;
                                    break;
                            }
                        }
                        i++;
                    }
                    if (type == null) {
                        orga.addProperty("type", "Other");
                    }
                } else {
                    orga.addProperty("type", "Other");
                }
                try {
                    return enrichmentUsingIso3166(orga);
                } catch (IOException ex) {
                    throw new Iso3116Exception(orga.get("iso3166").getAsString());
                }
            } catch (IOException ex) {
                throw new ScanRMetadataNotFoundException(orga.get("idScanR").getAsString());
            }
        } else {
            try {
                return enrichmentUsingIso3166(orga);
            } catch (IOException ex) {
                throw new Iso3116Exception(orga.get("iso3166").getAsString());
            }
        }
    }

    public static JsonObject enrichmentUsingIso3166(JsonObject orga) throws IOException {
        if (orga.get("iso3166") == null) {
            return orga;
        } else {
            return setCountryFromIso3166(orga);
        }
    }

    /**
     * Remove iso3166 field from a JsonObject and replace it with the corresponding country and its traduction
     *
     * @param orga JSonObject of the organisation
     * @return JsonObject with iso3166 field removed and country field added
     * @throws IOException
     */
    private static JsonObject setCountryFromIso3166(JsonObject orga) throws IOException {
        /**
         * Query rest country API to ahve information about the the country using ISO3166 code
         */
        if (orga.get("iso3166").getAsString().equals("international")) {
            JsonArray country = new JsonArray();
            JsonObject fr = new JsonObject();
            JsonObject en = new JsonObject();
            fr.addProperty("lang", "fr");
            fr.addProperty("text", "international");
            en.addProperty("lang", "en");
            en.addProperty("text", "international");
            country.add(fr);
            country.add(en);
            /**
             * Remove iso3166 field and add country field
             */
            orga.remove("iso3166");
            orga.add("country", country);
            return orga;
        } else {
            URL url = new URL("https://restcountries.eu/rest/v2/alpha/" + orga.get("iso3166").getAsString());
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }
            /**
             * Create the country josn object with the french and english traduction of the country name
             */
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(content.toString(), JsonObject.class);
            JsonArray country = new JsonArray();
            JsonObject fr = new JsonObject();
            JsonObject en = new JsonObject();
            fr.addProperty("lang", "fr");
            fr.addProperty("text", json.getAsJsonObject("translations").get("fr").getAsString());
            en.addProperty("lang", "en");
            en.addProperty("text", json.get("name").getAsString());
            country.add(fr);
            country.add(en);
            /**
             * Remove iso3166 field and add country field
             */
            orga.remove("iso3166");
            orga.add("country", country);

            return orga;
        }
    }

    /**
     * Set the producer fundings metadata using scanR informations
     *
     * @param fundings JsonArray of the producer.fundigs
     * @return JsonArray of enriched fundings
     */
    public static JsonArray setFundingsUsingScanR(JsonArray fundings) {
        JsonArray fundingsReturn = new JsonArray();
        fundings.forEach(funding -> {
            JsonObject fund = funding.getAsJsonObject();
            try {
                fundingsReturn.add(EnrichmentUtils.enrichmentUsingScanR(fund));
            } catch (ScanRMetadataNotFoundException ex) {
                logger.warn(ex.getMessage());
            } catch (Iso3116Exception ex) {
                logger.warn(ex.getMessage());
            }

        });
        return fundingsReturn;
    }

    public static JsonObject setOrganisationUsingScanR(JsonObject organisation) {
        try {
            return EnrichmentUtils.enrichmentUsingScanR(organisation);
        } catch (ScanRMetadataNotFoundException | Iso3116Exception ex) {
            logger.warn(ex.getMessage());
            return organisation;
        }
    }

    public static JsonArray setContactsUsingScanR(JsonArray contacts) {
        JsonArray contactModified = new JsonArray();
        for (JsonElement contact : contacts) {
            if (contact.getAsJsonObject().has("organisation") && contact.getAsJsonObject().getAsJsonObject("organisation").has("idScanR")) {
                JsonObject organisation = contact.getAsJsonObject().getAsJsonObject("organisation");
                contact.getAsJsonObject().remove("organisation");
                contact.getAsJsonObject().add("organisation", setOrganisationUsingScanR(organisation));
                contactModified.add(contact);
            } else if (contact.getAsJsonObject().has("idScanR")) {
                contactModified.add(setOrganisationUsingScanR(contact.getAsJsonObject()));
            } else {
                contactModified.add(contact.getAsJsonObject());
            }
        }
        return contactModified;
    }
}
