/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if a line of a csv file does not follow the defined pattern
 * @author coussotc
 */
public class InvalidTextFileFormatException extends Exception {

    private final String line;
    private final String zipFile;
    private final String txtFile;
    private final int cpt;

    public InvalidTextFileFormatException(String line, int cpt, String txtFile, String zipFile) {
        this.line = line;
        this.zipFile = zipFile;
        this.txtFile = txtFile;
        this.cpt =cpt;
    }
    
    @Override
    public String getMessage() {
        return "The text file does not match the defined pattern at line : " + cpt + "\n"
                + "ZIP file name : " + zipFile +"\n"
                + "Text file name : " + txtFile +"\n"
                + "line : " + line + "\n";
    }  
}
