/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.lollkosk.flipplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Wrapper for translation strings collection.
 * @author Marek Zuzi
 */
public class Translation {
    private static final String DEFAULT_LANG = "cs";
    
    private static final HashMap<String, Properties> languages = new HashMap<>();
    
    /**
     * Initializes the translation bundle.
     */
    public static void init() {
        languages.clear();
        
        File langDir = new File("lang");
        if(!langDir.isDirectory()) {
            System.err.println("Could not load language config from directory " + langDir.getAbsolutePath());
            return;
        }
        
        for(File langFile : langDir.listFiles()) {
            if(langFile.getName().endsWith(".properties")) {
                try (FileInputStream input = new FileInputStream(langFile);) {
                    Properties props = new Properties();
                    // load a properties file
                    props.load(input);
                    languages.put(langFile.getName().substring(0, langFile.getName().length() - 11), props);
                } catch (IOException ex) {
                    System.err.println("Could not load language from " + langFile.getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * Gets the translated string by key and language.
     * @param key
     * @param language
     * @return 
     */
    public static String T(String key, String language) {
        Properties l = null;
        if(languages.containsKey(language)) {
            l = languages.get(language);
        } else if(!language.equals(DEFAULT_LANG) && languages.containsKey(DEFAULT_LANG)) {
            l = languages.get(DEFAULT_LANG);
        }
        
        if(l == null) {
            return "Error: Can not find suitable language: " + language;
        }
        
        String s = l.getProperty(key);
        if(l == null || l.isEmpty()) {
            return "Error: Can not find requested string: " + key;
        } else {
            try {
                return new String(s.getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return s;
            }
        }
    }
    
    public static String T(String key) {
        return T(key, DEFAULT_LANG);
    }
}
