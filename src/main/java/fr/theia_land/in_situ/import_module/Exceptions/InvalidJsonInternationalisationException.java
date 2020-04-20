/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if a field that should be identical between two json file of different language is different
 * @author coussotc
 */
public class InvalidJsonInternationalisationException extends Exception{
    private String diff;
    private String fileName1, fileName2; 

    public InvalidJsonInternationalisationException(String diff, String fileName1, String fileName2) {
        this.diff = diff;
        this.fileName1 = fileName1;
        this.fileName2 = fileName2;
    }
    
    @Override
    public String getMessage() {
        return "The files " + fileName1 + " and " + fileName2 +" differ according to the following pattern:\n " + diff;
    }
    
}
