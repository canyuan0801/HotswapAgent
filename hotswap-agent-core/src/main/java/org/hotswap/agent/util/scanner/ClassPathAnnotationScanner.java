
package org.hotswap.agent.util.scanner;

import org.hotswap.agent.javassist.bytecode.AnnotationsAttribute;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.annotation.Annotation;
import org.hotswap.agent.logging.AgentLogger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;


public class ClassPathAnnotationScanner {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassPathAnnotationScanner.class);

    
    String annotation;

    
    Scanner scanner;

    
    public ClassPathAnnotationScanner(String annotation, Scanner scanner) {
        this.annotation = annotation;
        this.scanner = scanner;
    }

    
    public List<String> scanPlugins(ClassLoader classLoader, String path) throws IOException {
        final List<String> files = new LinkedList<>();
        scanner.scan(classLoader, path, new ScannerVisitor() {
            @Override
            public void visit(InputStream file) throws IOException {
                ClassFile cf;
                try {
                    DataInputStream dstream = new DataInputStream(file);
                    cf = new ClassFile(dstream);
                } catch (IOException e) {
                    throw new IOException("Stream not a valid classFile", e);
                }

                if (hasAnnotation(cf))
                    files.add(cf.getName());
            }
        });
        return files;
    }

    
    protected boolean hasAnnotation(ClassFile cf) throws IOException {

        AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
        if (visible != null) {
            for (Annotation ann : visible.getAnnotations()) {
                if (annotation.equals(ann.getTypeName())) {
                    return true;
                }
            }
        }
        return false;
    }


}
