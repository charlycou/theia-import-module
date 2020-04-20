/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if the json file name does not follow the name pattern
 * @author coussotc
 */
public class InvalidJsonFileNameException extends Exception{
    
    private final String jsonFileName;

    public InvalidJsonFileNameException(String jsonFileName) {
        this.jsonFileName = jsonFileName;
    }
    
    @Override
    public String getMessage() {
        return jsonFileName+" does not match the json file name pattern.\n";
    }
}
