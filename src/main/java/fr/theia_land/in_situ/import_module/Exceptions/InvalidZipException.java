/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if a zip file is not readable
 * @author coussotc
 */
public class InvalidZipException extends Exception {
    private final String ZipFileName;
    
    public InvalidZipException(String zipFileName) {
        this.ZipFileName=zipFileName;
    }
    
    @Override
    public String getMessage(){
        return ZipFileName + " is not readable or is not a valid zip file.\n";
    }
}
