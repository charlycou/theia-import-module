/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.FileValidation;

import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.DAO.ObservationDocumentsCreation;
import fr.theia_land.in_situ.import_module.Validation.JsonFileValidation;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonFileNameException;
import fr.theia_land.in_situ.import_module.Exceptions.InvalidJsonInternationalisationException;
import fr.theia_land.in_situ.import_module.Exceptions.JsonFileNotFoundException;
import fr.theia_land.in_situ.import_module.Exceptions.TheiaCategoriesException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author coussotc
 */
//@SpringBootTest(properties = {"json.schema.url=http://in-situ.theia-land.fr/json-schema/"})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { JsonFileValidation.class } ,webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableConfigurationProperties
public class JsonFileValidationTest {

//    @Value("${json.schema.url}")
//    private String url;

    @Autowired
    private JsonFileValidation jsonFileValidation;

//    public JsonFileValidationTest() {
//    }
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * -- valid json file name Test of validateJsonFileNames method, of class JsonFileValidation. For one valid file
     * name in the folder
     */
    @Test(expected = Test.None.class)
    public void testValidateJsonFileNames() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);

    }

    /**
     * Test of validateJsonFileNames method, of class JsonFileValidation. For two valid file names in the folder
     */
    @Test(expected = Test.None.class)
    public void testValidateJsonFileNames2Files() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        File tmpFile2 = new File(unzipFolder, "CATC_fr.json");
        InputStream in2 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in2, tmpFile2);
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);

    }

    /**
     * Test of validateJsonFileNames method, of class JsonFileValidation. For one file in the folder with an invalid
     * name
     */
    @Test(expected = InvalidJsonFileNameException.class)
    public void testInvalidateJsonFileNames() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATCH_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);
    }

    /**
     * -- wrong json file name: does not correspond to the folder name Test of validateJsonFileNames method, of class
     * JsonFileValidation. For one file in the folder with an invalid name
     */
    @Test(expected = InvalidJsonFileNameException.class)
    public void testInvalidateJsonFileNamesDiffFolderName() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "MSEC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);
    }

    /**
     * -- wrong json file name: wrong name for one out of two json Test of validateJsonFileNames method, of class
     * JsonFileValidation. For one out of two files in the folder with an invalid name
     */
    @Test(expected = InvalidJsonFileNameException.class)
    public void testInvalidateJson2FileNames() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATCH_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        File tmpFile2 = new File(unzipFolder, "CATC_en.json");
        InputStream in2 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in2, tmpFile2);
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);
    }

    /**
     * -- empty folder Test of validateJsonFileNames method, of class JsonFileValidation. For an empty folder
     */
    @Test(expected = JsonFileNotFoundException.class)
    public void testInvalidateJsonFileNamesEmpty() throws Exception {
        System.out.println("validateJsonFileNames");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        String folderPath = unzipFolder.getPath();
        jsonFileValidation.validateJsonFileNames(folderPath);
    }

    /**
     * -- valid json schema Test of validateSchema method, of class SchemaValidator. for a valid json file with the
     * valid format expected result: nothing
     */
    @Test(expected = Test.None.class)
    public void testValidateSchemaForValidFile() throws Exception {
        //System.out.println(url);
        System.out.println("validateSchema");
        File tmpCATCFolder = folder.newFolder("CATCH");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        StringBuilder builder = new StringBuilder();
        inputStreamToFile(in, tmpFile);
        String pathToJson = tmpFile.getPath();
        jsonFileValidation.validateJsonFileFormat(pathToJson);
    }

    /**
     * -- Wrong JSON format according to RFC8259 Test of validateSchema method, of class SchemaValidator. for a invalid
     * json file expected result the method throw a JSONException
     */
    @Test(expected = com.google.gson.JsonSyntaxException.class)
    public void testValidateSchemaForInvalidJsonFile() throws Exception {
        System.out.println("validateSchema");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(tmpCATCFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_invalidJsonFile.json");
        inputStreamToFile(in, tmpFile);
        String pathToJson = tmpFile.getPath();
        jsonFileValidation.validateJsonFileFormat(pathToJson);
    }

    /**
     * --valid JSON format but wrong Json schema Test of validateSchema method, of class SchemaValidator. for a valid
     * json file with invalid JsonFormat expected result the method throw a InvalidJsonException
     */
    @Test(expected = InvalidJsonException.class)
    public void testValidateSchemaForInvalidJsonFormat() throws Exception {
        System.out.println("validateSchema");
        File tmpCATCFolder = folder.newFolder("CATCH");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_invalidFormat.json");
        inputStreamToFile(in, tmpFile);
        String pathToJson = tmpFile.getPath();
        jsonFileValidation.validateJsonFileFormat(pathToJson);
    }

    /**
     * -- Valid json files for internationalisation Test of validateJsonInternationalisation method, of class
     * JsonFileValidation. Expected result: nothing
     */
    @Test(expected = Test.None.class)
    public void testValidateJsonInternationalisation() throws Exception {
        System.out.println("validateJsonInternationalisation");
        File tmpCATCFolder = folder.newFolder("CATCH");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile1 = new File(unzipFolder, "CATC_en.json");
        InputStream in1 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in1, tmpFile1);
        File tmpFile2 = new File(unzipFolder, "CATC_fr.json");
        InputStream in2 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_fr.json");
        inputStreamToFile(in2, tmpFile2);
        List<File> jsonFiles = new ArrayList<>();
        jsonFiles.add(tmpFile1);
        jsonFiles.add(tmpFile2);
        jsonFileValidation.validateJsonInternationalisation(jsonFiles);
    }

    /**
     * -- wrong json files : the two json files do not have the same values Test of validateJsonInternationalisation
     * method, of class JsonFileValidation. The two json do not have the same value Expected result: throw a
     * InvalidJsonInternationalisationException
     */
    @Test(expected = InvalidJsonInternationalisationException.class)
    public void testInvalidateJsonInternationalisation() throws Exception {
        System.out.println("validateJsonInternationalisation");
        File tmpCATCFolder = folder.newFolder("CATCH");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile1 = new File(unzipFolder, "CATC_en.json");
        InputStream in1 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in1, tmpFile1);
        File tmpFile2 = new File(unzipFolder, "CATC_fr.json");
        InputStream in2 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_fr_wrongValue.json");
        inputStreamToFile(in2, tmpFile2);
        List<File> jsonFiles = new ArrayList<>();
        jsonFiles.add(tmpFile1);
        jsonFiles.add(tmpFile2);
        jsonFileValidation.validateJsonInternationalisation(jsonFiles);
    }

    /**
     * -- wrong json files : the two json files do not have the same fields Test of validateJsonInternationalisation
     * method, of class JsonFileValidation. the two json does not have the same fields Expected result: throw a
     * InvalidJsonInternationalisationException
     */
    @Test(expected = InvalidJsonInternationalisationException.class)
    public void testInvalidateJsonInternationalisationWrongField() throws Exception {
        System.out.println("validateJsonInternationalisation");
        File tmpCATCFolder = folder.newFolder("CATCH");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile1 = new File(unzipFolder, "CATC_en.json");
        InputStream in1 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in1, tmpFile1);
        File tmpFile2 = new File(unzipFolder, "CATC_fr.json");
        InputStream in2 = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_fr_wrongField.json");
        inputStreamToFile(in2, tmpFile2);
        List<File> jsonFiles = new ArrayList<>();
        jsonFiles.add(tmpFile1);
        jsonFiles.add(tmpFile2);
        jsonFileValidation.validateJsonInternationalisation(jsonFiles);
    }

    @Test(expected = Test.None.class)
    public void testValidateTheiaCategories() throws Exception {
        System.out.println("validateTheiaCategories");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);

        List<String> uris = new ArrayList<>();
        String queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
                + "SELECT ?s\n"
                + "FROM <https://w3id.org/ozcar-theia/>\n"
                + "WHERE {\n"
                + "  <https://w3id.org/ozcar-theia/variableCategoriesGroup> skos:member ?s .\n"
                + "  MINUS {?s skos:narrower ?o}\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.createServiceRequest("http://in-situ.theia-land.fr:3030/theia_vocabulary/", query);) {
            ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                uris.add(rs.next().get("s").toString());
            }
        }

        JsonObject json = ObservationDocumentsCreation.readJsonFile(tmpFile);
        JsonFileValidation.validateTheiaCategories(json.getAsJsonArray("datasets").get(0)
                .getAsJsonObject().getAsJsonArray("observations").get(0).getAsJsonObject().getAsJsonObject("observedProperty")
                .getAsJsonArray("theiaCategories"), uris);
    }

    @Test(expected = TheiaCategoriesException.class)
    public void testValidateWrongTheiaCategories() throws Exception {
        System.out.println("validateTheiaCategories");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_wrong_category.json");
        inputStreamToFile(in, tmpFile);

        List<String> uris = new ArrayList<>();
        String queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
                + "SELECT ?s\n"
                + "FROM <https://w3id.org/ozcar-theia/>\n"
                + "WHERE {\n"
                + "  <https://w3id.org/ozcar-theia/variableCategoriesGroup> skos:member ?s .\n"
                + "  MINUS {?s skos:narrower ?o}\n"
                + "}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qExec = QueryExecutionFactory.createServiceRequest("http://in-situ.theia-land.fr:3030/theia_vocabulary/", query);) {
            ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                uris.add(rs.next().get("s").toString());
            }
        }

        JsonObject json = ObservationDocumentsCreation.readJsonFile(tmpFile);
        jsonFileValidation.validateTheiaCategories(json.getAsJsonArray("datasets").get(0)
                .getAsJsonObject().getAsJsonArray("observations").get(0).getAsJsonObject().getAsJsonObject("observedProperty")
                .getAsJsonArray("theiaCategories"), uris);
    }

    private static void inputStreamToFile(InputStream in, File file) {
        try (OutputStream out = new FileOutputStream(file)) {
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
