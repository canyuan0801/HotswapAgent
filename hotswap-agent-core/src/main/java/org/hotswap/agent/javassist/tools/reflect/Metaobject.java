

package org.hotswap.agent.javassist.tools.reflect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;


public class Metaobject implements Serializable {
    
    private static final long serialVersionUID = 1L;
    protected ClassMetaobject classmetaobject;
    protected Metalevel baseobject;
    protected Method[] methods;

    
    public Metaobject(Object self, Object[] args) {
        baseobject = (Metalevel)self;
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();
    }

    
    protected Metaobject() {
        baseobject = null;
        classmetaobject = null;
        methods = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(baseobject);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        baseobject = (Metalevel)in.readObject();
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();
    }

    
    public final ClassMetaobject getClassMetaobject() {
        return classmetaobject;
    }

    
    public final Object getObject() {
        return baseobject;
    }

    
    public final void setObject(Object self) {
        baseobject = (Metalevel)self;
        classmetaobject = baseobject._getClass();
        methods = classmetaobject.getReflectiveMethods();

        
        baseobject._setMetaobject(this);
    }

    
    public final String getMethodName(int identifier) {
        String mname = methods[identifier].getName();
        int j = ClassMetaobject.methodPrefixLen;
        for (;;) {
            char c = mname.charAt(j++);
            if (c < '0' || '9' < c)
                break;
        }

        return mname.substring(j);
    }

    
    public final Class<?>[] getParameterTypes(int identifier) {
        return methods[identifier].getParameterTypes();
    }

    
    public final Class<?> getReturnType(int identifier) {
        return methods[identifier].getReturnType();
    }

    
    public Object trapFieldRead(String name) {
        Class<?> jc = getClassMetaobject().getJavaClass();
        try {
            return jc.getField(name).get(getObject());
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    
    public void trapFieldWrite(String name, Object value) {
        Class<?> jc = getClassMetaobject().getJavaClass();
        try {
            jc.getField(name).set(getObject(), value);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.toString());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    
    public Object trapMethodcall(int identifier, Object[] args) 
        throws Throwable
    {
        try {
            return methods[identifier].invoke(getObject(), args);
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
        catch (java.lang.IllegalAccessException e) {
            throw new CannotInvokeException(e);
        }
    }
}
