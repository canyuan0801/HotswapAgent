

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;

public class TransformCall extends Transformer {
    protected String classname, methodname, methodDescriptor;
    protected String newClassname, newMethodname;
    protected boolean newMethodIsPrivate;

    
    protected int newIndex;
    protected ConstPool constPool;

    public TransformCall(Transformer next, CtMethod origMethod,
                         CtMethod substMethod)
    {
        this(next, origMethod.getName(), substMethod);
        classname = origMethod.getDeclaringClass().getName();
    }

    public TransformCall(Transformer next, String oldMethodName,
                         CtMethod substMethod)
    {
        super(next);
        methodname = oldMethodName;
        methodDescriptor = substMethod.getMethodInfo2().getDescriptor();
        classname = newClassname = substMethod.getDeclaringClass().getName(); 
        newMethodname = substMethod.getName();
        constPool = null;
        newMethodIsPrivate = Modifier.isPrivate(substMethod.getModifiers());
    }

    @Override
    public void initialize(ConstPool cp, CodeAttribute attr) {
        if (constPool != cp)
            newIndex = 0;
    }

    
    @Override
    public int transform(CtClass clazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws BadBytecode
    {
        int c = iterator.byteAt(pos);
        if (c == INVOKEINTERFACE || c == INVOKESPECIAL
                        || c == INVOKESTATIC || c == INVOKEVIRTUAL) {
            int index = iterator.u16bitAt(pos + 1);
            String cname = cp.eqMember(methodname, methodDescriptor, index);
            if (cname != null && matchClass(cname, clazz.getClassPool())) {
                int ntinfo = cp.getMemberNameAndType(index);
                pos = match(c, pos, iterator,
                            cp.getNameAndTypeDescriptor(ntinfo), cp);
            }
        }

        return pos;
    }

    private boolean matchClass(String name, ClassPool pool) {
        if (classname.equals(name))
            return true;

        try {
            CtClass clazz = pool.get(name);
            CtClass declClazz = pool.get(classname);
            if (clazz.subtypeOf(declClazz))
                try {
                    CtMethod m = clazz.getMethod(methodname, methodDescriptor);
                    return m.getDeclaringClass().getName().equals(classname);
                }
                catch (NotFoundException e) {
                    
                    return true;
                }
        }
        catch (NotFoundException e) {
            return false;
        }

        return false;
    }

    protected int match(int c, int pos, CodeIterator iterator,
                        int typedesc, ConstPool cp) throws BadBytecode
    {
        if (newIndex == 0) {
            int nt = cp.addNameAndTypeInfo(cp.addUtf8Info(newMethodname),
                                           typedesc);
            int ci = cp.addClassInfo(newClassname);
            if (c == INVOKEINTERFACE)
                newIndex = cp.addInterfaceMethodrefInfo(ci, nt);
            else {
                if (newMethodIsPrivate && c == INVOKEVIRTUAL)
                    iterator.writeByte(INVOKESPECIAL, pos);

                newIndex = cp.addMethodrefInfo(ci, nt);
            }

            constPool = cp;
        }

        iterator.write16bit(newIndex, pos + 1);
        return pos;
    }
}
