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
public class TheiaCategoriesException extends Exception {

    private final String uri;

    public TheiaCategoriesException(String uri) {
        this.uri = uri;
    }

    @Override
    public String getMessage() {
        return "Theia Categories " + this.uri + " does not exists in the thesaurus or is not a leaf of the category tree.";
    }
}
