/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Validation.Utils;

import com.google.gson.JsonObject;
import fr.theia_land.in_situ.import_module.DAO.ObservationDocumentsCreation;
import fr.theia_land.in_situ.import_module.Exceptions.ScanRMetadataNotFoundException;
import fr.theia_land.in_situ.import_module.FileValidation.JsonFileValidationTest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author coussotc
 */
public class EnrichmentUtilsTest {

    public EnrichmentUtilsTest() {
    }

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
     * Test of enrichmentUsingScanR method, of class EnrichmentUtils.
     */
     @Test(expected = ScanRMetadataNotFoundException.class)
    public void testEnrichmentUsingScanR() throws Exception {
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_invalid_scanrId_iso3166.json");
        inputStreamToFile(in, tmpFile);
        JsonObject json = ObservationDocumentsCreation.readJsonFile(tmpFile);
        EnrichmentUtils.enrichmentUsingScanR(json.getAsJsonObject("producer").getAsJsonArray("fundings").get(0).getAsJsonObject());
    }

    /**
     * Test of enrichmentUsingIso3166 method, of class EnrichmentUtils.
     */
    @Test(expected = IOException.class)
    public void testEnrichmentUsingIso3166() throws Exception {
        System.out.println("enrichmentUsingIso3166");
        System.out.println("populateFundingCountry");
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_invalid_scanrId_iso3166.json");
        inputStreamToFile(in, tmpFile);
        JsonObject json = ObservationDocumentsCreation.readJsonFile(tmpFile);
        JsonObject result = EnrichmentUtils.enrichmentUsingIso3166(json.getAsJsonObject("producer").getAsJsonArray("fundings").get(0).getAsJsonObject());
    }
    
        /**
     * Test of getFundingsUsingScanR method, of class ObservationDocumentsCreation.
     * The scanR id is wrong and the funder is not added to fundings list but the import process is not stopped
     */
    @Test(expected = Test.None.class)
    public void testGetFundingsUsingScanR() throws IOException {
        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en_invalid_scanrId_iso3166.json");
        inputStreamToFile(in, tmpFile);
        EnrichmentUtils.setFundingsUsingScanR(ObservationDocumentsCreation.readJsonFile(tmpFile).getAsJsonObject("producer").getAsJsonArray("fundings"));
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
