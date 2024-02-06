
package org.hotswap.agent.logging;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AgentLoggerHandler {


    PrintStream outputStream;

    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");


    public void setPrintStream(PrintStream outputStream) {
        this.outputStream = outputStream;
    }


    protected void printMessage(String message) {
        String log = "HOTSWAP AGENT: " + sdf.format(new Date()) +  " " + message;
        System.out.println(log);
        if (outputStream != null)
            outputStream.println(log);
    }

    public void print(Class clazz, AgentLogger.Level level, String message, Throwable throwable, Object... args) {


        String messageWithArgs = message;
        for (Object arg : args) {
            int index = messageWithArgs.indexOf("{}");
            if (index >= 0) {
                messageWithArgs = messageWithArgs.substring(0, index) + String.valueOf(arg) + messageWithArgs.substring(index + 2);
            }
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(level);
        stringBuffer.append(" (");
        stringBuffer.append(clazz.getName());
        stringBuffer.append(") - ");
        stringBuffer.append(messageWithArgs);

        if (throwable != null) {
            stringBuffer.append("\n");
            stringBuffer.append(formatErrorTrace(throwable));
        }

        printMessage(stringBuffer.toString());
    }

    private String formatErrorTrace(Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        sdf = new SimpleDateFormat(dateTimeFormat);
    }
}
