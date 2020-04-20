/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 * Thrown if the json file validation using the json schema failed
 * @author coussotc
 */
public class InvalidJsonException extends Exception{
    private final String validationOutput;

    public InvalidJsonException(String validationOutput) {
        this.validationOutput = validationOutput;
    }
    
    @Override
    public String getMessage(){
        return "Validation of JSON file format failed:\n"
                + validationOutput;
    }
}
