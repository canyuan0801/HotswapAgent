
package org.hotswap.agent.plugin.proxy.hscglib;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.hotswap.agent.logging.AgentLogger;


public class GeneratorParametersRecorder {

    public static ConcurrentHashMap<String, GeneratorParams> generatorParams = new ConcurrentHashMap<>();
    private static AgentLogger LOGGER = AgentLogger
            .getLogger(GeneratorParametersRecorder.class);


    public static void register(Object generatorStrategy, Object classGenerator,
            byte[] bytes) {
        try {
            generatorParams.putIfAbsent(getClassName(bytes),
                    new GeneratorParams(generatorStrategy, classGenerator));
        } catch (Exception e) {
            LOGGER.error(
                    "Error saving parameters of a creation of a Cglib proxy",
                    e);
        }
    }


    public static String getClassName(byte[] bytes) throws Exception {
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(bytes));
        dis.readLong();
        int cpcnt = (dis.readShort() & 0xffff) - 1;
        int[] classes = new int[cpcnt];
        String[] strings = new String[cpcnt];
        for (int i = 0; i < cpcnt; i++) {
            int t = dis.read();
            if (t == 7)
                classes[i] = dis.readShort() & 0xffff;
            else if (t == 1)
                strings[i] = dis.readUTF();
            else if (t == 5 || t == 6) {
                dis.readLong();
                i++;
            } else if (t == 8)
                dis.readShort();
            else
                dis.readInt();
        }
        dis.readShort();
        return strings[classes[(dis.readShort() & 0xffff) - 1] - 1].replace('/',
                '.');
    }
}
