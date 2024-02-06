
package org.hotswap.agent.plugin.weld_jakarta.command;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BdaAgentRegistry {

    
    private static Map<String, BeanClassRefreshAgent> INSTANCES = new ConcurrentHashMap<>();

    public static Map<String, BeanClassRefreshAgent> getInstances() {
        return INSTANCES;
    }

    public static boolean contains(String archivePath) {
        return INSTANCES.containsKey(archivePath);
    }

    public static void put(String archivePath, BeanClassRefreshAgent bdaAgent) {
        INSTANCES.put(archivePath, bdaAgent);
    }

    public static BeanClassRefreshAgent get(String archivePath) {
        return INSTANCES.get(archivePath);
    }

    public static Collection<BeanClassRefreshAgent> values() {
        return INSTANCES.values();
    }

    
    public static String getArchiveByClassName(String className){
        for(BeanClassRefreshAgent agent: INSTANCES.values()) {
            if(agent.getDeploymentArchive().getBeanClasses().contains(className)) {
                return agent.getArchivePath();
            }
        }
        return null;
    }
}
