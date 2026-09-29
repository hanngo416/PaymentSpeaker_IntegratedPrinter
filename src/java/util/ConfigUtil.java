/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author PC
 */
public final class ConfigUtil {

    private ConfigUtil() {
    }

    public static Properties load(String fileName) {
        Properties props = new Properties();
        try (InputStream in = ConfigUtil.class.getResourceAsStream("/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException(fileName + " not found. Copy " + fileName
                        + ".example to " + fileName + " in src/java and fill it in.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + fileName, e);
        }
        return props;
    }
}
