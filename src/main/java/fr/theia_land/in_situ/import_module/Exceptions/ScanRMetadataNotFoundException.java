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
public class ScanRMetadataNotFoundException extends Exception{

    private final String scanRiD;

    public ScanRMetadataNotFoundException(String scanRiD) {
        this.scanRiD = scanRiD;
    }
    
    @Override
    public String getMessage() {
       return "scanR ID: " + this.scanRiD +" - unable to retrieve metadata for this scanR id. This id does not exist or scanR API is not available.\n " ;
    }
    
}

