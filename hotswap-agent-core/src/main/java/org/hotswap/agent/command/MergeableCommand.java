
package org.hotswap.agent.command;

import java.util.ArrayList;
import java.util.List;


public abstract class MergeableCommand implements Command {

    List<Command> mergedCommands = new ArrayList<>();


    public Command merge(Command other) {
        mergedCommands.add(other);
        return this;
    }

    public List<Command> getMergedCommands() {
        return mergedCommands;
    }


    public List<Command> popMergedCommands() {
        List<Command> result = new ArrayList<>(mergedCommands);
        mergedCommands.clear();
        return result;
    }
}
