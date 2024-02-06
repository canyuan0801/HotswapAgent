
package org.hotswap.agent.util.test;


public class WaitHelper {


    public static boolean waitForResult(WaitHelper.ResultHolder resultHolder) {
        return waitForResult(resultHolder, 1000);
    }



    public static boolean waitForResult(WaitHelper.ResultHolder resultHolder, int timeout) {
        for (int i = 0; i < timeout / 10; i++) {
            if (resultHolder.result)
                return true;


            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }


    public static class ResultHolder {
        public boolean result = false;
    }



    public static boolean waitForCommand(Command command) {
        return waitForCommand(command, 1000);
    }


    public static boolean waitForCommand(Command command, int timeout) {
        for (int i = 0; i < timeout / 10; i++) {
            try {
                if (command.result())
                    return true;
            } catch (Exception e) {

            }


            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }


    public static abstract class Command {

        public abstract boolean result() throws Exception;
    }
}
