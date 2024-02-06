

package org.hotswap.agent.javassist;

import java.io.DataOutputStream;
import java.io.IOException;

import org.hotswap.agent.javassist.bytecode.ClassFile;

class CtNewClass extends CtClassType {

    protected boolean hasConstructor;

    CtNewClass(String name, ClassPool cp,
               boolean isInterface, CtClass superclass) {
        super(name, cp);
        wasChanged = true;
        String superName;
        if (isInterface || superclass == null)
            superName = null;
        else
            superName = superclass.getName();

        classfile = new ClassFile(isInterface, name, superName);
        if (isInterface && superclass != null)
            classfile.setInterfaces(new String[] { superclass.getName() });

        setModifiers(Modifier.setPublic(getModifiers()));
        hasConstructor = isInterface;
    }

    @Override
    protected void extendToString(StringBuffer buffer) {
        if (hasConstructor)
            buffer.append("hasConstructor ");

        super.extendToString(buffer);
    }

    @Override
    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        hasConstructor = true;
        super.addConstructor(c);
    }

    @Override
    public void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        if (!hasConstructor)
            try {
                inheritAllConstructors();
                hasConstructor = true;
            }
            catch (NotFoundException e) {
                throw new CannotCompileException(e);
            }

        super.toBytecode(out);
    }


    public void inheritAllConstructors()
        throws CannotCompileException, NotFoundException
    {
        CtClass superclazz;
        CtConstructor[] cs;

        superclazz = getSuperclass();
        cs = superclazz.getDeclaredConstructors();

        int n = 0;
        for (int i = 0; i < cs.length; ++i) {
            CtConstructor c = cs[i];
            int mod = c.getModifiers();
            if (isInheritable(mod, superclazz)) {
                CtConstructor cons
                    = CtNewConstructor.make(c.getParameterTypes(),
                                            c.getExceptionTypes(), this);
                cons.setModifiers(mod & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE));
                addConstructor(cons);
                ++n;
            }
        }

        if (n < 1)
            throw new CannotCompileException(
                        "no inheritable constructor in " + superclazz.getName());

    }

    private boolean isInheritable(int mod, CtClass superclazz) {
        if (Modifier.isPrivate(mod))
            return false;

        if (Modifier.isPackage(mod)) {
            String pname = getPackageName();
            String pname2 = superclazz.getPackageName();
            if (pname == null)
                return pname2 == null;
            return pname.equals(pname2);
        }

        return true;
    }
}
