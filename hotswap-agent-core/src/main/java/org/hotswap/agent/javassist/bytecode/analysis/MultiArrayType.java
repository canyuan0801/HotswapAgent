
package org.hotswap.agent.javassist.bytecode.analysis;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;


public class MultiArrayType extends Type {
    private MultiType component;
    private int dims;

    public MultiArrayType(MultiType component, int dims) {
        super(null);
        this.component = component;
        this.dims = dims;
    }

    @Override
    public CtClass getCtClass() {
        CtClass clazz = component.getCtClass();
        if (clazz == null)
            return null;

        ClassPool pool = clazz.getClassPool();
        if (pool == null)
            pool = ClassPool.getDefault();

        String name = arrayName(clazz.getName(), dims);

        try {
            return pool.get(name);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    boolean popChanged() {
        return component.popChanged();
    }

    @Override
    public int getDimensions() {
        return dims;
    }

    @Override
    public Type getComponent() {
       return dims == 1 ? (Type)component : new MultiArrayType(component, dims - 1);
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isReference() {
       return true;
    }

    public boolean isAssignableTo(Type type) {
        if (eq(type.getCtClass(), Type.OBJECT.getCtClass()))
            return true;

        if (eq(type.getCtClass(), Type.CLONEABLE.getCtClass()))
            return true;

        if (eq(type.getCtClass(), Type.SERIALIZABLE.getCtClass()))
            return true;

        if (! type.isArray())
            return false;

        Type typeRoot = getRootComponent(type);
        int typeDims = type.getDimensions();

        if (typeDims > dims)
            return false;

        if (typeDims < dims) {
            if (eq(typeRoot.getCtClass(), Type.OBJECT.getCtClass()))
                return true;

            if (eq(typeRoot.getCtClass(), Type.CLONEABLE.getCtClass()))
                return true;

            if (eq(typeRoot.getCtClass(), Type.SERIALIZABLE.getCtClass()))
                return true;

            return false;
        }

        return component.isAssignableTo(typeRoot);
    }


    @Override
    public int hashCode() {
        return component.hashCode() + dims;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof MultiArrayType))
            return false;
        MultiArrayType multi = (MultiArrayType)o;

        return component.equals(multi.component) && dims == multi.dims;
    }

    @Override
    public String toString() {

        return arrayName(component.toString(), dims);
    }
}
