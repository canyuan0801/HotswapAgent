
package org.hotswap.agent.util.scanner;

import org.hotswap.agent.logging.AgentLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class ClassPathScanner implements Scanner {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassPathScanner.class);


    public static final String JAR_URL_SEPARATOR = "!/";
    public static final String JAR_URL_PREFIX = "jar:";
    public static final String ZIP_URL_PREFIX = "zip:";
    public static final String FILE_URL_PREFIX = "file:";


    @Override
    public void scan(ClassLoader classLoader, String path, ScannerVisitor visitor) throws IOException {
        LOGGER.trace("Scanning path {}", path);

        Enumeration<URL> en = classLoader == null ? ClassLoader.getSystemResources(path) : classLoader.getResources(path);
        while (en.hasMoreElements()) {
            URL pluginDirURL = en.nextElement();
            File pluginDir = new File(pluginDirURL.getFile());
            if (pluginDir.isDirectory()) {
                scanDirectory(pluginDir, visitor);
            } else {

                String uri;
                try {
                    uri = pluginDirURL.toURI().toString();
                } catch (URISyntaxException e) {
                    throw new IOException("Illegal directory URI " + pluginDirURL, e);
                }

                if (uri.startsWith(JAR_URL_PREFIX) || uri.startsWith(ZIP_URL_PREFIX)) {
                    String jarFile = uri.substring(uri.indexOf(':') + 1);
                    scanJar(jarFile, visitor);
                } else {
                    LOGGER.warning("Unknown resource type of file " + uri);
                }
            }
        }
    }


    protected void scanDirectory(File pluginDir, ScannerVisitor visitor) throws IOException {
        LOGGER.trace("Scanning directory " + pluginDir.getName());

        for (File file : pluginDir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, visitor);
            } else if (file.isFile() && file.getName().endsWith(".class")) {
                visitor.visit(new FileInputStream(file));
            }
        }
    }


    private void scanJar(String urlFile, ScannerVisitor visitor) throws IOException {
        LOGGER.trace("Scanning JAR file '{}'", urlFile);

        int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
        JarFile jarFile = null;
        String rootEntryPath;

        try {
            if (separatorIndex != -1) {
                String jarFileUrl = urlFile.substring(0, separatorIndex);
                rootEntryPath = urlFile.substring(separatorIndex + JAR_URL_SEPARATOR.length());
                jarFile = getJarFile(jarFileUrl);
            } else {
                rootEntryPath = "";
                jarFile = new JarFile(urlFile);
            }

            if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
                rootEntryPath = rootEntryPath + "/";
            }

            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                String entryPath = entry.getName();


                if (entryPath.startsWith(rootEntryPath) && entryPath.endsWith(".class")) {
                    LOGGER.trace("Visiting JAR entry {}", entryPath);
                    visitor.visit(jarFile.getInputStream(entry));
                }
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }


    protected JarFile getJarFile(String jarFileUrl) throws IOException {
        LOGGER.trace("Opening JAR file " + jarFileUrl);
        if (jarFileUrl.startsWith(FILE_URL_PREFIX)) {
            try {
                return new JarFile(toURI(jarFileUrl).getSchemeSpecificPart());
            } catch (URISyntaxException ex) {

                return new JarFile(jarFileUrl.substring(FILE_URL_PREFIX.length()));
            }
        } else {
            return new JarFile(jarFileUrl);
        }
    }


    public static URI toURI(String location) throws URISyntaxException {
        return new URI(location.replace(" ", "%20"));
    }
}
