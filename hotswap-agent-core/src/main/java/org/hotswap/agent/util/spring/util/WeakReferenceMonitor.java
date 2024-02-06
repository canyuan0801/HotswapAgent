

package org.hotswap.agent.util.spring.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.logging.AgentLogger;





public class WeakReferenceMonitor {
    private static AgentLogger logger = AgentLogger.getLogger(WeakReferenceMonitor.class);


    private static final ReferenceQueue<Object> handleQueue = new ReferenceQueue<Object>();


    private static final Map<Reference<?>, ReleaseListener> trackedEntries = new HashMap<Reference<?>, ReleaseListener>();


    private static Thread monitoringThread = null;


    public static void monitor(Object handle, ReleaseListener listener) {
        if (logger.isDebugEnabled()) {
            logger.debug("Monitoring handle [" + handle + "] with release listener [" + listener + "]");
        }



        WeakReference<Object> weakRef = new WeakReference<Object>(handle, handleQueue);


        addEntry(weakRef, listener);
    }


    private static void addEntry(Reference<?> ref, ReleaseListener entry) {
        synchronized (WeakReferenceMonitor.class) {

            trackedEntries.put(ref, entry);


            if (monitoringThread == null) {
                monitoringThread = new Thread(new MonitoringProcess(), WeakReferenceMonitor.class.getName());
                monitoringThread.setDaemon(true);
                monitoringThread.start();
            }
        }
    }


    private static ReleaseListener removeEntry(Reference<?> reference) {
        synchronized (WeakReferenceMonitor.class) {
            return trackedEntries.remove(reference);
        }
    }


    private static boolean keepMonitoringThreadAlive() {
        synchronized (WeakReferenceMonitor.class) {
            if (!trackedEntries.isEmpty()) {
                return true;
            } else {
                logger.debug("No entries left to track - stopping reference monitor thread");
                monitoringThread = null;
                return false;
            }
        }
    }


    private static class MonitoringProcess implements Runnable {

        @Override
        public void run() {
            logger.debug("Starting reference monitor thread");

            while (keepMonitoringThreadAlive()) {
                try {
                    Reference<?> reference = handleQueue.remove();

                    ReleaseListener entry = removeEntry(reference);
                    if (entry != null) {

                        try {
                            entry.released();
                        } catch (Throwable ex) {
                            logger.warning("Reference release listener threw exception", ex);
                        }
                    }
                } catch (InterruptedException ex) {
                    synchronized (WeakReferenceMonitor.class) {
                        monitoringThread = null;
                    }
                    logger.debug("Reference monitor thread interrupted", ex);
                    break;
                }
            }
        }
    }


    public static interface ReleaseListener {


        void released();
    }

}