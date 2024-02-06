package org.hotswap.agent.tutorial.plugin;

import org.hotswap.agent.annotation.*;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.tutorial.framework.PrintSource;
import org.hotswap.agent.tutorial.framework.PrinterService;
import org.hotswap.agent.tutorial.framework.PrinterSourceScanner;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.ReflectionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hotswap.agent.annotation.LoadEvent.REDEFINE;


@Plugin(name = "Printer Plugin", description = "Listen to any redefinition and refresh cache.",
        testedVersions = "1.x")
public class PrinterHAPlugin {


    PrinterService printerService;


    public PrinterHAPlugin(PrinterService printerService) {
        this.printerService = printerService;
    }


    public static void register(PrinterService printer) {
        PluginManager.getInstance().getPluginRegistry().initializePluginInstance(new PrinterHAPlugin(printer));
    }



    @Init
    Scheduler scheduler;


    @OnClassLoadEvent(classNameRegexp = ".*", events = REDEFINE)
    public void reloadClass(CtClass clazz) throws NotFoundException {
        if (isPrintSource(clazz)) {



            scheduler.scheduleCommand(() -> printerService.refresh());
        }
    }


    @OnClassFileEvent(classNameRegexp = ".*", events = FileEvent.CREATE)
    public void createClass(CtClass clazz) throws NotFoundException, ClassNotFoundException, IllegalAccessException, InstantiationException {



        if (isPrintSource(clazz)) {


            Class<PrintSource> newClass = (Class<PrintSource>) printerService.getClass().getClassLoader().loadClass(clazz.getName());
            PrintSource newPrintSource = newClass.newInstance();


            printerService.addPrintSource(newPrintSource);

            autoDiscoveredPrintSources.add(newPrintSource);


            printerService.refresh();
        }
    }











    @OnResourceFileEvent(path = PrinterSourceScanner.PRINTER_PROPERTY_FILE, events = FileEvent.MODIFY)
    public void reloadConfiguration() throws IOException {


        List<PrintSource> currentSource = (List<PrintSource>) ReflectionHelper.get(printerService, "printSources");


        currentSource.removeAll(autoDiscoveredPrintSources);



        currentSource.addAll(new PrinterSourceScanner().scanPrintSources());


        printerService.refresh();
    }




    List<PrintSource> autoDiscoveredPrintSources = new ArrayList<>();


    public void setAutoDiscoveredPrintSources(List<PrintSource> autoDiscoveredPrintSources) {
        this.autoDiscoveredPrintSources = autoDiscoveredPrintSources;
    }


    @OnClassLoadEvent(classNameRegexp = "org.hotswap.agent.example.framework.PrinterSourceScanner")
    public void register(CtClass clazz) throws CannotCompileException {

        String callbackMethod = PluginManagerInvoker.buildCallPluginMethod(PrinterHAPlugin.class,
                "setAutoDiscoveredPrintSources", "$_", "java.util.List");


        CtMethod scanPrintSourcesMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals("scanPrintSources"))
                .findFirst().orElseThrow(IllegalStateException::new);


        scanPrintSourcesMethod.insertAfter(callbackMethod);
    }




    private boolean isPrintSource(CtClass clazz) throws NotFoundException {
        return Arrays.stream(clazz.getInterfaces())
                .anyMatch(i -> i.getName().equals(PrintSource.class.getName()));
    }
}
