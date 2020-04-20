/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.Exceptions;

import java.io.FileNotFoundException;

/**
 * Thrown if no zip file is found in a folder.
 * @author coussotc
 */
public class ZipFileNotFoundException extends FileNotFoundException {

    @Override
    public String getMessage() {
        return "ZIP data files not found in the folder.\n";
    }
}
