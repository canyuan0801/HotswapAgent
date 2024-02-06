

package org.hotswap.agent.javassist;


final class CtArray extends CtClass
{
    protected ClassPool pool;


    CtArray(String name, ClassPool cp)
    {
        super(name);
        pool = cp;
    }

    @Override
    public ClassPool getClassPool()
    {
        return pool;
    }

    @Override
    public boolean isArray()
    {
        return true;
    }

    private CtClass[] interfaces = null;

    @Override
    public int getModifiers()
    {
        int mod = Modifier.FINAL;
        try {
            mod |= getComponentType().getModifiers()
                   & (Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE);
        }
        catch (NotFoundException e) {}
        return mod;
    }

    @Override
    public CtClass[] getInterfaces() throws NotFoundException
    {
        if (interfaces == null) {
            Class<?>[] intfs = Object[].class.getInterfaces();


            interfaces = new CtClass[intfs.length];
            for (int i = 0; i < intfs.length; i++)
                interfaces[i] = pool.get(intfs[i].getName());
        }

        return interfaces;
    }

    @Override
    public boolean subtypeOf(CtClass clazz) throws NotFoundException
    {
        if (super.subtypeOf(clazz))
            return true;

        String cname = clazz.getName();
        if (cname.equals(javaLangObject))
            return true;

        CtClass[] intfs = getInterfaces();
        for (int i = 0; i < intfs.length; i++)
            if (intfs[i].subtypeOf(clazz))
                return true;

        return clazz.isArray()
            && getComponentType().subtypeOf(clazz.getComponentType());
    }

    @Override
    public CtClass getComponentType() throws NotFoundException
    {
        String name = getName();
        return pool.get(name.substring(0, name.length() - 2));
    }

    @Override
    public CtClass getSuperclass() throws NotFoundException
    {
        return pool.get(javaLangObject);
    }

    public String getSuperclassName() throws NotFoundException
    {
        CtClass superclass = getSuperclass();
        return superclass != null ? superclass.getName() : null;
    }

    @Override
    public CtMethod[] getMethods()
    {
        try {
            return getSuperclass().getMethods();
        }
        catch (NotFoundException e) {
            return super.getMethods();
        }
    }

    @Override
    public CtMethod getMethod(String name, String desc)
        throws NotFoundException
    {
        return getSuperclass().getMethod(name, desc);
    }

    @Override
    public CtConstructor[] getConstructors()
    {
        try {
            return getSuperclass().getConstructors();
        }
        catch (NotFoundException e) {
            return super.getConstructors();
        }
    }
}
