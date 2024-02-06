package org.hotswap.agent.distribution.markdown;

import org.hotswap.agent.distribution.PluginDocs;
import org.hotswap.agent.util.IOUtils;
import org.pegdown.PegDownProcessor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class MarkdownProcessor {
    PegDownProcessor pegDownProcessor;

    public MarkdownProcessor() {
        pegDownProcessor = new PegDownProcessor();
    }


    public boolean processPlugin(Class plugin, URL targetFile) {
        String markdown = resolveMarkdownDoc(plugin);
        if (markdown == null)
            return false;

        String html = markdownToHtml(markdown);


        if (html.startsWith("<h1>")) {
            int h1EndIndex = html.indexOf("</h1>");
            if (h1EndIndex > 0) {
                html = html.substring(h1EndIndex + 5);
            }
        }

        PluginDocs.assertDirExists(targetFile);

        try {
            PrintWriter out = new PrintWriter(targetFile.getFile());
            out.print(html);
            out.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Unable to open file " + targetFile + " to write HTML content.");
        }

        return true;
    }


    public String markdownToHtml(String src) {
        return pegDownProcessor.markdownToHtml(src);
    }


    public String resolveMarkdownDoc(Class plugin) {
        InputStream is = resolveSamePackageReadme(plugin);

        if (is == null) {
            is = resolveMavenMainDirectoryReadme(plugin);
        }

        if (is != null)
            return IOUtils.streamToString(is);
        else
            return null;
    }


    private InputStream resolveSamePackageReadme(Class plugin) {

        return plugin.getResourceAsStream("README.md");
    }


    private InputStream resolveMavenMainDirectoryReadme(Class plugin) {
        try {
            URI uri = new URI(PluginDocs.getBaseURL(plugin) + "/README.md");
            if (uri.toString().endsWith("/HotswapAgent/README.md")) {


                return null;
            } else {
                return new FileInputStream(new File(uri));
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }


}
