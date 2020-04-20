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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author coussotc
 */
public class ValidationUtils {

    /**
     * list all files of a given extension of a folder
     *
     * @param folder path of the folder containing the json files
     * @param extension the extension of the files to look for
     * @return the list of the zip files of the folder
     */
    public static ArrayList<File> listFiles(String folder, String extension) {
        File[] files = new File(folder).listFiles();
        ArrayList<File> textFiles = new ArrayList<>();
        for (File f : files) {
            if (FilenameUtils.getExtension(f.getName()).equals(extension)) {
                textFiles.add(f);
            }
        }
        return textFiles;
    }

    public static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static String getLangFromJsonFile(File jsonFile) {
        String regexLang = "(?:[A-Z]{4}_)(en|fr|es)(?:\\.json)";
        String lang = null;
        Pattern languagesPattern = Pattern.compile(regexLang);
        Matcher m = languagesPattern.matcher(jsonFile.getName());
        if (m.find()) {
            lang = m.group(1);
        }
        return lang;
    }

    public static JsonElement removeEmptyStringAndNullField(JsonElement json) {
        JsonElement jsonToReturn = json.deepCopy();
        if (json.isJsonObject()) {
            for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonNull()) {
                    jsonToReturn.getAsJsonObject().remove(entry.getKey());
                } else {
                    //entry.setValue(removeEmptyStringAndNullField(entry.getValue()));
                    jsonToReturn.getAsJsonObject().remove(entry.getKey());
                    jsonToReturn.getAsJsonObject().add(entry.getKey(), removeEmptyStringAndNullField(entry.getValue()));
                }
            }
            //jsonToReturn = json;
//            json.getAsJsonObject().entrySet().forEach(item -> {

//                removeEmptyStringAndNullField(item.getValue());
//            });
        } else if (json.isJsonArray()) {
            JsonArray tmp = new JsonArray();
            for (JsonElement items : json.getAsJsonArray()) {
                tmp.add(removeEmptyStringAndNullField(items));
            }
            jsonToReturn = tmp;
//            json.getAsJsonArray().forEach(item ->{
//                removeEmptyStringAndNullField(item);
//            });
        } else if (json.isJsonPrimitive()) {
            if (json.getAsJsonPrimitive().isString()) {
                if ("".equals(json.getAsString())) {
                    jsonToReturn = null;
                } else {
                    jsonToReturn = json;
                }
            }
        } else {
            jsonToReturn = json;
        }
        return jsonToReturn;
    }

    /**
     * Check if the object returned by the jsonsEqual method correponds to an emty object
     *
     * @param obj Object return by the jsonsEqual method
     * @return boolean true if the object correspond to an empty
     */
    private static boolean checkObjectIsEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        String objData = obj.toString();
        if (objData.length() == 0) {
            return true;
        }
        if (objData.equalsIgnoreCase("{}")) {
            return true;
        }
        return false;
    }

    /**
     * Check for the differences between two JSON object and return a pattern descibing the difference
     *
     * @param obj1 a JSONObject
     * @param obj2 a JSONObject
     * @return a Object that can be an instance of String of JSONObject describing the difference between the two
     * objects an empty JSON object state that there is not differences between the two objects
     * @throws JSONException if a JSONObject format is invalid
     */
    public static Object jsonsEqual(JsonElement obj1, JsonElement obj2) throws JSONException {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject diff = new JsonObject();

        /**
         * Check if the the two object are instance of the same class Return a String is the two object are not of the
         * same class
         */
        if (!obj1.getClass().equals(obj2.getClass())) {
            return "The two object does not have the same class";
        }

        /**
         * Check if the objects are instance of JSONObject return a JSONObject if the two JSONObject differ
         */
        if (obj1.isJsonObject() && obj2.isJsonObject()) {

            JsonObject jsonObj1 = obj1.getAsJsonObject();
            JsonObject jsonObj2 = obj2.getAsJsonObject();
            List<String> names = null;
            List<String> names2 = null;

            /**
             * if one object is equal to an empty JSON object the fields are not stored into the a variable
             */
            if (!jsonObj1.toString().equalsIgnoreCase("{}")) {
                names = new ArrayList<>(jsonObj1.keySet());
            }
            if (!jsonObj2.toString().equalsIgnoreCase("{}")) {
                names2 = new ArrayList<>(jsonObj2.keySet());
            }

            /**
             * Return an empty JSON object if both JSONObject are emtpy JSON object
             */
            if (names == null && names2 == null) {
                return new JsonObject();
                /**
                 * Check if only on of the JSON object is an empty object
                 */
            } else if (names == null || names2 == null) {
                return "One of the two object is an empty JSON object";
                /**
                 * check for the difference between the JSON object
                 */
            } else {
                /**
                 * Check that the name of the fields are similar for both of the JSONObject
                 */
                if (!(names.containsAll(names2) && names2.containsAll(names))) {
                    List<String> names3 = new ArrayList<>(jsonObj2.keySet());
                    names2.removeAll(names);
                    if (!names2.isEmpty()) {
                        for (String fieldName : names2) {
                            if (jsonObj1.has(fieldName) || jsonObj2.has(fieldName)) {
                                diff.addProperty(fieldName, "field name not present in the first file");
                            }
                        }
                    }
                    names.removeAll(names3);
                    if (!names.isEmpty()) {
                        for (String fieldName : names) {
                            if (jsonObj1.has(fieldName) || jsonObj2.has(fieldName)) {
                                diff.addProperty(fieldName, "field name not present in the second file");
                            }
                        }
                    }
                    names = new ArrayList<>(jsonObj1.keySet());
                    names2 = new ArrayList<>(jsonObj2.keySet());

                }
                /**
                 * Check that the value of the JSON object fields' are similar
                 */
                if (names.containsAll(names2) && names.size() == names2.size()) {
                    /**
                     * Recurcively call the function of the each field of the JSONObject
                     */
                    for (String fieldName : names) {
                        JsonElement obj1FieldValue = jsonObj1.get(fieldName);
                        JsonElement obj2FieldValue = jsonObj2.get(fieldName);
                        Object obj = jsonsEqual(obj1FieldValue, obj2FieldValue);
                        if (obj != null && !checkObjectIsEmpty(obj)) {
                            if (obj instanceof String) {
                                diff.addProperty(fieldName, obj.toString());
                            } else if (obj instanceof JsonElement) {
                                diff.add(fieldName, (JsonElement) obj);
                            }
                        }
                    }
                } else {
                    for (String fieldName : names) {
                        if (names2.contains(fieldName) && names.contains(fieldName)) {
                            JsonElement obj1FieldValue = jsonObj1.get(fieldName);
                            JsonElement obj2FieldValue = jsonObj2.get(fieldName);
                            Object obj = jsonsEqual(obj1FieldValue, obj2FieldValue);
                            if (obj != null && !checkObjectIsEmpty(obj)) {
                                if (obj instanceof String) {
                                    diff.addProperty(fieldName, obj.toString());
                                } else if (obj instanceof JsonElement) {
                                    diff.add(fieldName, (JsonElement) obj);
                                }
                            }
                        }
                    }
                }
                return diff;
            }
            /**
             * Check if hte objects are instance of JSONArray return a JSONObject if the two JSONArray differ
             */
        } else if (obj1.isJsonArray() && obj2.isJsonArray()) {

            JsonArray obj1Array = obj1.getAsJsonArray();
            JsonArray obj2Array = obj2.getAsJsonArray();
            /**
             * if the string values of the array are not equal we are looking for the differences
             */
            if (!obj1Array.toString().equals(obj2Array.toString())) {
                JsonArray diffArray = new JsonArray();
                /**
                 * Check if there is the same number of item in the two JSONArray
                 */
                if (obj1Array.size() != obj2Array.size()) {
                    diffArray.add("The lengths of the arrays are not equals ");
                } else {
                    /**
                     * Recurcively call the function for each item of the JSONArray
                     */
                    Object obj = null;
                    for (int i = 0; i < obj1Array.size(); i++) {
                        obj = jsonsEqual(obj1Array.get(i), obj2Array.get(i));
                        JsonObject tmp = new JsonObject();
                        if (obj instanceof JsonObject && !obj.toString().equalsIgnoreCase("{}")) {
                            tmp.add(String.valueOf(i), (JsonObject) obj);
                            diffArray.add(tmp);
                        } else if ((!obj.toString().equalsIgnoreCase("{}")) || obj instanceof String) {
                            tmp.addProperty(String.valueOf(i), (String) obj);
                            diffArray.add(tmp);
                        }
                    }
                }
                /**
                 * return a JSONArray describing the difference between the two JSONArray
                 */
                if (diffArray.size() > 0) {
                    return diffArray;
                }
            }
        } else {
            /**
             * if the two object are neither JSONObject nor JSONArray object, the two object are instance on JSON
             * primitive types we check for direct equality between the two object
             */
            if (!obj1.equals(obj2)) {
                return "the two objects does not have the same value";
            }
        }
        /**
         * The two object are of privitive type and equal. An empty JSON object is returned.
         */
        return new JSONObject();
    }

    /**
     * List all the file contained a valid json file and compare them to the file existing in a given folder. The method
     * produces a json file in which file and zip entries are associated to boolean value. True if they are realy
     * present in the json file, false if not
     *
     * @param folderPath the folder where the link between files and json are checked
     * @return String that can be parsed into JSON
     * @throws IOException
     */
    public static String listLinkedFiles(String folderPath) throws IOException {

        /**
         * The Json Array to store information from
         */
        JsonArray files_linked = new JsonArray();

        /**
         * Folder containing the json files and the zip files
         */
        File folder = new File(folderPath);
        List<File> folderFiles = Arrays.asList(folder.listFiles());

        /**
         * Create a List of zip files and datafile linked to the json files in order to compare to what is really
         * present in the folder
         */
        Map<String, List<String>> fileFromJson = new HashMap<>();
        //Gson builder to build JSON from String object
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<File> jsonFiles = listFiles(folderPath, "json");
        for (File jsonFile : jsonFiles) {
            JsonObject json = gson.fromJson(readFile(jsonFile.getAbsolutePath(), StandardCharsets.UTF_8), JsonObject.class);
            JsonArray datasets = json.getAsJsonArray("datasets");
            for (JsonElement el : datasets) {
                JsonObject dataset = (JsonObject) el;
                String zipFileName = folder.getAbsolutePath() + "\\" + dataset.get("datasetId").getAsString() + ".zip";
                if (folderFiles.stream().filter((t) -> {
                    return t.getAbsolutePath().equals(zipFileName);
                }).findFirst().orElse(null) != null) {
                    List<String> dataFiles = new ArrayList<>();
                    dataset.getAsJsonArray("observations").forEach((t) -> {
                        JsonObject observation = (JsonObject) t;
                        dataFiles.add(observation.getAsJsonObject("result").getAsJsonObject("dataFile").get("name").getAsString());
                    });
                    fileFromJson.put(zipFileName, dataFiles);
                }
            }
        }

        /**
         * List the file that are present in the folder and compare them to fileFromJson Map object.
         */
        for (File file : folderFiles) {
            //Exclude the json file
            if (!"json".equals(FilenameUtils.getExtension(file.getAbsolutePath()))) {
                //check the zip files
                if ("zip".equals(FilenameUtils.getExtension(file.getAbsolutePath()))) {
                    //check if the zip file frrom the folder was found in the json
                    if (fileFromJson.containsKey(file.getAbsolutePath())) {
                        //if the file is found we check that the entry of the file are found in the json
                        //zip entries are checked and stored in a JsonArray object
                        JsonArray jsonZipEntries = new JsonArray();
                        List<String> jsonDataFiles = fileFromJson.get(file.getAbsolutePath());
                        try (ZipFile zip = new ZipFile(file.getAbsolutePath());) {
                            //List all the entries of the zip file
                            Enumeration<? extends ZipEntry> entries = zip.entries();
                            while (entries.hasMoreElements()) {
                                ZipEntry zipEntry = entries.nextElement();
                                if (jsonDataFiles.stream().filter((t) -> {
                                    return t.equals(zipEntry.getName());
                                }).findAny().orElse(null) != null) {
                                    //If the entry file of the zip file is found in the json we add the entry file name (key)
                                    // as "true" (value) in the the file_linked.json return file
                                    JsonObject tmp = new JsonObject();
                                    tmp.addProperty(zipEntry.getName(), Boolean.TRUE);
                                    jsonZipEntries.add(tmp);
                                } else {
                                    //If the entry file of the zip file is not found in the json we add the entry file name (key)
                                    // as "false" (value) in the the file_linked.json return file
                                    JsonObject tmp = new JsonObject();
                                    tmp.addProperty(zipEntry.getName(), Boolean.FALSE);
                                    jsonZipEntries.add(tmp);
                                }
                            }
                            //Store the jsonZipEntries array in a json object named according the zip file name
                            JsonObject tmp2 = new JsonObject();
                            tmp2.add(file.getAbsolutePath(), jsonZipEntries);
                            files_linked.add(tmp2);
                        } catch (IOException ex) {
                            //if the zip file cannot be opened, the zip file name is set to false in the file_linked.json return file
                            JsonObject tmp = new JsonObject();
                            tmp.addProperty(file.getAbsolutePath(), Boolean.FALSE);
                            files_linked.add(tmp);
                        }
                    } else {
                        //if the zip file name is not found in the json the zip file name is set to false in the file_linked.json return file
                        JsonObject tmp = new JsonObject();
                        tmp.addProperty(file.getAbsolutePath(), Boolean.FALSE);
                        files_linked.add(tmp);
                    }
                } else {
                    //if the file name is not a zip file, the file name is set to false in the file_linked.json return file
                    JsonObject tmp = new JsonObject();
                    tmp.addProperty(file.getAbsolutePath(), Boolean.FALSE);
                    files_linked.add(tmp);
                }
            }
        }
        return files_linked.toString();
    }

    /**
     * Recursive method that remove all key/value pair that is subject to internationalisation
     *
     * @param jsonToValidate
     * @param configJson
     * @return JsonElement without internationalised key/value pair
     */
    public static JsonElement removeMultiLangKey(JsonElement jsonToValidate, JsonObject configJson) {

        String type = null;

        type = configJson.get("type").getAsString();

        switch (type) {
            case "object":

                JsonObject properties = configJson.getAsJsonObject("properties");
                for (String key : properties.keySet()) {
                    if (jsonToValidate.getAsJsonObject().has(key)) {
                        JsonElement tmpObject = jsonToValidate.getAsJsonObject().get(key);
                        tmpObject = removeMultiLangKey(jsonToValidate.getAsJsonObject().get(key),
                                configJson.getAsJsonObject("properties").getAsJsonObject(key));
                        jsonToValidate.getAsJsonObject().remove(key);
                        jsonToValidate.getAsJsonObject().add(key, tmpObject);
                    }
                }
                break;

            case "array":
                for (int i = 0; i < jsonToValidate.getAsJsonArray().size(); i++) {
                    JsonElement tmpArray = jsonToValidate.getAsJsonArray().get(i);
                    tmpArray = removeMultiLangKey(jsonToValidate.getAsJsonArray().get(i),
                            configJson.getAsJsonObject("items"));
                    jsonToValidate.getAsJsonArray().set(i, tmpArray);
                }
                break;

            case "string":
                jsonToValidate = null;
        }
        return jsonToValidate;
    }

}
