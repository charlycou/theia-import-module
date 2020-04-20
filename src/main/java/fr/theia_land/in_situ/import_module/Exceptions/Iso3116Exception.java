/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

/**
 *
 * @author coussotc
 */
public class Iso3116Exception extends Exception {

    private final String code;

    public Iso3116Exception(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return "The country Iso3116 code: " + this.code + " does not exist or the service is unavailable.";
    }
}
