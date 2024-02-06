
package org.hotswap.agent.watch.nio;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.WatchFileEvent;


public class EventDispatcher implements Runnable {


    protected AgentLogger LOGGER = AgentLogger.getLogger(this.getClass());


    static class Event {


        final WatchEvent<Path> event;


        final Path path;


        public Event(WatchEvent<Path> event, Path path) {
            super();
            this.event = event;
            this.path = path;
        }
    }


    private final Map<Path, List<WatchEventListener>> listeners;


    private final ArrayList<Event> working = new ArrayList<>();


    private Thread runnable = null;


    public EventDispatcher(Map<Path, List<WatchEventListener>> listeners) {
        super();
        this.listeners = listeners;
    }


    private final ArrayBlockingQueue<Event> eventQueue = new ArrayBlockingQueue<>(500);


    @Override
    public void run() {


        while (true) {

            for (Event e : working) {
                callListeners(e.event, e.path);
                if (Thread.interrupted()) {
                    return;
                }
                Thread.yield();
            }

            eventQueue.drainTo(working);


            for (Event e : working) {
                callListeners(e.event, e.path);
                if (Thread.interrupted()) {
                    return;
                }
                Thread.yield();
            }


            working.clear();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e1) {

                return;
            }
        }
    }


    public void add(WatchEvent<Path> event, Path path) {
        eventQueue.offer(new Event(event, path));
    }



    private void callListeners(final WatchEvent<?> event, final Path path) {
        boolean matchedOne = false;
        for (Map.Entry<Path, List<WatchEventListener>> list : listeners.entrySet()) {
            if (path.startsWith(list.getKey())) {
                matchedOne = true;
                for (WatchEventListener listener : new ArrayList<>(list.getValue())) {
                    WatchFileEvent agentEvent = new HotswapWatchFileEvent(event, path);
                    try {
                        listener.onEvent(agentEvent);
                    } catch (Throwable e) {


                    }
                }
            }
        }
        if (!matchedOne) {
            LOGGER.error("No match for  watch event '{}',  path '{}'", event, path);
        }
    }


    public void start() {
        runnable = new Thread(this);
        runnable.setDaemon(true);
        runnable.setName("HotSwap Dispatcher");
        runnable.start();
    }


    public void stop() throws InterruptedException {
        if (runnable != null) {
            runnable.interrupt();
            runnable.join();
        }
        runnable = null;
    }
}
