/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author coussotc
 */
public class SchemaValidator {

    private static final Logger logger = LogManager.getLogger(SchemaValidator.class);


    public static String validateSchema(String jsonString, String schemaUrl) throws IOException, JSONException {

        String schemaString = readSchemaFromUrl(schemaUrl, StandardCharsets.UTF_8);
        //String jsonString = readFile(json, StandardCharsets.UTF_8);
        JSONObject rawSchema = new JSONObject(new JSONTokener(schemaString));
        Schema schema = SchemaLoader.load(rawSchema);

        try {
            JSONObject json = new JSONObject(jsonString);
            //JSONObject json = new JSONObject(new JSONTokener(jsonString));
            schema.validate(json); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            String outputString = "";
            outputString = getValidationSubException(e, outputString);
            return outputString;
        }
        return "success";
    }

    private static String readSchemaFromUrl(String url, Charset encoding) throws IOException {
        InputStream in = new URL(url).openStream();
        byte[] encoded = IOUtils.toByteArray(in);
        return new String(encoded, encoding);
    }

    private static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private static String getValidationSubException(ValidationException e, String outputString) {
        outputString = outputString + e.getMessage() + "\n";
        for (ValidationException sub : e.getCausingExceptions()) {
            outputString = getValidationSubException(sub, outputString);
        }
        return outputString;
    }
}
