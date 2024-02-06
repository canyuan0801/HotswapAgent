package org.hotswap.agent.distribution;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.distribution.markdown.MarkdownProcessor;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.util.scanner.ClassPathAnnotationScanner;
import org.hotswap.agent.util.scanner.ClassPathScanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


public class PluginDocs {

    public static final String TARGET_DIR = "/target/web-sources/";
    MarkdownProcessor markdownProcessor = new MarkdownProcessor();


    public static void main(String[] args) {
        try {
            new PluginDocs().scan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String getBaseURL(Class clazz) {
        String clazzUrl = clazz.getResource(clazz.getSimpleName() + ".class").toString();


        String classPath = clazz.getName().replace(".", "/");
        return clazzUrl.replace("/target/classes/" + classPath, "").replace(".class", "");
    }


    public void scan() throws Exception {
        StringBuilder html = new StringBuilder();
        addHtmlHeader(html);

        ClassPathAnnotationScanner scanner = new ClassPathAnnotationScanner(Plugin.class.getName(), new ClassPathScanner());

        for (String plugin : scanner.scanPlugins(getClass().getClassLoader(), PluginManager.PLUGIN_PACKAGE.replace(".", "/"))) {
            Class pluginClass = Class.forName(plugin);
            Plugin pluginAnnotation = (Plugin) pluginClass.getAnnotation(Plugin.class);

            String pluginName = pluginAnnotation.name();
            String pluginDocFile = "plugin/" + pluginName + ".html";
            String pluginLink = "ha-plugins/" + pluginName.toLowerCase() + "-plugin";

            URL url = new URL(getBaseURL(getClass()) + TARGET_DIR + pluginDocFile);
            boolean docExists = markdownProcessor.processPlugin(pluginClass, url);

            addHtmlRow(html, pluginAnnotation, docExists ? pluginLink : null);
        }

        addHtmlFooter(html);
        writeHtml(new URL(getBaseURL(getClass()) + TARGET_DIR + "plugins.html"), html.toString());


        String mainReadme = markdownProcessor.markdownToHtml(IOUtils.streamToString(new URL(
                getBaseURL(getClass()) + "/../README.md"
        ).openStream()));

        writeMainReadme(mainReadme);
    }

    private void writeMainReadme(String mainReadme) throws MalformedURLException {
        writeHtml(new URL(getBaseURL(getClass()) + TARGET_DIR + "README.html"), mainReadme);


        for (String section : mainReadme.split("\\<h1\\>")) {
            if (section.isEmpty())
                continue;


            int h1EndIndex = section.indexOf("</h1>");
            if (h1EndIndex > 0) {
                String label = section.substring(0, h1EndIndex);

                String content = section.substring(h1EndIndex+5);


                label = label.replaceAll("\\s", "-");
                label = label.replaceAll("[^A-Za-z0-9-]", "");
                label = label.toLowerCase();


                writeHtml(new URL(getBaseURL(getClass()) + TARGET_DIR + "section/" + label + ".html"), content);
            }
        }

    }


    private void addHtmlRow(StringBuilder html, Plugin annot, String pluginDocFile) {
        html.append("<tr>");
        html.append("<td>");
        html.append(annot.name());
        html.append("</td>");
        html.append("<td>");
        html.append(annot.description());
        html.append("</td>");
        html.append("<td>");
        commaSeparated(html, annot.testedVersions());
        html.append("</td>");
        html.append("<td>");
        commaSeparated(html, annot.expectedVersions());
        html.append("</td>");
        html.append("<td>");
        if (pluginDocFile != null) {
            html.append("<a href='");
            html.append(pluginDocFile);
            html.append("'>Documentation</a>");
        }
        html.append("</td>");
        html.append("</tr>");
    }

    private void addHtmlHeader(StringBuilder html) {
        html.append("<table>");
    }

    private void addHtmlFooter(StringBuilder html) {
        html.append("</table>");
    }

    private void commaSeparated(StringBuilder html, String[] strings) {
        boolean first = true;
        for (String s : strings) {
            if (!first)
                html.append(", ");
            html.append(s);
            first = false;
        }
    }

    private void writeHtml(URL url, String html) {
        try {
            assertDirExists(url);
            PrintWriter out = new PrintWriter(url.getFile());
            out.print(html);
            out.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Unable to open file " + url + " to write HTML content.");
        }
    }


    public static void assertDirExists(URL targetFile) {
        File parent = null;
        try {
            parent = new File(targetFile.toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

        if(!parent.exists() && !parent.mkdirs()){
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
    }
}
