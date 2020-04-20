/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown when the encoding of a String file is not UTF-8
 * @author coussotc
 */
public class InvalidEncodingException extends Exception {


    private final String zipFile;
    private final String txtFile;
    private final String encoding;

    
    public InvalidEncodingException (String txtFile, String zipFile, String encoding) {
        this.zipFile = zipFile;
        this.txtFile = txtFile;
        this.encoding = encoding;
    }
    
    @Override
    public String getMessage() {
        return "The encoding of the text file is not UTF-8 but "+ encoding
                + "\n"
                + "ZIP file name : " + zipFile +"\n"
                + "Text file name : " + txtFile +"\n";
    }
}
