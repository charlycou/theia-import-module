/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.DAO;

import fr.theia_land.in_situ.import_module.FileValidation.JsonFileValidationTest;
import fr.theia_land.in_situ.import_module.Validation.Utils.EnrichmentUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author coussotc
 */
public class ObservationDocumentsCreationTest {

    public ObservationDocumentsCreationTest() {
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
     * Test of listJsonFiles method, of class ObservationDocumentsCreation.
     */
    @Test
    public void testListJsonFiles() throws IOException {
        System.out.println("listJsonFiles");

        File tmpCATCFolder = folder.newFolder("CATC");
        File unzipFolder = new File(tmpCATCFolder.getAbsolutePath() + "/tmp/123/unizp/");
        unzipFolder.mkdirs();
        File tmpFile = new File(unzipFolder, "CATC_en.json");
        InputStream in = JsonFileValidationTest.class.getClassLoader().getResourceAsStream("json/CATC_en.json");
        inputStreamToFile(in, tmpFile);
        ArrayList<File> result = ObservationDocumentsCreation.listJsonFiles(unzipFolder.getPath());
        assertEquals(result.size(), 1);
    }

    /**
     * Test of getScanRMetadata method, of class ObservationDocumentsCreation.
     */
    @Test(expected = IOException.class)
    public void testGetScanRMetadata() throws Exception {
        System.out.println("getScanRMetadata");
        String idScanR = "azdazda";
        EnrichmentUtils.getScanRMetadata(idScanR);
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
