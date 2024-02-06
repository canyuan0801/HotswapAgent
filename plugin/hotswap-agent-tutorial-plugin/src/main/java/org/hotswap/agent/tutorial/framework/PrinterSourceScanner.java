package org.hotswap.agent.tutorial.framework;

import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.util.scanner.ClassPathScanner;
import org.hotswap.agent.util.scanner.ScannerVisitor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class PrinterSourceScanner {

    public static final String PRINTER_PROPERTY_FILE = "printer.properties";


    public List<PrintSource> scanPrintSources() throws IOException {


        Properties printerProps = new Properties();
        printerProps.load(getClass().getClassLoader().getResourceAsStream(PRINTER_PROPERTY_FILE));


        String sourcePackage = printerProps.getProperty("sourcePackage", "org.hotswap.agent.example.printerApp");


        return scanPackageForPrintSources(sourcePackage);
    }



    private List<PrintSource> scanPackageForPrintSources(String sourcePackage) throws IOException {
        final List<PrintSource> discoveredSources = new ArrayList<>();
        String sourcePath = sourcePackage.replace(".", "/");

        new ClassPathScanner().scan(getClass().getClassLoader(), sourcePath, new ScannerVisitor() {
            @Override
            public void visit(InputStream file) throws IOException {
                ClassFile cf;
                try {
                    DataInputStream dstream = new DataInputStream(file);
                    cf = new ClassFile(dstream);

                    for (String iface : cf.getInterfaces()) {
                        if (iface.equals(PrintSource.class.getName())) {

                            Class<PrintSource> printSource = (Class<PrintSource>) getClass().getClassLoader().loadClass(cf.getName());
                            discoveredSources.add(printSource.newInstance());
                        }
                    }
                } catch (IOException e) {
                    throw new IOException("Stream not a valid classFile", e);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Discovered class not found by classloader", e);
                } catch (IllegalAccessException e) {
                    throw new IOException("Unable to create new instance", e);
                } catch (InstantiationException e) {
                    throw new IOException("Print source does not contain no-arg constructor", e);
                }

            }
        });

        return discoveredSources;
    }
}
