/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if the name of a text file does not follow the defined pattern
 * @author coussotc
 */
public class InvalidTextFileNameException extends Exception {
    
    private final String zipFile;
    private final String txtFile;

    public InvalidTextFileNameException(String zipFile, String txtFile) {
        this.zipFile = zipFile;
        this.txtFile = txtFile;
    }
    
    @Override
    public String getMessage() {
        return "The file " + txtFile + " was not found or does not corresponds to Json Datafile name.\n"
                + "ZIP file : " + zipFile +" \n";
    }
}
