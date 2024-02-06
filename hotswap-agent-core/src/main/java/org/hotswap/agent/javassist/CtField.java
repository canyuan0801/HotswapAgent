

package org.hotswap.agent.javassist;

import java.util.List;

import org.hotswap.agent.javassist.bytecode.AccessFlag;
import org.hotswap.agent.javassist.bytecode.AnnotationsAttribute;
import org.hotswap.agent.javassist.bytecode.AttributeInfo;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.FieldInfo;
import org.hotswap.agent.javassist.bytecode.SignatureAttribute;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;
import org.hotswap.agent.javassist.compiler.SymbolTable;
import org.hotswap.agent.javassist.compiler.ast.ASTree;
import org.hotswap.agent.javassist.compiler.ast.DoubleConst;
import org.hotswap.agent.javassist.compiler.ast.IntConst;
import org.hotswap.agent.javassist.compiler.ast.StringL;


public class CtField extends CtMember {
    static final String javaLangString = "java.lang.String";

    protected FieldInfo fieldInfo;

    
    public CtField(CtClass type, String name, CtClass declaring)
        throws CannotCompileException
    {
        this(Descriptor.of(type), name, declaring);
    }

    
    public CtField(CtField src, CtClass declaring)
        throws CannotCompileException
    {
        this(src.fieldInfo.getDescriptor(), src.fieldInfo.getName(),
             declaring);
        FieldInfo fi = fieldInfo;
        fi.setAccessFlags(src.fieldInfo.getAccessFlags());
        ConstPool cp = fi.getConstPool();
        List<AttributeInfo> attributes = src.fieldInfo.getAttributes();
        for (AttributeInfo ainfo : attributes) 
            fi.addAttribute(ainfo.copy(cp, null));
    }

    private CtField(String typeDesc, String name, CtClass clazz)
        throws CannotCompileException
    {
        super(clazz);
        ClassFile cf = clazz.getClassFile2();
        if (cf == null)
            throw new CannotCompileException("bad declaring class: "
                                             + clazz.getName());

        fieldInfo = new FieldInfo(cf.getConstPool(), name, typeDesc);
    }

    CtField(FieldInfo fi, CtClass clazz) {
        super(clazz);
        fieldInfo = fi;
    }

    
    @Override
    public String toString() {
        return getDeclaringClass().getName() + "." + getName()
               + ":" + fieldInfo.getDescriptor();
    }

    @Override
    protected void extendToString(StringBuffer buffer) {
        buffer.append(' ');
        buffer.append(getName());
        buffer.append(' ');
        buffer.append(fieldInfo.getDescriptor());
    }

    
    protected ASTree getInitAST() { return null; }

    
    Initializer getInit() {
        ASTree tree = getInitAST();
        if (tree == null)
            return null;
        return Initializer.byExpr(tree);
    }

    
    public static CtField make(String src, CtClass declaring)
        throws CannotCompileException
    {
        Javac compiler = new Javac(declaring);
        try {
            CtMember obj = compiler.compile(src);
            if (obj instanceof CtField)
                return (CtField)obj; 
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }

        throw new CannotCompileException("not a field");
    }

    
    public FieldInfo getFieldInfo() {
        declaringClass.checkModify();
        return fieldInfo;
    }

    
    public FieldInfo getFieldInfo2() { return fieldInfo; }

    
    @Override
    public CtClass getDeclaringClass() {
        
        return super.getDeclaringClass();
    }

    
    @Override
    public String getName() {
        return fieldInfo.getName();
    }

    
    public void setName(String newName) {
        declaringClass.checkModify();
        fieldInfo.setName(newName);
    }

    
    @Override
    public int getModifiers() {
        return AccessFlag.toModifier(fieldInfo.getAccessFlags());
    }

    
    @Override
    public void setModifiers(int mod) {
        declaringClass.checkModify();
        fieldInfo.setAccessFlags(AccessFlag.of(mod));
    }

    
    @Override
    public boolean hasAnnotation(String typeName) {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.hasAnnotationType(typeName, getDeclaringClass().getClassPool(),
                                             ainfo, ainfo2);
    }

    
    @Override
    public Object getAnnotation(Class<?> clz) throws ClassNotFoundException {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.getAnnotationType(clz, getDeclaringClass().getClassPool(),
                                             ainfo, ainfo2);
    }

    
    @Override
    public Object[] getAnnotations() throws ClassNotFoundException {
        return getAnnotations(false);
    }

    
    @Override
    public Object[] getAvailableAnnotations(){
        try {
            return getAnnotations(true);
        }
        catch (ClassNotFoundException e) {
           throw new RuntimeException("Unexpected exception", e);
        }
    }

    private Object[] getAnnotations(boolean ignoreNotFound) throws ClassNotFoundException {
        FieldInfo fi = getFieldInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.invisibleTag);  
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                    fi.getAttribute(AnnotationsAttribute.visibleTag);  
        return CtClassType.toAnnotationType(ignoreNotFound, getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2);
    }

    
    @Override
    public String getSignature() {
        return fieldInfo.getDescriptor();
    }

    
    @Override
    public String getGenericSignature() {
        SignatureAttribute sa
            = (SignatureAttribute)fieldInfo.getAttribute(SignatureAttribute.tag);
        return sa == null ? null : sa.getSignature();
    }

    
    @Override
    public void setGenericSignature(String sig) {
        declaringClass.checkModify();
        fieldInfo.addAttribute(new SignatureAttribute(fieldInfo.getConstPool(), sig));
    }

    
    public CtClass getType() throws NotFoundException {
        return Descriptor.toCtClass(fieldInfo.getDescriptor(),
                                    declaringClass.getClassPool());
    }

    
    public void setType(CtClass clazz) {
        declaringClass.checkModify();
        fieldInfo.setDescriptor(Descriptor.of(clazz));
    }

    
    public Object getConstantValue() {
        
        

        int index = fieldInfo.getConstantValue();
        if (index == 0)
            return null;

        ConstPool cp = fieldInfo.getConstPool();
        switch (cp.getTag(index)) {
            case ConstPool.CONST_Long :
                return Long.valueOf(cp.getLongInfo(index));
            case ConstPool.CONST_Float :
                return Float.valueOf(cp.getFloatInfo(index));
            case ConstPool.CONST_Double :
                return Double.valueOf(cp.getDoubleInfo(index));
            case ConstPool.CONST_Integer :
                int value = cp.getIntegerInfo(index);
                
                if ("Z".equals(fieldInfo.getDescriptor()))
                    return Boolean.valueOf(value != 0);
                return Integer.valueOf(value);
            case ConstPool.CONST_String :
                return cp.getStringInfo(index);
            default :
                throw new RuntimeException("bad tag: " + cp.getTag(index)
                                           + " at " + index);
        }
    }

    
    @Override
    public byte[] getAttribute(String name) {
        AttributeInfo ai = fieldInfo.getAttribute(name);
        if (ai == null)
            return null;
        return ai.get();
    }

    
    @Override
    public void setAttribute(String name, byte[] data) {
        declaringClass.checkModify();
        fieldInfo.addAttribute(new AttributeInfo(fieldInfo.getConstPool(),
                                                 name, data));
    }

    

    
    public static abstract class Initializer {
        
        public static Initializer constant(int i) {
            return new IntInitializer(i);
        }

        
        public static Initializer constant(boolean b) {
            return new IntInitializer(b ? 1 : 0);
        }

        
        public static Initializer constant(long l) {
            return new LongInitializer(l);
        }

        
        public static Initializer constant(float l) {
            return new FloatInitializer(l);
        }

        
        public static Initializer constant(double d) {
            return new DoubleInitializer(d);
        }

        
        public static Initializer constant(String s) {
            return new StringInitializer(s);
        }

        
        public static Initializer byParameter(int nth) {
            ParamInitializer i = new ParamInitializer();
            i.nthParam = nth;
            return i;
        }

        
        public static Initializer byNew(CtClass objectType) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = null;
            i.withConstructorParams = false;
            return i;
        }

        
        public static Initializer byNew(CtClass objectType,
                                             String[] stringParams) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = stringParams;
            i.withConstructorParams = false;
            return i;
        }

        
        public static Initializer byNewWithParams(CtClass objectType) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = null;
            i.withConstructorParams = true;
            return i;
        }

        
        public static Initializer byNewWithParams(CtClass objectType,
                                               String[] stringParams) {
            NewInitializer i = new NewInitializer();
            i.objectType = objectType;
            i.stringParams = stringParams;
            i.withConstructorParams = true;
            return i;
        }

        
        public static Initializer byCall(CtClass methodClass,
                                              String methodName) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = null;
            i.withConstructorParams = false;
            return i;
        }

        
        public static Initializer byCall(CtClass methodClass,
                                              String methodName,
                                              String[] stringParams) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = stringParams;
            i.withConstructorParams = false;
            return i;
        }

        
        public static Initializer byCallWithParams(CtClass methodClass,
                                                        String methodName) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = null;
            i.withConstructorParams = true;
            return i;
        }

        
        public static Initializer byCallWithParams(CtClass methodClass,
                                String methodName, String[] stringParams) {
            MethodInitializer i = new MethodInitializer();
            i.objectType = methodClass;
            i.methodName = methodName;
            i.stringParams = stringParams;
            i.withConstructorParams = true;
            return i;
        }

        
        public static Initializer byNewArray(CtClass type, int size) 
            throws NotFoundException
        {
            return new ArrayInitializer(type.getComponentType(), size);
        }

        
        public static Initializer byNewArray(CtClass type, int[] sizes) {
            return new MultiArrayInitializer(type, sizes);
        }

        
        public static Initializer byExpr(String source) {
            return new CodeInitializer(source);
        }

        static Initializer byExpr(ASTree source) {
            return new PtreeInitializer(source);
        }

        
        
        void check(String desc) throws CannotCompileException {}

        
        abstract int compile(CtClass type, String name, Bytecode code,
                             CtClass[] parameters, Javac drv)
            throws CannotCompileException;

        
        abstract int compileIfStatic(CtClass type, String name,
                Bytecode code, Javac drv) throws CannotCompileException;

        
        
        int getConstantValue(ConstPool cp, CtClass type) { return 0; }
    }

    static abstract class CodeInitializer0 extends Initializer {
        abstract void compileExpr(Javac drv) throws CompileError;

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            try {
                code.addAload(0);
                compileExpr(drv);
                code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
                return code.getMaxStack();
            }
            catch (CompileError e) {
                throw new CannotCompileException(e);
            }
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            try {
                compileExpr(drv);
                code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
                return code.getMaxStack();
            }
            catch (CompileError e) {
                throw new CannotCompileException(e);
            }
        }

        int getConstantValue2(ConstPool cp, CtClass type, ASTree tree) {
            if (type.isPrimitive()) {
                if (tree instanceof IntConst) {
                    long value = ((IntConst)tree).get();
                    if (type == CtClass.doubleType)
                        return cp.addDoubleInfo(value);
                    else if (type == CtClass.floatType)
                        return cp.addFloatInfo(value);
                    else if (type == CtClass.longType)
                        return cp.addLongInfo(value);
                    else  if (type != CtClass.voidType)
                        return cp.addIntegerInfo((int)value);
                }
                else if (tree instanceof DoubleConst) {
                    double value = ((DoubleConst)tree).get();
                    if (type == CtClass.floatType)
                        return cp.addFloatInfo((float)value);
                    else if (type == CtClass.doubleType)
                        return cp.addDoubleInfo(value);
                }
            }
            else if (tree instanceof StringL
                     && type.getName().equals(javaLangString))
                return cp.addStringInfo(((StringL)tree).get());

            return 0;
        }
    }

    static class CodeInitializer extends CodeInitializer0 {
        private String expression;

        CodeInitializer(String expr) { expression = expr; }

        @Override
        void compileExpr(Javac drv) throws CompileError {
            drv.compileExpr(expression);
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            try {
                ASTree t = Javac.parseExpr(expression, new SymbolTable());
                return getConstantValue2(cp, type, t);
            }
            catch (CompileError e) {
                return 0;
            }
        }
    }

    static class PtreeInitializer extends CodeInitializer0 {
        private ASTree expression;

        PtreeInitializer(ASTree expr) { expression = expr; }

        @Override
        void compileExpr(Javac drv) throws CompileError {
            drv.compileExpr(expression);
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            return getConstantValue2(cp, type, expression);
        }
    }

    
    static class ParamInitializer extends Initializer {
        int nthParam;

        ParamInitializer() {}

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            if (parameters != null && nthParam < parameters.length) {
                code.addAload(0);
                int nth = nthParamToLocal(nthParam, parameters, false);
                int s = code.addLoad(nth, type) + 1;
                code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
                return s;       
            }
            return 0;       
        }

        
        static int nthParamToLocal(int nth, CtClass[] params,
                                   boolean isStatic) {
            CtClass longType = CtClass.longType;
            CtClass doubleType = CtClass.doubleType;
            int k;
            if (isStatic)
                k = 0;
            else
                k = 1;  

            for (int i = 0; i < nth; ++i) {
                CtClass type = params[i];
                if (type == longType || type == doubleType)
                    k += 2;
                else
                    ++k;
            }

            return k;
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            return 0;
        }
    }

    
    static class NewInitializer extends Initializer {
        CtClass objectType;
        String[] stringParams;
        boolean withConstructorParams;

        NewInitializer() {}

        
        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            int stacksize;

            code.addAload(0);
            code.addNew(objectType);
            code.add(Bytecode.DUP);
            code.addAload(0);

            if (stringParams == null)
                stacksize = 4;
            else
                stacksize = compileStringParameter(code) + 4;

            if (withConstructorParams)
                stacksize += CtNewWrappedMethod.compileParameterList(code,
                                                            parameters, 1);

            code.addInvokespecial(objectType, "<init>", getDescriptor());
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return stacksize;
        }

        private String getDescriptor() {
            final String desc3
        = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)V";

            if (stringParams == null)
                if (withConstructorParams)
                    return "(Ljava/lang/Object;[Ljava/lang/Object;)V";
                else
                    return "(Ljava/lang/Object;)V";

            if (withConstructorParams)
                return desc3;

            return "(Ljava/lang/Object;[Ljava/lang/String;)V";
        }

        
        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            String desc;

            code.addNew(objectType);
            code.add(Bytecode.DUP);

            int stacksize = 2;
            if (stringParams == null)
                desc = "()V";
            else {
                desc = "([Ljava/lang/String;)V";
                stacksize += compileStringParameter(code);
            }

            code.addInvokespecial(objectType, "<init>", desc);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return stacksize;
        }

        protected final int compileStringParameter(Bytecode code)
            throws CannotCompileException
        {
            int nparam = stringParams.length;
            code.addIconst(nparam);
            code.addAnewarray(javaLangString);
            for (int j = 0; j < nparam; ++j) {
                code.add(Bytecode.DUP);         
                code.addIconst(j);                      
                code.addLdc(stringParams[j]);   
                code.add(Bytecode.AASTORE);             
            }

            return 4;
        }

    }

    
    static class MethodInitializer extends NewInitializer {
        String methodName;
        

        MethodInitializer() {}

        
        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            int stacksize;

            code.addAload(0);
            code.addAload(0);

            if (stringParams == null)
                stacksize = 2;
            else
                stacksize = compileStringParameter(code) + 2;

            if (withConstructorParams)
                stacksize += CtNewWrappedMethod.compileParameterList(code,
                                                            parameters, 1);

            String typeDesc = Descriptor.of(type);
            String mDesc = getDescriptor() + typeDesc;
            code.addInvokestatic(objectType, methodName, mDesc);
            code.addPutfield(Bytecode.THIS, name, typeDesc);
            return stacksize;
        }

        private String getDescriptor() {
            final String desc3
                = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)";

            if (stringParams == null)
                if (withConstructorParams)
                    return "(Ljava/lang/Object;[Ljava/lang/Object;)";
                else
                    return "(Ljava/lang/Object;)";

            if (withConstructorParams)
                return desc3;

            return "(Ljava/lang/Object;[Ljava/lang/String;)";
        }

        
        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            String desc;

            int stacksize = 1;
            if (stringParams == null)
                desc = "()";
            else {
                desc = "([Ljava/lang/String;)";
                stacksize += compileStringParameter(code);
            }

            String typeDesc = Descriptor.of(type);
            code.addInvokestatic(objectType, methodName, desc + typeDesc);
            code.addPutstatic(Bytecode.THIS, name, typeDesc);
            return stacksize;
        }
    }

    static class IntInitializer extends Initializer {
        int value;

        IntInitializer(int v) { value = v; }

        @Override
        void check(String desc) throws CannotCompileException {
            char c = desc.charAt(0);
            if (c != 'I' && c != 'S' && c != 'B' && c != 'C' && c != 'Z')
                throw new CannotCompileException("type mismatch");
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addIconst(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addIconst(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            return cp.addIntegerInfo(value);
        }
    }

    static class LongInitializer extends Initializer {
        long value;

        LongInitializer(long v) { value = v; }

        @Override
        void check(String desc) throws CannotCompileException {
            if (!desc.equals("J"))
                throw new CannotCompileException("type mismatch");
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc2w(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc2w(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.longType)
                return cp.addLongInfo(value);
            return 0;
        }
    }

    static class FloatInitializer extends Initializer {
        float value;

        FloatInitializer(float v) { value = v; }

        @Override
        void check(String desc) throws CannotCompileException {
            if (!desc.equals("F"))
                throw new CannotCompileException("type mismatch");
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addFconst(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addFconst(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.floatType)
                return cp.addFloatInfo(value);
            return 0;
        }
    }

    static class DoubleInitializer extends Initializer {
        double value;

        DoubleInitializer(double v) { value = v; }

        @Override
        void check(String desc) throws CannotCompileException {
            if (!desc.equals("D"))
                throw new CannotCompileException("type mismatch");
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc2w(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 3;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc2w(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            if (type == CtClass.doubleType)
                return cp.addDoubleInfo(value);
            return 0;
        }
    }

    static class StringInitializer extends Initializer {
        String value;

        StringInitializer(String v) { value = v; }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            code.addLdc(value);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            code.addLdc(value);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   
        }

        @Override
        int getConstantValue(ConstPool cp, CtClass type) {
            if (type.getName().equals(javaLangString))
                return cp.addStringInfo(value);
            return 0;
        }
    }

    static class ArrayInitializer extends Initializer {
        CtClass type;
        int size;

        ArrayInitializer(CtClass t, int s) { type = t; size = s; }

        private void addNewarray(Bytecode code) {
            if (type.isPrimitive())
                code.addNewarray(((CtPrimitiveType)type).getArrayType(),
                                 size);
            else
                code.addAnewarray(type, size);
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            addNewarray(code);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return 2;   
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            addNewarray(code);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return 1;   
        }
    }

    static class MultiArrayInitializer extends Initializer {
        CtClass type;
        int[] dim;

        MultiArrayInitializer(CtClass t, int[] d) { type = t; dim = d; }

        @Override
        void check(String desc) throws CannotCompileException {
            if (desc.charAt(0) != '[')
                throw new CannotCompileException("type mismatch");
        }

        @Override
        int compile(CtClass type, String name, Bytecode code,
                    CtClass[] parameters, Javac drv)
            throws CannotCompileException
        {
            code.addAload(0);
            int s = code.addMultiNewarray(type, dim);
            code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
            return s + 1;       
        }

        @Override
        int compileIfStatic(CtClass type, String name, Bytecode code,
                            Javac drv) throws CannotCompileException
        {
            int s = code.addMultiNewarray(type, dim);
            code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
            return s;   
        }
    }
}
