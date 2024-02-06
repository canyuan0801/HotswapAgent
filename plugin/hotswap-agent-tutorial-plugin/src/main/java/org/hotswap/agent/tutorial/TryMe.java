package org.hotswap.agent.tutorial;

import org.hotswap.agent.tutorial.framework.PrinterService;


public class TryMe {

    public static void main(String[] args) throws InterruptedException {
        PrinterService printerService = new PrinterService();
        printerService.run();
        Thread.sleep(1000000);
    }
}
