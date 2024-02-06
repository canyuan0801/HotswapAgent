
package org.hotswap.agent.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class Version {


    public static String version() {
        try {
            Properties prop = new Properties();
            InputStream in = Version.class.getResourceAsStream("/version.properties");
            prop.load(in);
            in.close();

            return prop.getProperty("version") == null ? "unkown" : prop.getProperty("version");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
