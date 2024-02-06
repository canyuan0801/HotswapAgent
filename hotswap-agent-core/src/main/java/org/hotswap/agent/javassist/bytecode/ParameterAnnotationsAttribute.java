

package org.hotswap.agent.javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.javassist.bytecode.AnnotationsAttribute.Copier;
import org.hotswap.agent.javassist.bytecode.AnnotationsAttribute.Parser;
import org.hotswap.agent.javassist.bytecode.AnnotationsAttribute.Renamer;
import org.hotswap.agent.javassist.bytecode.annotation.Annotation;
import org.hotswap.agent.javassist.bytecode.annotation.AnnotationsWriter;


public class ParameterAnnotationsAttribute extends AttributeInfo {

    public static final String visibleTag
        = "RuntimeVisibleParameterAnnotations";


    public static final String invisibleTag
        = "RuntimeInvisibleParameterAnnotations";

    public ParameterAnnotationsAttribute(ConstPool cp, String attrname,
                                         byte[] info) {
        super(cp, attrname, info);
    }


    public ParameterAnnotationsAttribute(ConstPool cp, String attrname) {
        this(cp, attrname, new byte[] { 0 });
    }


    ParameterAnnotationsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }


    public int numParameters() {
        return info[0] & 0xff;
    }


    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        Copier copier = new Copier(info, constPool, newCp, classnames);
        try {
            copier.parameters();
            return new ParameterAnnotationsAttribute(newCp, getName(),
                                                     copier.close());
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }


    public Annotation[][] getAnnotations() {
        try {
            return new Parser(info, constPool).parseParameters();
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }


    public void setAnnotations(Annotation[][] params) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AnnotationsWriter writer = new AnnotationsWriter(output, constPool);
        try {
            writer.numParameters(params.length);
            for (Annotation[] anno:params) {
                writer.numAnnotations(anno.length);
                for (int j = 0; j < anno.length; ++j)
                    anno[j].write(writer);
            }

            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        set(output.toByteArray());
    }


    @Override
    void renameClass(String oldname, String newname) {
        Map<String,String> map = new HashMap<String,String>();
        map.put(oldname, newname);
        renameClass(map);
    }

    @Override
    void renameClass(Map<String,String> classnames) {
        Renamer renamer = new Renamer(info, getConstPool(), classnames);
        try {
            renamer.parameters();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void getRefClasses(Map<String,String> classnames) { renameClass(classnames); }


    @Override
    public String toString() {
        Annotation[][] aa = getAnnotations();
        StringBuilder sbuf = new StringBuilder();
        for (Annotation[] a : aa) {
            for (Annotation i : a)
                sbuf.append(i.toString()).append(" ");

            sbuf.append(", ");
        }

        return sbuf.toString().replaceAll(" (?=,)|, $","");
    }
}
