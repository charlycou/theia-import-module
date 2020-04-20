/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if a string of an id does not follow the specified pattern for ids
 * @author coussotc
 */
public class InvalidIdsException extends Exception{
    private final String id;

    public InvalidIdsException(String id) {
        this.id = id;
    }
    
    @Override
    public String getMessage() {
       return "ID: " + this.id +" - does not follow the specified pattern.\n " ;
    }
    
}
