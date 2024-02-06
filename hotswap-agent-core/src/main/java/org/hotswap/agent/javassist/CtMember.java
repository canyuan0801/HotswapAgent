

package org.hotswap.agent.javassist;


public abstract class CtMember {
    CtMember next;
    protected CtClass declaringClass;


    static class Cache extends CtMember {
        @Override
        protected void extendToString(StringBuffer buffer) {}
        @Override
        public boolean hasAnnotation(String clz) { return false; }
        @Override
        public Object getAnnotation(Class<?> clz)
            throws ClassNotFoundException { return null; }
        @Override
        public Object[] getAnnotations()
            throws ClassNotFoundException { return null; }
        @Override
        public byte[] getAttribute(String name) { return null; }
        @Override
        public Object[] getAvailableAnnotations() { return null; }
        @Override
        public int getModifiers() { return 0; }
        @Override
        public String getName() { return null; }
        @Override
        public String getSignature() { return null; }
        @Override
        public void setAttribute(String name, byte[] data) {}
        @Override
        public void setModifiers(int mod) {}
        @Override
        public String getGenericSignature() { return null; }
        @Override
        public void setGenericSignature(String sig) {}

        private CtMember methodTail;
        private CtMember consTail;
        private CtMember fieldTail;

        Cache(CtClassType decl) {
            super(decl);
            methodTail = this;
            consTail = this;
            fieldTail = this;
            fieldTail.next = this;
        }

        CtMember methodHead() { return this; }
        CtMember lastMethod() { return methodTail; }
        CtMember consHead() { return methodTail; }
        CtMember lastCons() { return consTail; }
        CtMember fieldHead() { return consTail; }
        CtMember lastField() { return fieldTail; }

        void addMethod(CtMember method) {
            method.next = methodTail.next;
            methodTail.next = method;
            if (methodTail == consTail) {
                consTail = method;
                if (methodTail == fieldTail)
                    fieldTail = method;
            }

            methodTail = method;
        }


        void addConstructor(CtMember cons) {
            cons.next = consTail.next;
            consTail.next = cons;
            if (consTail == fieldTail)
                fieldTail = cons;

            consTail = cons;
        }

        void addField(CtMember field) {
            field.next = this;
            fieldTail.next = field;
            fieldTail = field;
        }

        static int count(CtMember head, CtMember tail) {
            int n = 0;
            while (head != tail) {
                n++;
                head = head.next;
            }

            return n;
        }

        void remove(CtMember mem) {
            CtMember m = this;
            CtMember node;
            while ((node = m.next) != this) {
                if (node == mem) {
                    m.next = node.next;
                    if (node == methodTail)
                        methodTail = m;

                    if (node == consTail)
                        consTail = m;

                    if (node == fieldTail)
                        fieldTail = m;

                    break;
                }
                m = m.next;
            }
        }
    }

    protected CtMember(CtClass clazz) {
        declaringClass = clazz;
        next = null;
    }

    final CtMember next() { return next; }


    void nameReplaced() {}

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(getClass().getName());
        buffer.append("@");
        buffer.append(Integer.toHexString(hashCode()));
        buffer.append("[");
        buffer.append(Modifier.toString(getModifiers()));
        extendToString(buffer);
        buffer.append("]");
        return buffer.toString();
    }


    protected abstract void extendToString(StringBuffer buffer);


    public CtClass getDeclaringClass() { return declaringClass; }


    public boolean visibleFrom(CtClass clazz) {
        int mod = getModifiers();
        if (Modifier.isPublic(mod))
            return true;
        else if (Modifier.isPrivate(mod))
            return clazz == declaringClass;
        else {
            String declName = declaringClass.getPackageName();
            String fromName = clazz.getPackageName();
            boolean visible;
            if (declName == null)
                visible = fromName == null;
            else
                visible = declName.equals(fromName);

            if (!visible && Modifier.isProtected(mod))
                return clazz.subclassOf(declaringClass);

            return visible;
        }
    }


    public abstract int getModifiers();


    public abstract void setModifiers(int mod);


    public boolean hasAnnotation(Class<?> clz) {
        return hasAnnotation(clz.getName());
    }


    public abstract boolean hasAnnotation(String annotationTypeName);


    public abstract Object getAnnotation(Class<?> annotationType) throws ClassNotFoundException;


    public abstract Object[] getAnnotations() throws ClassNotFoundException;


    public abstract Object[] getAvailableAnnotations();


    public abstract String getName();


    public abstract String getSignature();


    public abstract String getGenericSignature();


    public abstract void setGenericSignature(String sig);


    public abstract byte[] getAttribute(String name);


    public abstract void setAttribute(String name, byte[] data);
}
