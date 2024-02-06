

package org.hotswap.agent.javassist;


public final class CtPrimitiveType extends CtClass {
    private char descriptor;
    private String wrapperName;
    private String getMethodName;
    private String mDescriptor;
    private int returnOp;
    private int arrayType;
    private int dataSize;

    CtPrimitiveType(String name, char desc, String wrapper,
                    String methodName, String mDesc, int opcode, int atype,
                    int size) {
        super(name);
        descriptor = desc;
        wrapperName = wrapper;
        getMethodName = methodName;
        mDescriptor = mDesc;
        returnOp = opcode;
        arrayType = atype;
        dataSize = size;
    }


    @Override
    public boolean isPrimitive() { return true; }


    @Override
    public int getModifiers() {
        return Modifier.PUBLIC | Modifier.FINAL;
    }


    public char getDescriptor() { return descriptor; }


    public String getWrapperName() { return wrapperName; }


    public String getGetMethodName() { return getMethodName; }


    public String getGetMethodDescriptor() { return mDescriptor; }


    public int getReturnOp() { return returnOp; }


    public int getArrayType() { return arrayType; }


    public int getDataSize() { return dataSize; }
}
