/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.theia_land.in_situ.import_module.model.POJO;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author coussotc
 */
public class ImportConfig {

    private String[] languages;
    Map<String,Map<String,Boolean>> linkTowardDatafile;

    public ImportConfig(Map<String,Map<String,Boolean>> linkTowardDatafile, List<File> jsonFiles) {
        this.linkTowardDatafile = linkTowardDatafile;
        getLanguagesFromJsonFileNames(jsonFiles);
    }

    public String[] getLanguages() {
        return languages;
    }

    public Map<String, Map<String, Boolean>> getLinkTowardDatafile() {
        return linkTowardDatafile;
    }
    

    public boolean isLinkTowardDatafile(String lang, String observationId) {
        return this.linkTowardDatafile.get(lang).get(observationId);
    }

    private void getLanguagesFromJsonFileNames(List<File> jsonFiles) {
        String regexLanguages = "(?:[A-Z]{4}_)(en|fr|es)(?:\\.json)";
        this.languages = new String[jsonFiles.size()];
        Pattern languagesPattern = Pattern.compile(regexLanguages);
        for (int i = 0; i < this.languages.length; i++) {
            Matcher m = languagesPattern.matcher(jsonFiles.get(i).getName());
            if (m.find()) {
                this.languages[i] = m.group(1);
            }
        }
    }
}
