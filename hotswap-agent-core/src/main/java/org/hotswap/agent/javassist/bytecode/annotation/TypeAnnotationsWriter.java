package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.io.OutputStream;

import org.hotswap.agent.javassist.bytecode.ConstPool;


public class TypeAnnotationsWriter extends AnnotationsWriter {

    public TypeAnnotationsWriter(OutputStream os, ConstPool cp) {
        super(os, cp);
    }


    @Override
    public void numAnnotations(int num) throws IOException {
        super.numAnnotations(num);
    }


    public void typeParameterTarget(int targetType, int typeParameterIndex)
        throws IOException
    {
        output.write(targetType);
        output.write(typeParameterIndex);
    }


    public void supertypeTarget(int supertypeIndex)
        throws IOException
    {
        output.write(0x10);
        write16bit(supertypeIndex);
    }    


    public void typeParameterBoundTarget(int targetType, int typeParameterIndex, int boundIndex)
        throws IOException
    {
        output.write(targetType);
        output.write(typeParameterIndex);
        output.write(boundIndex);
    }


    public void emptyTarget(int targetType) throws IOException {
        output.write(targetType);
    }


    public void formalParameterTarget(int formalParameterIndex)
        throws IOException
    {
        output.write(0x16);
        output.write(formalParameterIndex);
    }


    public void throwsTarget(int throwsTypeIndex)
        throws IOException
    {
        output.write(0x17);
        write16bit(throwsTypeIndex);
    } 


    public void localVarTarget(int targetType, int tableLength)
        throws IOException
    {
        output.write(targetType);
        write16bit(tableLength);
    }


    public void localVarTargetTable(int startPc, int length, int index)
        throws IOException
    {
        write16bit(startPc);
        write16bit(length);
        write16bit(index);
    }


    public void catchTarget(int exceptionTableIndex)
        throws IOException
    {
        output.write(0x42);
        write16bit(exceptionTableIndex);
    } 


    public void offsetTarget(int targetType, int offset)
        throws IOException
    {
        output.write(targetType);
        write16bit(offset);
    }


    public void typeArgumentTarget(int targetType, int offset, int type_argument_index)
        throws IOException
    {
        output.write(targetType);
        write16bit(offset);
        output.write(type_argument_index);
    }


    public void typePath(int pathLength) throws IOException {
        output.write(pathLength);
    }


    public void typePathPath(int typePathKind, int typeArgumentIndex)
        throws IOException
    {
        output.write(typePathKind);
        output.write(typeArgumentIndex);
    }
}
