
package org.hotswap.agent.plugin.ibatis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;
import org.springframework.core.io.Resource;

import com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapParser;
import com.ibatis.sqlmap.engine.builder.xml.XmlParserState;


public class IBatisConfigurationHandler {
    private static AgentLogger LOGGER = AgentLogger.getLogger(IBatisConfigurationHandler.class);
    private static Resource[] configLocations;
    private static Resource[] mappingLocations;
    private static Properties properties;
    private static SqlMapConfigParser sqlMapConfigParser;
    private static XmlParserState parserState;


    public static void setSqlMapConfigParser(SqlMapConfigParser parser) {
        if(sqlMapConfigParser==null) {
            sqlMapConfigParser=parser;
            LOGGER.info("Set ibatis sql map config parser -> "+parser);
        }
    }


    public static void setMapFiles(Resource[] configLocationsArg,Resource[] mappingLocationsArg,Properties propertiesArg) {
        configLocations=configLocationsArg;
        mappingLocations=mappingLocationsArg;
        properties=propertiesArg;
        LOGGER.info("Set ibatis config files -> "+configLocations+","+mappingLocations+","+properties);
    }


    public static void setParserState(XmlParserState state) {
        if(parserState==null) {
            parserState=state;
            LOGGER.info("Set ibatis parser state -> "+state);
        }
    }


    public static String toPath(Resource[]res) throws IOException {
        StringBuilder phs=new StringBuilder();
        for(int i=0;i<res.length;i++) {
            if(i!=0) phs.append("\n");
            phs.append(res[i].getURL().getPath());
        }
        return phs.toString();
    }


    public static void refresh() {
        LOGGER.info("Ibatis sql map refresh ...");
        parserState.getSqlIncludes().clear();
        ReflectionHelper.invoke(parserState.getConfig().getDelegate(), IBatisTransformers.REFRESH_METHOD);
        for (Resource configLocation : configLocations) {
            try {
                InputStream is = configLocation.getInputStream();
                sqlMapConfigParser.parse(is, properties);
            }catch (Exception ex) {
                LOGGER.error("Failed to parse config resource: " + configLocation, ex.getCause());
            }
        }
        SqlMapParser parser=new SqlMapParser(parserState);
        for (Resource mappingLocation : mappingLocations) {
            try {
                parser.parse(mappingLocation.getInputStream());
            }catch (Exception ex) {
                LOGGER.error("Failed to parse sql map resource: " + mappingLocation, ex.getCause());
            }
        }
        LOGGER.info("Ibatis sql map refresh successful!!!");
    }
}
