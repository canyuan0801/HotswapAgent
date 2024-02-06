

package org.hotswap.agent.util.spring.io.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.hotswap.agent.util.spring.util.Assert;
import org.hotswap.agent.util.spring.util.StringUtils;


public class FileSystemResource extends AbstractResource implements WritableResource {

    private final File file;

    private final String path;

    
    public FileSystemResource(File file) {
        Assert.notNull(file, "File must not be null");
        this.file = file;
        this.path = StringUtils.cleanPath(file.getPath());
    }

    
    public FileSystemResource(String path) {
        Assert.notNull(path, "Path must not be null");
        this.file = new File(path);
        this.path = StringUtils.cleanPath(path);
    }

    
    public final String getPath() {
        return this.path;
    }

    
    @Override
    public boolean exists() {
        return this.file.exists();
    }

    
    @Override
    public boolean isReadable() {
        return (this.file.canRead() && !this.file.isDirectory());
    }

    
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    
    @Override
    public URL getURL() throws IOException {
        return this.file.toURI().toURL();
    }

    
    @Override
    public URI getURI() throws IOException {
        return this.file.toURI();
    }

    
    @Override
    public File getFile() {
        return this.file;
    }

    
    @Override
    public long contentLength() throws IOException {
        return this.file.length();
    }

    
    @Override
    public Resource createRelative(String relativePath) {
        String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
        return new FileSystemResource(pathToUse);
    }

    
    @Override
    public String getFilename() {
        return this.file.getName();
    }

    
    @Override
    public String getDescription() {
        return "file [" + this.file.getAbsolutePath() + "]";
    }

    

    
    @Override
    public boolean isWritable() {
        return (this.file.canWrite() && !this.file.isDirectory());
    }

    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(this.file);
    }

    
    @Override
    public boolean equals(Object obj) {
        return (obj == this || (obj instanceof FileSystemResource && this.path.equals(((FileSystemResource) obj).path)));
    }

    
    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

}