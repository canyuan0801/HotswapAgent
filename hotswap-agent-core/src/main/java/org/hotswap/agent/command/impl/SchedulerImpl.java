
package org.hotswap.agent.command.impl;

import org.hotswap.agent.annotation.handler.WatchEventCommand;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.logging.AgentLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SchedulerImpl implements Scheduler {
    private static AgentLogger LOGGER = AgentLogger.getLogger(SchedulerImpl.class);

    int DEFAULT_SCHEDULING_TIMEOUT = 100;





    final Map<Command, DuplicateScheduleConfig> scheduledCommands = new ConcurrentHashMap<>();
    final Set<Command> runningCommands = Collections.synchronizedSet(new HashSet<Command>());

    Thread runner;
    boolean stopped;

    @Override
    public void scheduleCommand(Command command) {
        scheduleCommand(command, DEFAULT_SCHEDULING_TIMEOUT);
    }

    @Override
    public void scheduleCommand(Command command, int timeout) {
        scheduleCommand(command, timeout, DuplicateSheduleBehaviour.WAIT_AND_RUN_AFTER);
    }

    @Override
    public void scheduleCommand(Command command, int timeout, DuplicateSheduleBehaviour behaviour) {
        synchronized (scheduledCommands) {
            Command targetCommand = command;
            if (scheduledCommands.containsKey(command) && (command instanceof MergeableCommand)) {

                for (Command scheduledCommand : scheduledCommands.keySet()) {
                    if (command.equals(scheduledCommand)) {
                        targetCommand = ((MergeableCommand) scheduledCommand).merge(command);
                        break;
                    }
                }
            }


            scheduledCommands.put(targetCommand, new DuplicateScheduleConfig(System.currentTimeMillis() + timeout, behaviour));
            LOGGER.trace("{} scheduled for execution in {}ms", targetCommand, timeout);
        }
    }


    private boolean processCommands() {
        Long currentTime = System.currentTimeMillis();
        synchronized (scheduledCommands) {
            for (Iterator<Map.Entry<Command, DuplicateScheduleConfig>> it = scheduledCommands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Command, DuplicateScheduleConfig> entry = it.next();
                DuplicateScheduleConfig config = entry.getValue();
                Command command = entry.getKey();


                if (config.getTime() < currentTime) {

                    if (runningCommands.contains(command)) {
                        if (config.getBehaviour().equals(DuplicateSheduleBehaviour.SKIP)) {
                            LOGGER.debug("Skipping duplicate running command {}", command);
                            it.remove();
                        } else if (config.getBehaviour().equals(DuplicateSheduleBehaviour.RUN_DUPLICATE)) {
                            executeCommand(command);
                            it.remove();
                        }
                    } else {
                        executeCommand(command);
                        it.remove();
                    }
                }
            }
        }

        return true;
    }


    private void executeCommand(Command command) {
        if (command instanceof WatchEventCommand)
            LOGGER.trace("Executing {}", command);
        else
            LOGGER.debug("Executing {}", command);

        runningCommands.add(command);
        new CommandExecutor(command) {
            @Override
            public void finished() {
                runningCommands.remove(command);
            }
        }.start();
    }

    @Override
    public void run() {
        runner = new Thread() {
            @Override
            public void run() {
                for (; ; ) {
                    if (stopped || !processCommands())
                        break;


                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

            }
        };

        runner.setDaemon(true);
        runner.start();
    }

    @Override
    public void stop() {
        stopped = true;
    }

    private static class DuplicateScheduleConfig {

        long time;


        DuplicateSheduleBehaviour behaviour;

        private DuplicateScheduleConfig(long time, DuplicateSheduleBehaviour behaviour) {
            this.time = time;
            this.behaviour = behaviour;
        }

        public long getTime() {
            return time;
        }

        public DuplicateSheduleBehaviour getBehaviour() {
            return behaviour;
        }
    }
}
