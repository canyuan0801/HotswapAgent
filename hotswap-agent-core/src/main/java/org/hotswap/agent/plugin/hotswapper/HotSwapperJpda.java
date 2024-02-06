


package org.hotswap.agent.plugin.hotswapper;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.LoaderClassPath;

import java.io.IOException;
import java.util.*;

class Trigger {
    void doSwap() {
    }
}


public class HotSwapperJpda {
    private VirtualMachine jvm;
    private MethodEntryRequest request;
    private Map<ReferenceType,byte[]> newClassFiles;

    private Trigger trigger;

    private static final String HOST_NAME = "localhost";
    private static final String TRIGGER_NAME = Trigger.class.getName();


    public HotSwapperJpda(int port)
            throws IOException, IllegalConnectorArgumentsException {
        this(Integer.toString(port));
    }


    public HotSwapperJpda(String port)
            throws IOException, IllegalConnectorArgumentsException {
        jvm = null;
        request = null;
        newClassFiles = null;
        trigger = new Trigger();
        AttachingConnector connector
            = (AttachingConnector)findConnector("com.sun.jdi.SocketAttach");

        Map<String,Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue(HOST_NAME);
        arguments.get("port").setValue(port);
        jvm = connector.attach(arguments);
        EventRequestManager manager = jvm.eventRequestManager();
        request = methodEntryRequests(manager, TRIGGER_NAME);
    }

    private Connector findConnector(String connector) throws IOException {
        List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();

        for (Connector con:connectors)
            if (con.name().equals(connector))
                return con;

        throw new IOException("Not found: " + connector);
    }

    private static MethodEntryRequest methodEntryRequests(
            EventRequestManager manager,
            String classpattern) {
        MethodEntryRequest mereq = manager.createMethodEntryRequest();
        mereq.addClassFilter(classpattern);
        mereq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        return mereq;
    }


    @SuppressWarnings("unused")
    private void deleteEventRequest(EventRequestManager manager,
                                    MethodEntryRequest request) {
        manager.deleteEventRequest(request);
    }


    public void reload(String className, byte[] classFile) {
        ReferenceType classtype = toRefType(className);
        Map<ReferenceType,byte[]> map = new HashMap<ReferenceType,byte[]>();
        map.put(classtype, classFile);
        reload2(map, className);
    }


    public void reload(Map<String,byte[]> classFiles) {
        Map<ReferenceType,byte[]> map = new HashMap<ReferenceType,byte[]>();
        String className = null;
        for (Map.Entry<String,byte[]> e:classFiles.entrySet()) {
            className = e.getKey();
            map.put(toRefType(className), e.getValue());
        }

        if (className != null)
            reload2(map, className + " etc.");
    }

    private ReferenceType toRefType(String className) {
        List<ReferenceType> list = jvm.classesByName(className);
        if (list == null || list.isEmpty())
            throw new RuntimeException("no such class: " + className);
        return list.get(0);
    }

    private void reload2(Map<ReferenceType,byte[]> map, String msg) {
        synchronized (trigger) {
            startDaemon();
            newClassFiles = map;
            request.enable();
            trigger.doSwap();
            request.disable();
            Map<ReferenceType,byte[]> ncf = newClassFiles;
            if (ncf != null) {
                newClassFiles = null;
                throw new RuntimeException("failed to reload: " + msg);
            }
        }
    }

    private void startDaemon() {
        new Thread() {
            private void errorMsg(Throwable e) {
                System.err.print("Exception in thread \"HotSwap\" ");
                e.printStackTrace(System.err);
            }

            @Override
            public void run() {
                EventSet events = null;
                try {
                    events = waitEvent();
                    EventIterator iter = events.eventIterator();
                    while (iter.hasNext()) {
                        Event event = iter.nextEvent();
                        if (event instanceof MethodEntryEvent) {
                            hotswap();
                            break;
                        }
                    }
                } catch (Throwable e) {
                    errorMsg(e);
                }
                try {
                    if (events != null)
                        events.resume();
                } catch (Throwable e) {
                    errorMsg(e);
                }
            }
        }.start();
    }

    EventSet waitEvent() throws InterruptedException {
        EventQueue queue = jvm.eventQueue();
        return queue.remove();
    }

    void hotswap() {
        Map<ReferenceType,byte[]> map = newClassFiles;
        jvm.redefineClasses(map);
        newClassFiles = null;
    }


    public void swapClasses(Class original, String swap) throws Exception {

        ClassPool classPool = new ClassPool();
        classPool.appendClassPath(new LoaderClassPath(original.getClassLoader()));

        CtClass ctClass = classPool.getAndRename(swap, original.getName());

        reload(original.getName(), ctClass.toBytecode());
    }
}
