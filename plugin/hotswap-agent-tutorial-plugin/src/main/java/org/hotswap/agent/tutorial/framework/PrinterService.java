package org.hotswap.agent.tutorial.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class PrinterService {


    private List<PrintSource> printSources = new ArrayList<>();

    private PrintTarget printTarget = new PrintTarget() {};

    private List<String> cachedContents = new ArrayList<>();


    private void print() {
        printTarget.print("Contents: " + Arrays.toString(cachedContents.toArray()));
    }


    public void refresh() {
        cachedContents = printSources.stream()
                .map(PrintSource::getPrintContent)
                .collect(Collectors.toList());
    }


    public void run()  {

        registerHaPlugin();


        try {
            printSources = new PrinterSourceScanner().scanPrintSources();
            refresh();
        } catch (IOException e) {
            throw new IllegalStateException("Wrong configuration, unable to scan for print sources", e);
        } catch (NoClassDefFoundError e) {




            throw new Error("Hotswap Agent classes not found. " +
                    "Please run java with '-javaagent:hotswap-agent-core.jar' switch.");
        }


        startWorkingThread();
    }





    private void registerHaPlugin() {
        try {
            org.hotswap.agent.tutorial.plugin.PrinterHAPlugin.register(this);
        } catch (NoClassDefFoundError e) {

        }
    }


    public void stop() {
        stopped = true;
    }


    private void startWorkingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    if (stopped) {
                        break;
                    }
                    print();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }


    private boolean stopped;


    public void addPrintSource(PrintSource printSource) {
        printSources.add(printSource);
        refresh();
    }


    public void setPrintTarget(PrintTarget printTarget) {
        this.printTarget = printTarget;
    }

}
