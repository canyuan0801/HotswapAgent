
package org.hotswap.agent.javassist.bytecode.analysis;

import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.javassist.CtClass;




public class MultiType extends Type {
    private Map<String,CtClass> interfaces;
    private Type resolved;
    private Type potentialClass;
    private MultiType mergeSource;
    private boolean changed = false;

    public MultiType(Map<String,CtClass> interfaces) {
        this(interfaces, null);
    }

    public MultiType(Map<String,CtClass> interfaces, Type potentialClass) {
        super(null);
        this.interfaces = interfaces;
        this.potentialClass = potentialClass;
    }


    @Override
    public CtClass getCtClass() {
        if (resolved != null)
            return resolved.getCtClass();

        return Type.OBJECT.getCtClass();
    }


    @Override
    public Type getComponent() {
        return null;
    }


    @Override
    public int getSize() {
        return 1;
    }


    @Override
    public boolean isArray() {
        return false;
    }


    @Override
    boolean popChanged() {
        boolean changed = this.changed;
        this.changed = false;
        return changed;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isAssignableTo(Type type) {
        if (resolved != null)
            return type.isAssignableFrom(resolved);

        if (Type.OBJECT.equals(type))
            return true;

        if (potentialClass != null && !type.isAssignableFrom(potentialClass))
            potentialClass = null;

        Map<String,CtClass> map = mergeMultiAndSingle(this, type);

        if (map.size() == 1 && potentialClass == null) {

            resolved = Type.get(map.values().iterator().next());
            propogateResolved();

            return true;
        }


        if (map.size() >= 1) {
            interfaces = map;
            propogateState();

            return true;
        }

        if (potentialClass != null) {
            resolved = potentialClass;
            propogateResolved();

            return true;
        }

        return false;
    }

    private void propogateState() {
        MultiType source = mergeSource;
        while (source != null) {
            source.interfaces = interfaces;
            source.potentialClass = potentialClass;
            source = source.mergeSource;
        }
    }

    private void propogateResolved() {
        MultiType source = mergeSource;
        while (source != null) {
            source.resolved = resolved;
            source = source.mergeSource;
        }
    }


    @Override
    public boolean isReference() {
       return true;
    }

    private Map<String,CtClass> getAllMultiInterfaces(MultiType type) {
        Map<String,CtClass> map = new HashMap<String,CtClass>();

        for (CtClass intf:type.interfaces.values()) {
            map.put(intf.getName(), intf);
            getAllInterfaces(intf, map);
        }

        return map;
    }


    private Map<String,CtClass> mergeMultiInterfaces(MultiType type1, MultiType type2) {
        Map<String,CtClass> map1 = getAllMultiInterfaces(type1);
        Map<String,CtClass> map2 = getAllMultiInterfaces(type2);

        return findCommonInterfaces(map1, map2);
    }

    private Map<String,CtClass> mergeMultiAndSingle(MultiType multi, Type single) {
        Map<String,CtClass> map1 = getAllMultiInterfaces(multi);
        Map<String,CtClass> map2 = getAllInterfaces(single.getCtClass(), null);

        return findCommonInterfaces(map1, map2);
    }

    private boolean inMergeSource(MultiType source) {
        while (source != null) {
            if (source == this)
                return true;

            source = source.mergeSource;
        }

        return false;
    }

    @Override
    public Type merge(Type type) {
        if (this == type)
            return this;

        if (type == UNINIT)
            return this;

        if (type == BOGUS)
            return BOGUS;

        if (type == null)
            return this;

        if (resolved != null)
            return resolved.merge(type);

        if (potentialClass != null) {
            Type mergePotential = potentialClass.merge(type);
            if (! mergePotential.equals(potentialClass) || mergePotential.popChanged()) {
                potentialClass = Type.OBJECT.equals(mergePotential) ? null : mergePotential;
                changed = true;
            }
        }

        Map<String,CtClass> merged;

        if (type instanceof MultiType) {
            MultiType multi = (MultiType)type;

            if (multi.resolved != null) {
                merged = mergeMultiAndSingle(this, multi.resolved);
            } else {
                merged = mergeMultiInterfaces(multi, this);
                if (! inMergeSource(multi))
                    mergeSource = multi;
            }
        } else {
            merged = mergeMultiAndSingle(this, type);
        }


        if (merged.size() > 1 || (merged.size() == 1 && potentialClass != null)) {

            if (merged.size() != interfaces.size())
                changed = true;
            else if (changed == false)
                for (String key:merged.keySet())
                    if (!interfaces.containsKey(key))
                        changed = true;


            interfaces = merged;
            propogateState();

            return this;
        }

        if (merged.size() == 1)
            resolved = Type.get(merged.values().iterator().next());
        else if (potentialClass != null)
            resolved = potentialClass;
        else
            resolved = OBJECT;

        propogateResolved();

        return resolved;
    }

    @Override
    public int hashCode() {
        if (resolved != null)
            return resolved.hashCode();

        return interfaces.keySet().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof MultiType))
            return false;

        MultiType multi = (MultiType) o;
        if (resolved != null)
            return resolved.equals(multi.resolved);
        else if (multi.resolved != null)
            return false;

        return interfaces.keySet().equals(multi.interfaces.keySet());
    }

    @Override
    public String toString() {
        if (resolved != null)
            return resolved.toString();

        StringBuffer buffer = new StringBuffer("{");
        for (String key:interfaces.keySet())
            buffer.append(key).append(", ");
        if (potentialClass != null)
            buffer.append("*").append(potentialClass.toString());
        else
            buffer.setLength(buffer.length() - 2);
        buffer.append("}");
        return buffer.toString();
    }
}
