
package org.hotswap.agent.command;

import java.util.concurrent.TimeUnit;


public interface Scheduler {

    public static enum DuplicateSheduleBehaviour {
        SKIP,
        WAIT_AND_RUN_AFTER,
        RUN_DUPLICATE
    }


    void scheduleCommand(Command command);


    void scheduleCommand(Command command, int timeout);


    void scheduleCommand(Command command, int timeout, DuplicateSheduleBehaviour behaviour);


    void run();


    void stop();
}
