

package org.hotswap.agent.javassist.util.proxy;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.invoke.MethodHandles.Lookup;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.bytecode.AccessFlag;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.DuplicateMemberException;
import org.hotswap.agent.javassist.bytecode.ExceptionsAttribute;
import org.hotswap.agent.javassist.bytecode.FieldInfo;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.bytecode.StackMapTable;




public class ProxyFactory {
    private Class<?> superClass;
    private Class<?>[] interfaces;
    private MethodFilter methodFilter;
    private MethodHandler handler;
    private List<Map.Entry<String,Method>> signatureMethods;
    private boolean hasGetHandler;
    private byte[] signature;
    private String classname;
    private String basename;
    private String superName;
    private Class<?> thisClass;

    private boolean factoryUseCache;

    private boolean factoryWriteReplace;


    public static boolean onlyPublicMethods = false;


    public String writeDirectory;

    private static final Class<?> OBJECT_TYPE = Object.class;

    private static final String HOLDER = "_methods_";
    private static final String HOLDER_TYPE = "[Ljava/lang/reflect/Method;";
    private static final String FILTER_SIGNATURE_FIELD = "_filter_signature";
    private static final String FILTER_SIGNATURE_TYPE = "[B";
    private static final String HANDLER = "handler";
    private static final String NULL_INTERCEPTOR_HOLDER = "org.hotswap.agent.javassist.util.proxy.RuntimeSupport";
    private static final String DEFAULT_INTERCEPTOR = "default_interceptor";
    private static final String HANDLER_TYPE
        = 'L' + MethodHandler.class.getName().replace('.', '/') + ';';
    private static final String HANDLER_SETTER = "setHandler";
    private static final String HANDLER_SETTER_TYPE = "(" + HANDLER_TYPE + ")V";

    private static final String HANDLER_GETTER = "getHandler";
    private static final String HANDLER_GETTER_TYPE = "()" + HANDLER_TYPE;

    private static final String SERIAL_VERSION_UID_FIELD = "serialVersionUID";
    private static final String SERIAL_VERSION_UID_TYPE = "J";
    private static final long SERIAL_VERSION_UID_VALUE = -1L;


    public static volatile boolean useCache = true;


    public static volatile boolean useWriteReplace = true;




    public boolean isUseCache()
    {
        return factoryUseCache;
    }


    public void setUseCache(boolean useCache)
    {


        if (handler != null && useCache) {
            throw new RuntimeException("caching cannot be enabled if the factory default interceptor has been set");
        }
        factoryUseCache = useCache;
    }


    public boolean isUseWriteReplace()
    {
        return factoryWriteReplace;
    }


    public void setUseWriteReplace(boolean useWriteReplace)
    {
        factoryWriteReplace = useWriteReplace;
    }

    private static Map<ClassLoader,Map<String,ProxyDetails>> proxyCache =
            new WeakHashMap<ClassLoader,Map<String,ProxyDetails>>();


    public static boolean isProxyClass(Class<?> cl)
    {

        return (Proxy.class.isAssignableFrom(cl));
    }


    static class ProxyDetails {

        byte[] signature;

        Reference<Class<?>> proxyClass;

        boolean isUseWriteReplace;

        ProxyDetails(byte[] signature, Class<?> proxyClass, boolean isUseWriteReplace)
        {
            this.signature = signature;
            this.proxyClass = new WeakReference<Class<?>>(proxyClass);
            this.isUseWriteReplace = isUseWriteReplace;
        }
    }


    public ProxyFactory() {
        superClass = null;
        interfaces = null;
        methodFilter = null;
        handler = null;
        signature = null;
        signatureMethods = null;
        hasGetHandler = false;
        thisClass = null;
        writeDirectory = null;
        factoryUseCache = useCache;
        factoryWriteReplace = useWriteReplace;
    }


    public void setSuperclass(Class<?> clazz) {
        superClass = clazz;

        signature = null;
    }


    public Class<?> getSuperclass() { return superClass; }


    public void setInterfaces(Class<?>[] ifs) {
        interfaces = ifs;

        signature = null;
    }


    public Class<?>[] getInterfaces() { return interfaces; }


    public void setFilter(MethodFilter mf) {
        methodFilter = mf;

        signature = null;
    }


    public Class<?> createClass() {
        if (signature == null) {
            computeSignature(methodFilter);
        }
        return createClass1(null);
    }


    public Class<?> createClass(MethodFilter filter) {
        computeSignature(filter);
        return createClass1(null);
    }


    Class<?> createClass(byte[] signature)
    {
        installSignature(signature);
        return createClass1(null);
    }


    public Class<?> createClass(Lookup lookup) {
        if (signature == null) {
            computeSignature(methodFilter);
        }
        return createClass1(lookup);
    }


    public Class<?> createClass(Lookup lookup, MethodFilter filter) {
        computeSignature(filter);
        return createClass1(lookup);
    }


    Class<?> createClass(Lookup lookup, byte[] signature)
    {
        installSignature(signature);
        return createClass1(lookup);
    }

    private Class<?> createClass1(Lookup lookup) {
        Class<?> result = thisClass;
        if (result == null) {
            ClassLoader cl = getClassLoader();
            synchronized (proxyCache) {
                if (factoryUseCache)
                    createClass2(cl, lookup);
                else
                    createClass3(cl, lookup);

                result = thisClass;

                thisClass = null;
            }
        }

        return result;
    }

    private static char[] hexDigits =
            { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public String getKey(Class<?> superClass, Class<?>[] interfaces, byte[] signature, boolean useWriteReplace)
    {
        StringBuffer sbuf = new StringBuffer();
        if (superClass != null){
            sbuf.append(superClass.getName());
        }
        sbuf.append(":");
        for (int i = 0; i < interfaces.length; i++) {
            sbuf.append(interfaces[i].getName());
            sbuf.append(":");
        }
        for (int i = 0; i < signature.length; i++) {
            byte b = signature[i];
            int lo = b & 0xf;
            int hi = (b >> 4) & 0xf;
            sbuf.append(hexDigits[lo]);
            sbuf.append(hexDigits[hi]);
        }
        if (useWriteReplace) {
            sbuf.append(":w");
        }

        return sbuf.toString();
    }

    private void createClass2(ClassLoader cl, Lookup lookup) {
        String key = getKey(superClass, interfaces, signature, factoryWriteReplace);


            Map<String,ProxyDetails> cacheForTheLoader = proxyCache.get(cl);
            ProxyDetails details;
            if (cacheForTheLoader == null) {
                cacheForTheLoader = new HashMap<String,ProxyDetails>();
                proxyCache.put(cl, cacheForTheLoader);
            }
            details = cacheForTheLoader.get(key);
            if (details != null) {
                Reference<Class<?>> reference = details.proxyClass;
                thisClass = reference.get();
                if (thisClass != null) {
                    return;
                }
            }
            createClass3(cl, lookup);
            details = new  ProxyDetails(signature, thisClass, factoryWriteReplace);
            cacheForTheLoader.put(key, details);

    }

    private void createClass3(ClassLoader cl, Lookup lookup) {

        allocateClassName();

        try {
            ClassFile cf = make();
            if (writeDirectory != null)
                FactoryHelper.writeFile(cf, writeDirectory);

            if (lookup == null)
                thisClass = FactoryHelper.toClass(cf, getClassInTheSamePackage(), cl, getDomain());
            else
                thisClass = FactoryHelper.toClass(cf, lookup);

            setField(FILTER_SIGNATURE_FIELD, signature);

            if (!factoryUseCache) {
                setField(DEFAULT_INTERCEPTOR, handler);
            }
        }
        catch (CannotCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }


    private Class<?> getClassInTheSamePackage() {
        if (basename.startsWith("org.hotswap.agent.javassist.util.proxy."))
            return this.getClass();
        else if (superClass != null && superClass != OBJECT_TYPE)
            return superClass;
        else if (interfaces != null && interfaces.length > 0)
            return interfaces[0];
        else
            return this.getClass();
    }

    private void setField(String fieldName, Object value) {
        if (thisClass != null && value != null)
            try {
                Field f = thisClass.getField(fieldName);
                SecurityActions.setAccessible(f, true);
                f.set(null, value);
                SecurityActions.setAccessible(f, false);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    static byte[] getFilterSignature(Class<?> clazz) {
        return (byte[])getField(clazz, FILTER_SIGNATURE_FIELD);
    }

    private static Object getField(Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getField(fieldName);
            f.setAccessible(true);
            Object value = f.get(null);
            f.setAccessible(false);
            return value;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static MethodHandler getHandler(Proxy p) {
        try {
            Field f = p.getClass().getDeclaredField(HANDLER);
            f.setAccessible(true);
            Object value = f.get(p);
            f.setAccessible(false);
            return (MethodHandler)value;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static interface ClassLoaderProvider {

        public ClassLoader get(ProxyFactory pf);
    }


    public static ClassLoaderProvider classLoaderProvider =
        new ClassLoaderProvider() {
            @Override
            public ClassLoader get(ProxyFactory pf) {
                  return pf.getClassLoader0();
              }
        };

    protected ClassLoader getClassLoader() {
        return classLoaderProvider.get(this);
    }

    protected ClassLoader getClassLoader0() {
        ClassLoader loader = null;
        if (superClass != null && !superClass.getName().equals("java.lang.Object"))
            loader = superClass.getClassLoader();
        else if (interfaces != null && interfaces.length > 0)
            loader = interfaces[0].getClassLoader();

        if (loader == null) {
            loader = getClass().getClassLoader();

            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
                if (loader == null)
                    loader = ClassLoader.getSystemClassLoader();
            }
        }

        return loader;
    }

    protected ProtectionDomain getDomain() {
        Class<?> clazz;
        if (superClass != null && !superClass.getName().equals("java.lang.Object"))
            clazz = superClass;
        else if (interfaces != null && interfaces.length > 0)
            clazz = interfaces[0];
        else
            clazz = this.getClass();

        return clazz.getProtectionDomain();
    }


    public Object create(Class<?>[] paramTypes, Object[] args, MethodHandler mh)
        throws NoSuchMethodException, IllegalArgumentException,
               InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Object obj = create(paramTypes, args);
        ((Proxy)obj).setHandler(mh);
        return obj;
    }


    public Object create(Class<?>[] paramTypes, Object[] args)
        throws NoSuchMethodException, IllegalArgumentException,
               InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class<?> c = createClass();
        Constructor<?> cons = c.getConstructor(paramTypes);
        return cons.newInstance(args);
    }


    @Deprecated
    public void setHandler(MethodHandler mi) {

        if (factoryUseCache && mi != null)  {
            factoryUseCache = false;

          thisClass  = null;
        }
        handler = mi;


        setField(DEFAULT_INTERCEPTOR, handler);
    }


    public static interface UniqueName {

        String get(String classname);
    }


    public static UniqueName nameGenerator = new UniqueName() {
        private final String sep = "_$$_jvst" + Integer.toHexString(this.hashCode() & 0xfff) + "_";
        private int counter = 0;

        @Override
        public String get(String classname) {
            return classname + sep + Integer.toHexString(counter++);
        }
    };

    private static String makeProxyName(String classname) {
        synchronized (nameGenerator) {
            return nameGenerator.get(classname);
        }
    }

    private ClassFile make() throws CannotCompileException {
        ClassFile cf = new ClassFile(false, classname, superName);
        cf.setAccessFlags(AccessFlag.PUBLIC);
        setInterfaces(cf, interfaces, hasGetHandler ? Proxy.class : ProxyObject.class);
        ConstPool pool = cf.getConstPool();


        if  (!factoryUseCache) {
            FieldInfo finfo = new FieldInfo(pool, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            finfo.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
            cf.addField(finfo);
        }


        FieldInfo finfo2 = new FieldInfo(pool, HANDLER, HANDLER_TYPE);
        finfo2.setAccessFlags(AccessFlag.PRIVATE);
        cf.addField(finfo2);


        FieldInfo finfo3 = new FieldInfo(pool, FILTER_SIGNATURE_FIELD, FILTER_SIGNATURE_TYPE);
        finfo3.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC);
        cf.addField(finfo3);


        FieldInfo finfo4 = new FieldInfo(pool, SERIAL_VERSION_UID_FIELD, SERIAL_VERSION_UID_TYPE);
        finfo4.setAccessFlags(AccessFlag.PUBLIC | AccessFlag.STATIC| AccessFlag.FINAL);
        cf.addField(finfo4);



        makeConstructors(classname, cf, pool, classname);

        List<Find2MethodsArgs> forwarders = new ArrayList<Find2MethodsArgs>();
        int s = overrideMethods(cf, pool, classname, forwarders);
        addClassInitializer(cf, pool, classname, s, forwarders);
        addSetter(classname, cf, pool);
        if (!hasGetHandler)
            addGetter(classname, cf, pool);

        if (factoryWriteReplace) {
            try {
                cf.addMethod(makeWriteReplace(pool));
            }
            catch (DuplicateMemberException e) {

            }
        }

        thisClass = null;
        return cf;
    }

    private void checkClassAndSuperName() {
        if (interfaces == null)
            interfaces = new Class[0];

        if (superClass == null) {
            superClass = OBJECT_TYPE;
            superName = superClass.getName();
            basename = interfaces.length == 0 ? superName
                                              : interfaces[0].getName();
        } else {
            superName = superClass.getName();
            basename = superName;
        }

        if (Modifier.isFinal(superClass.getModifiers()))
            throw new RuntimeException(superName + " is final");

        if (basename.startsWith("java.") || basename.startsWith("jdk.") || onlyPublicMethods)
            basename = "org.hotswap.agent.javassist.util.proxy." + basename.replace('.', '_');
    }

    private void allocateClassName() {
        classname = makeProxyName(basename);
    }

    private static Comparator<Map.Entry<String,Method>> sorter =
        new Comparator<Map.Entry<String,Method>>() {
            @Override
            public int compare(Map.Entry<String,Method> e1, Map.Entry<String,Method> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        };

    private void makeSortedMethodList() {
        checkClassAndSuperName();

        hasGetHandler = false;
        Map<String,Method> allMethods = getMethods(superClass, interfaces);
        signatureMethods = new ArrayList<Map.Entry<String,Method>>(allMethods.entrySet());
        Collections.sort(signatureMethods, sorter);
    }

    private void computeSignature(MethodFilter filter)
    {
        makeSortedMethodList();

        int l = signatureMethods.size();
        int maxBytes = ((l + 7) >> 3);
        signature = new byte[maxBytes];
        for (int idx = 0; idx < l; idx++)
        {
            Method m = signatureMethods.get(idx).getValue();
            int mod = m.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isStatic(mod)
                    && isVisible(mod, basename, m) && (filter == null || filter.isHandled(m))) {
                setBit(signature, idx);
            }
        }
    }

    private void installSignature(byte[] signature)
    {
        makeSortedMethodList();

        int l = signatureMethods.size();
        int maxBytes = ((l + 7) >> 3);
        if (signature.length != maxBytes) {
            throw new RuntimeException("invalid filter signature length for deserialized proxy class");
        }

        this.signature =  signature;
    }

    private boolean testBit(byte[] signature, int idx) {
        int byteIdx = idx >> 3;
        if (byteIdx > signature.length)
            return false;
        int bitIdx = idx & 0x7;
        int mask = 0x1 << bitIdx;
        int sigByte = signature[byteIdx];
        return ((sigByte & mask) != 0);
    }

    private void setBit(byte[] signature, int idx) {
        int byteIdx = idx >> 3;
        if (byteIdx < signature.length) {
            int bitIdx = idx & 0x7;
            int mask = 0x1 << bitIdx;
            int sigByte = signature[byteIdx];
            signature[byteIdx] = (byte)(sigByte | mask);
        }
    }

    private static void setInterfaces(ClassFile cf, Class<?>[] interfaces, Class<?> proxyClass) {
        String setterIntf = proxyClass.getName();
        String[] list;
        if (interfaces == null || interfaces.length == 0)
            list = new String[] { setterIntf };
        else {
            list = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++)
                list[i] = interfaces[i].getName();

            list[interfaces.length] = setterIntf;
        }

        cf.setInterfaces(list);
    }

    private static void addClassInitializer(ClassFile cf, ConstPool cp,
                String classname, int size, List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        FieldInfo finfo = new FieldInfo(cp, HOLDER, HOLDER_TYPE);
        finfo.setAccessFlags(AccessFlag.PRIVATE | AccessFlag.STATIC);
        cf.addField(finfo);
        MethodInfo minfo = new MethodInfo(cp, "<clinit>", "()V");
        minfo.setAccessFlags(AccessFlag.STATIC);
        setThrows(minfo, cp, new Class<?>[] { ClassNotFoundException.class });

        Bytecode code = new Bytecode(cp, 0, 2);
        code.addIconst(size * 2);
        code.addAnewarray("java.lang.reflect.Method");
        final int varArray = 0;
        code.addAstore(varArray);



        code.addLdc(classname);
        code.addInvokestatic("java.lang.Class",
                "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        final int varClass = 1;
        code.addAstore(varClass);

        for (Find2MethodsArgs args:forwarders)
            callFind2Methods(code, args.methodName, args.delegatorName,
                             args.origIndex, args.descriptor, varClass, varArray);

        code.addAload(varArray);
        code.addPutstatic(classname, HOLDER, HOLDER_TYPE);

        code.addLconst(SERIAL_VERSION_UID_VALUE);
        code.addPutstatic(classname, SERIAL_VERSION_UID_FIELD, SERIAL_VERSION_UID_TYPE);
        code.addOpcode(Bytecode.RETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }


    private static void callFind2Methods(Bytecode code, String superMethod, String thisMethod,
                                         int index, String desc, int classVar, int arrayVar) {
        String findClass = RuntimeSupport.class.getName();
        String findDesc
            = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;[Ljava/lang/reflect/Method;)V";

        code.addAload(classVar);
        code.addLdc(superMethod);
        if (thisMethod == null)
            code.addOpcode(Opcode.ACONST_NULL);
        else
            code.addLdc(thisMethod);

        code.addIconst(index);
        code.addLdc(desc);
        code.addAload(arrayVar);
        code.addInvokestatic(findClass, "find2Methods", findDesc);
    }

    private static void addSetter(String classname, ClassFile cf, ConstPool cp)
        throws CannotCompileException
    {
        MethodInfo minfo = new MethodInfo(cp, HANDLER_SETTER,
                                          HANDLER_SETTER_TYPE);
        minfo.setAccessFlags(AccessFlag.PUBLIC);
        Bytecode code = new Bytecode(cp, 2, 2);
        code.addAload(0);
        code.addAload(1);
        code.addPutfield(classname, HANDLER, HANDLER_TYPE);
        code.addOpcode(Bytecode.RETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }

    private static void addGetter(String classname, ClassFile cf, ConstPool cp)
        throws CannotCompileException
    {
        MethodInfo minfo = new MethodInfo(cp, HANDLER_GETTER,
                                          HANDLER_GETTER_TYPE);
        minfo.setAccessFlags(AccessFlag.PUBLIC);
        Bytecode code = new Bytecode(cp, 1, 1);
        code.addAload(0);
        code.addGetfield(classname, HANDLER, HANDLER_TYPE);
        code.addOpcode(Bytecode.ARETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        cf.addMethod(minfo);
    }

    private int overrideMethods(ClassFile cf, ConstPool cp, String className, List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        String prefix = makeUniqueName("_d", signatureMethods);
        Iterator<Map.Entry<String,Method>> it = signatureMethods.iterator();
        int index = 0;
        while (it.hasNext()) {
            Map.Entry<String,Method> e = it.next();
            if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_5 || !isBridge(e.getValue()))
                if (testBit(signature, index)) {
                    override(className, e.getValue(), prefix, index,
                             keyToDesc(e.getKey(), e.getValue()), cf, cp, forwarders);
                }

            index++;
        }

        return index;
    }

    private static boolean isBridge(Method m) {
        return m.isBridge();
    }

    private void override(String thisClassname, Method meth, String prefix,
                          int index, String desc, ClassFile cf, ConstPool cp,
                          List<Find2MethodsArgs> forwarders)
        throws CannotCompileException
    {
        Class<?> declClass = meth.getDeclaringClass();
        String delegatorName = prefix + index + meth.getName();
        if (Modifier.isAbstract(meth.getModifiers()))
            delegatorName = null;
        else {
            MethodInfo delegator
                = makeDelegator(meth, desc, cp, declClass, delegatorName);

            delegator.setAccessFlags(delegator.getAccessFlags() & ~AccessFlag.BRIDGE);
            cf.addMethod(delegator);
        }

        MethodInfo forwarder
            = makeForwarder(thisClassname, meth, desc, cp, declClass,
                            delegatorName, index, forwarders);
        cf.addMethod(forwarder);
    }

    private void makeConstructors(String thisClassName, ClassFile cf,
            ConstPool cp, String classname) throws CannotCompileException
    {
        Constructor<?>[] cons = SecurityActions.getDeclaredConstructors(superClass);

        boolean doHandlerInit = !factoryUseCache;
        for (int i = 0; i < cons.length; i++) {
            Constructor<?> c = cons[i];
            int mod = c.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isPrivate(mod)
                    && isVisible(mod, basename, c)) {
                MethodInfo m = makeConstructor(thisClassName, c, cp, superClass, doHandlerInit);
                cf.addMethod(m);
            }
        }
    }

    private static String makeUniqueName(String name, List<Map.Entry<String,Method>> sortedMethods) {
        if (makeUniqueName0(name, sortedMethods.iterator()))
            return name;

        for (int i = 100; i < 999; i++) {
            String s = name + i;
            if (makeUniqueName0(s, sortedMethods.iterator()))
                return s;
        }

        throw new RuntimeException("cannot make a unique method name");
    }

    private static boolean makeUniqueName0(String name, Iterator<Map.Entry<String,Method>> it) {
        while (it.hasNext())
            if (it.next().getKey().startsWith(name))
                return false;
        return true;
    }


    private static boolean isVisible(int mod, String from, Member meth) {
        if ((mod & Modifier.PRIVATE) != 0)
            return false;
        else if ((mod & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0)
            return true;
        else {
            String p = getPackageName(from);
            String q = getPackageName(meth.getDeclaringClass().getName());
            if (p == null)
                return q == null;
            return p.equals(q);
        }
    }

    private static String getPackageName(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0)
            return null;
        return name.substring(0, i);
    }


    private Map<String,Method> getMethods(Class<?> superClass, Class<?>[] interfaceTypes) {
        Map<String,Method> hash = new HashMap<String,Method>();
        Set<Class<?>> set = new HashSet<Class<?>>();
        for (int i = 0; i < interfaceTypes.length; i++)
            getMethods(hash, interfaceTypes[i], set);

        getMethods(hash, superClass, set);
        return hash;
    }

    private void getMethods(Map<String,Method> hash, Class<?> clazz, Set<Class<?>> visitedClasses) {


        if (!visitedClasses.add(clazz))
            return;

        Class<?>[] ifs = clazz.getInterfaces();
        for (int i = 0; i < ifs.length; i++)
            getMethods(hash, ifs[i], visitedClasses);

        Class<?> parent = clazz.getSuperclass();
        if (parent != null)
            getMethods(hash, parent, visitedClasses);


        Method[] methods = SecurityActions.getDeclaredMethods(clazz);
        for (int i = 0; i < methods.length; i++)
            if (!Modifier.isPrivate(methods[i].getModifiers())) {
                Method m = methods[i];
                String key = m.getName() + ':' + RuntimeSupport.makeDescriptor(m);
                if (key.startsWith(HANDLER_GETTER_KEY))
                    hasGetHandler = true;



                Method oldMethod = hash.put(key, m);



                if (null != oldMethod && isBridge(m)
                    && !Modifier.isPublic(oldMethod.getDeclaringClass().getModifiers())
                    && !Modifier.isAbstract(oldMethod.getModifiers()) && !isDuplicated(i, methods))
                    hash.put(key, oldMethod);


                if (null != oldMethod && Modifier.isPublic(oldMethod.getModifiers())
                                      && !Modifier.isPublic(m.getModifiers())) {


                    hash.put(key, oldMethod);
                }
            }
    }

    private static boolean isDuplicated(int index, Method[] methods) {
        String name = methods[index].getName();
        for (int i = 0; i < methods.length; i++)
            if (i != index)
                if (name.equals(methods[i].getName()) && areParametersSame(methods[index], methods[i]))
                    return true;

        return false;
    }

    private static boolean areParametersSame(Method method, Method targetMethod) {
        Class<?>[] methodTypes = method.getParameterTypes();
        Class<?>[] targetMethodTypes = targetMethod.getParameterTypes();
        if (methodTypes.length == targetMethodTypes.length) {
            for (int i = 0; i< methodTypes.length; i++) {
                if (methodTypes[i].getName().equals(targetMethodTypes[i].getName())) {
                    continue;
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static final String HANDLER_GETTER_KEY
        = HANDLER_GETTER + ":()";

    private static String keyToDesc(String key, Method m) {
        return key.substring(key.indexOf(':') + 1);
    }

    private static MethodInfo makeConstructor(String thisClassName, Constructor<?> cons,
                                              ConstPool cp, Class<?> superClass, boolean doHandlerInit) {
        String desc = RuntimeSupport.makeDescriptor(cons.getParameterTypes(),
                                                    Void.TYPE);
        MethodInfo minfo = new MethodInfo(cp, "<init>", desc);
        minfo.setAccessFlags(Modifier.PUBLIC);
        setThrows(minfo, cp, cons.getExceptionTypes());
        Bytecode code = new Bytecode(cp, 0, 0);




        if (doHandlerInit) {
            code.addAload(0);
            code.addGetstatic(thisClassName, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            code.addPutfield(thisClassName, HANDLER, HANDLER_TYPE);
            code.addGetstatic(thisClassName, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
            code.addOpcode(Opcode.IFNONNULL);
            code.addIndex(10);
        }


        code.addAload(0);
        code.addGetstatic(NULL_INTERCEPTOR_HOLDER, DEFAULT_INTERCEPTOR, HANDLER_TYPE);
        code.addPutfield(thisClassName, HANDLER, HANDLER_TYPE);
        int pc = code.currentPc();

        code.addAload(0);
        int s = addLoadParameters(code, cons.getParameterTypes(), 1);
        code.addInvokespecial(superClass.getName(), "<init>", desc);
        code.addOpcode(Opcode.RETURN);
        code.setMaxLocals(s + 1);
        CodeAttribute ca = code.toCodeAttribute();
        minfo.setCodeAttribute(ca);

        StackMapTable.Writer writer = new StackMapTable.Writer(32);
        writer.sameFrame(pc);
        ca.setAttribute(writer.toStackMapTable(cp));
        return minfo;
    }

    private MethodInfo makeDelegator(Method meth, String desc,
                ConstPool cp, Class<?> declClass, String delegatorName) {
        MethodInfo delegator = new MethodInfo(cp, delegatorName, desc);
        delegator.setAccessFlags(Modifier.FINAL | Modifier.PUBLIC
                | (meth.getModifiers() & ~(Modifier.PRIVATE
                                           | Modifier.PROTECTED
                                           | Modifier.ABSTRACT
                                           | Modifier.NATIVE
                                           | Modifier.SYNCHRONIZED)));
        setThrows(delegator, cp, meth);
        Bytecode code = new Bytecode(cp, 0, 0);
        code.addAload(0);
        int s = addLoadParameters(code, meth.getParameterTypes(), 1);
        Class<?> targetClass = invokespecialTarget(declClass);
        code.addInvokespecial(targetClass.isInterface(), cp.addClassInfo(targetClass.getName()),
                              meth.getName(), desc);
        addReturn(code, meth.getReturnType());
        code.setMaxLocals(++s);
        delegator.setCodeAttribute(code.toCodeAttribute());
        return delegator;
    }


    private Class<?> invokespecialTarget(Class<?> declClass) {
        if (declClass.isInterface())
            for (Class<?> i: interfaces)
                if (declClass.isAssignableFrom(i))
                    return i;

        return superClass;
    }


    private static MethodInfo makeForwarder(String thisClassName,
                    Method meth, String desc, ConstPool cp,
                    Class<?> declClass, String delegatorName, int index,
                    List<Find2MethodsArgs> forwarders) {
        MethodInfo forwarder = new MethodInfo(cp, meth.getName(), desc);
        forwarder.setAccessFlags(Modifier.FINAL
                    | (meth.getModifiers() & ~(Modifier.ABSTRACT
                                               | Modifier.NATIVE
                                               | Modifier.SYNCHRONIZED)));
        setThrows(forwarder, cp, meth);
        int args = Descriptor.paramSize(desc);
        Bytecode code = new Bytecode(cp, 0, args + 2);

        int origIndex = index * 2;
        int delIndex = index * 2 + 1;
        int arrayVar = args + 1;
        code.addGetstatic(thisClassName, HOLDER, HOLDER_TYPE);
        code.addAstore(arrayVar);

        forwarders.add(new Find2MethodsArgs(meth.getName(), delegatorName, desc, origIndex));

        code.addAload(0);
        code.addGetfield(thisClassName, HANDLER, HANDLER_TYPE);
        code.addAload(0);

        code.addAload(arrayVar);
        code.addIconst(origIndex);
        code.addOpcode(Opcode.AALOAD);

        code.addAload(arrayVar);
        code.addIconst(delIndex);
        code.addOpcode(Opcode.AALOAD);

        makeParameterList(code, meth.getParameterTypes());
        code.addInvokeinterface(MethodHandler.class.getName(), "invoke",
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
            5);
        Class<?> retType = meth.getReturnType();
        addUnwrapper(code, retType);
        addReturn(code, retType);

        CodeAttribute ca = code.toCodeAttribute();
        forwarder.setCodeAttribute(ca);
        return forwarder;
    }

    static class Find2MethodsArgs {
        String methodName, delegatorName, descriptor;
        int origIndex;

        Find2MethodsArgs(String mname, String dname, String desc, int index) {
            methodName = mname;
            delegatorName = dname;
            descriptor = desc;
            origIndex = index;
        }
    }

    private static void setThrows(MethodInfo minfo, ConstPool cp, Method orig) {
        Class<?>[] exceptions = orig.getExceptionTypes();
        setThrows(minfo, cp, exceptions);
    }

    private static void setThrows(MethodInfo minfo, ConstPool cp,
                                  Class<?>[] exceptions) {
        if (exceptions.length == 0)
            return;

        String[] list = new String[exceptions.length];
        for (int i = 0; i < exceptions.length; i++)
            list[i] = exceptions[i].getName();

        ExceptionsAttribute ea = new ExceptionsAttribute(cp);
        ea.setExceptions(list);
        minfo.setExceptionsAttribute(ea);
    }

    private static int addLoadParameters(Bytecode code, Class<?>[] params,
                                         int offset) {
        int stacksize = 0;
        int n = params.length;
        for (int i = 0; i < n; ++i)
            stacksize += addLoad(code, stacksize + offset, params[i]);

        return stacksize;
    }

    private static int addLoad(Bytecode code, int n, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Long.TYPE) {
                code.addLload(n);
                return 2;
            }
            else if (type == Float.TYPE)
                code.addFload(n);
            else if (type == Double.TYPE) {
                code.addDload(n);
                return 2;
            }
            else
                code.addIload(n);
        }
        else
            code.addAload(n);

        return 1;
    }

    private static int addReturn(Bytecode code, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Long.TYPE) {
                code.addOpcode(Opcode.LRETURN);
                return 2;
            }
            else if (type == Float.TYPE)
                code.addOpcode(Opcode.FRETURN);
            else if (type == Double.TYPE) {
                code.addOpcode(Opcode.DRETURN);
                return 2;
            }
            else if (type == Void.TYPE) {
                code.addOpcode(Opcode.RETURN);
                return 0;
            }
            else
                code.addOpcode(Opcode.IRETURN);
        }
        else
            code.addOpcode(Opcode.ARETURN);

        return 1;
    }

    private static void makeParameterList(Bytecode code, Class<?>[] params) {
        int regno = 1;
        int n = params.length;
        code.addIconst(n);
        code.addAnewarray("java/lang/Object");
        for (int i = 0; i < n; i++) {
            code.addOpcode(Opcode.DUP);
            code.addIconst(i);
            Class<?> type = params[i];
            if (type.isPrimitive())
                regno = makeWrapper(code, type, regno);
            else {
                code.addAload(regno);
                regno++;
            }

            code.addOpcode(Opcode.AASTORE);
        }
    }

    private static int makeWrapper(Bytecode code, Class<?> type, int regno) {
        int index = FactoryHelper.typeIndex(type);
        String wrapper = FactoryHelper.wrapperTypes[index];
        code.addNew(wrapper);
        code.addOpcode(Opcode.DUP);
        addLoad(code, regno, type);
        code.addInvokespecial(wrapper, "<init>",
                              FactoryHelper.wrapperDesc[index]);
        return regno + FactoryHelper.dataSize[index];
    }

    private static void addUnwrapper(Bytecode code, Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Void.TYPE)
                code.addOpcode(Opcode.POP);
            else {
                int index = FactoryHelper.typeIndex(type);
                String wrapper = FactoryHelper.wrapperTypes[index];
                code.addCheckcast(wrapper);
                code.addInvokevirtual(wrapper,
                                      FactoryHelper.unwarpMethods[index],
                                      FactoryHelper.unwrapDesc[index]);
            }
        }
        else
            code.addCheckcast(type.getName());
    }

    private static MethodInfo makeWriteReplace(ConstPool cp) {
        MethodInfo minfo = new MethodInfo(cp, "writeReplace", "()Ljava/lang/Object;");
        String[] list = new String[1];
        list[0] = "java.io.ObjectStreamException";
        ExceptionsAttribute ea = new ExceptionsAttribute(cp);
        ea.setExceptions(list);
        minfo.setExceptionsAttribute(ea);
        Bytecode code = new Bytecode(cp, 0, 1);
        code.addAload(0);
        code.addInvokestatic("org.hotswap.agent.javassist.util.proxy.RuntimeSupport",
                             "makeSerializedProxy",
                             "(Ljava/lang/Object;)Lorg/hotswap/agent/javassist/util/proxy/SerializedProxy;");
        code.addOpcode(Opcode.ARETURN);
        minfo.setCodeAttribute(code.toCodeAttribute());
        return minfo;
    }
}
