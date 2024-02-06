
package org.hotswap.agent.plugin.proxy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;


public class JDIRedefiner implements Redefiner {

    private static final String PORT_ARGUMENT_NAME = "port";
    private static final String TRANSPORT_NAME = "dt_socket";

    private VirtualMachine vm;


    public static final int PORT = 4000;

    public JDIRedefiner(int port) throws IOException {
        vm = connect(port);
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    private VirtualMachine connect(int port) throws IOException {
        VirtualMachineManager manager = Bootstrap.virtualMachineManager();


        List<AttachingConnector> connectors = manager.attachingConnectors();
        AttachingConnector chosenConnector = null;
        for (AttachingConnector c : connectors) {
            if (c.transport().name().equals(TRANSPORT_NAME)) {
                chosenConnector = c;
                break;
            }
        }
        if (chosenConnector == null) {
            throw new IllegalStateException("Could not find socket connector");
        }


        AttachingConnector connector = chosenConnector;
        Map<String, Argument> defaults = connector.defaultArguments();
        Argument arg = defaults.get(PORT_ARGUMENT_NAME);
        if (arg == null) {
            throw new IllegalStateException("Could not find port argument");
        }
        arg.setValue(Integer.toString(port));


        try {
            System.out.println("Connector arguments: " + defaults);
            return connector.attach(defaults);
        } catch (IllegalConnectorArgumentsException e) {
            throw new IllegalArgumentException("Illegal connector arguments",
                    e);
        }
    }

    public void disconnect() {
        if (vm != null) {
            vm.dispose();
            vm = null;
        }
    }

    public void redefineClasses(Map<Class<?>, byte[]> classes) {
        refreshAllClasses();
        List<ReferenceType> references = vm.allClasses();

        Map<ReferenceType, byte[]> map = new HashMap<ReferenceType, byte[]>(
                classes.size());
        for (Map.Entry<Class<?>, byte[]> entry : classes.entrySet()) {
            map.put(findReference(references, entry.getKey().getName()),
                    entry.getValue());
        }
        vm.redefineClasses(map);
    }


    private void refreshAllClasses() {
        try {
            Field f = vm.getClass().getDeclaredField("retrievedAllTypes");
            f.setAccessible(true);
            f.set(vm, false);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(HotSwapTool.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(HotSwapTool.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(HotSwapTool.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(HotSwapTool.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    private static ReferenceType findReference(List<ReferenceType> list,
            String name) {
        for (ReferenceType ref : list) {
            if (ref.name().equals(name)) {
                return ref;
            }
        }
        throw new IllegalArgumentException(
                "Cannot find corresponding reference for class name '" + name
                        + "'");
    }
}
