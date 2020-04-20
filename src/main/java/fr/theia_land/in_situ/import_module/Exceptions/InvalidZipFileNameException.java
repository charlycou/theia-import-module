/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if a zip file name does not follow the defined pattern
 * @author coussotc
 */
public class InvalidZipFileNameException extends Exception{
    private final String zipFile;

    public InvalidZipFileNameException(String zipFile) {
        this.zipFile = zipFile;
    }
    
    @Override
    public String getMessage() {
        return "Zip file "+ zipFile + " not found.\n";
    }
}
