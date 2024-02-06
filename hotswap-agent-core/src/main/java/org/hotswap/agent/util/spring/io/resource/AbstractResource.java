

package org.hotswap.agent.util.spring.io.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.hotswap.agent.util.spring.util.Assert;
import org.hotswap.agent.util.spring.util.ResourceUtils;


public abstract class AbstractResource implements Resource {


    @Override
    public boolean exists() {

        try {
            return getFile().exists();
        } catch (IOException ex) {

            try (InputStream is = getInputStream()) {
                return true;
            } catch (Throwable isEx) {
                return false;
            }
        }
    }


    @Override
    public boolean isReadable() {
        return true;
    }


    @Override
    public boolean isOpen() {
        return false;
    }


    @Override
    public URL getURL() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
    }


    @Override
    public URI getURI() throws IOException {
        URL url = getURL();
        try {
            return ResourceUtils.toURI(url);
        } catch (URISyntaxException ex) {
            throw new NestedIOException("Invalid URI [" + url + "]", ex);
        }
    }


    @Override
    public File getFile() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
    }


    @Override
    public long contentLength() throws IOException {
        try (InputStream is = this.getInputStream()) {
            Assert.state(is != null, "resource input stream must not be null");
            long size = 0;
            byte[] buf = new byte[255];
            int read;
            while ((read = is.read(buf)) != -1) {
                size += read;
            }
            return size;
        }
    }


    @Override
    public long lastModified() throws IOException {
        long lastModified = getFileForLastModifiedCheck().lastModified();
        if (lastModified == 0L) {
            throw new FileNotFoundException(getDescription() + " cannot be resolved in the file system for resolving its last-modified timestamp");
        }
        return lastModified;
    }


    protected File getFileForLastModifiedCheck() throws IOException {
        return getFile();
    }


    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
    }


    @Override
    public String getFilename() {
        return null;
    }


    @Override
    public String toString() {
        return getDescription();
    }


    @Override
    public boolean equals(Object obj) {
        return (obj == this || (obj instanceof Resource && ((Resource) obj).getDescription().equals(getDescription())));
    }


    @Override
    public int hashCode() {
        return getDescription().hashCode();
    }

}