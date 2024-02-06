
package org.hotswap.agent.util;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import org.hotswap.agent.annotation.handler.PluginClassFileTransformer;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;


public class HotswapTransformer implements ClassFileTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(HotswapTransformer.class);


    private static final Set<String> skippedClassLoaders = new HashSet<>(Arrays.asList(
            "jdk.internal.reflect.DelegatingClassLoader",
            "sun.reflect.DelegatingClassLoader"
    ));


    private static final Set<String> excludedClassLoaders = new HashSet<>(Arrays.asList(
            "org.apache.felix.framework.BundleWiringImpl$BundleClassLoader",
            "org.apache.felix.framework.BundleWiringImpl$BundleClassLoaderJava5"
    ));

    private static class RegisteredTransformersRecord {
        Pattern pattern;
        List<HaClassFileTransformer> transformerList = new LinkedList<>();
    }

    protected Map<String, RegisteredTransformersRecord> redefinitionTransformers = new LinkedHashMap<>();
    protected Map<String, RegisteredTransformersRecord> otherTransformers = new LinkedHashMap<>();


    protected Map<ClassFileTransformer, ClassLoader> classLoaderTransformers = new LinkedHashMap<>();

    protected Map<ClassLoader, Object> seenClassLoaders = new WeakHashMap<>();
    private List<Pattern> includedClassLoaderPatterns;
    private List<Pattern> excludedClassLoaderPatterns;
    public List<Pattern> getIncludedClassLoaderPatterns() {
        return includedClassLoaderPatterns;
    }

    public void setIncludedClassLoaderPatterns(List<Pattern> includedClassLoaderPatterns) {
        this.includedClassLoaderPatterns = includedClassLoaderPatterns;
    }



    public void setExcludedClassLoaderPatterns(List<Pattern> excludedClassLoaderPatterns) {
        this.excludedClassLoaderPatterns = excludedClassLoaderPatterns;
    }

    public List<Pattern> getExcludedClassLoaderPatterns() {
        return excludedClassLoaderPatterns;
    }


    public void registerTransformer(ClassLoader classLoader, String classNameRegexp, HaClassFileTransformer transformer) {
        LOGGER.debug("Registering transformer for class regexp '{}'.", classNameRegexp);

        String normalizeRegexp = normalizeTypeRegexp(classNameRegexp);

        Map<String, RegisteredTransformersRecord> transformersMap = getTransformerMap(transformer);

        RegisteredTransformersRecord transformerRecord = transformersMap.get(normalizeRegexp);
        if (transformerRecord == null) {
            transformerRecord = new RegisteredTransformersRecord();
            transformerRecord.pattern = Pattern.compile(normalizeRegexp);
            transformersMap.put(normalizeRegexp, transformerRecord);
        }

        if (!transformerRecord.transformerList.contains(transformer)) {
            transformerRecord.transformerList.add(transformer);
        }


        if (classLoader != null) {
            classLoaderTransformers.put(transformer, classLoader);
        }
    }

    private Map<String, RegisteredTransformersRecord> getTransformerMap(HaClassFileTransformer transformer) {
        if (transformer.isForRedefinitionOnly()) {
            return redefinitionTransformers;
        }
        return otherTransformers;
    }


    public void removeTransformer(String classNameRegexp, HaClassFileTransformer transformer) {
        String normalizeRegexp = normalizeTypeRegexp(classNameRegexp);
        Map<String, RegisteredTransformersRecord> transformersMap = getTransformerMap(transformer);
        RegisteredTransformersRecord transformerRecord = transformersMap.get(normalizeRegexp);
        if (transformerRecord != null) {
            transformerRecord.transformerList.remove(transformer);
        }
    }


    public void closeClassLoader(ClassLoader classLoader) {
        for (Iterator<Map.Entry<ClassFileTransformer, ClassLoader>> entryIterator = classLoaderTransformers.entrySet().iterator();
                entryIterator.hasNext(); ) {
            Map.Entry<ClassFileTransformer, ClassLoader> entry = entryIterator.next();
            if (entry.getValue().equals(classLoader)) {
                entryIterator.remove();
                for (RegisteredTransformersRecord transformerRecord : redefinitionTransformers.values()) {
                    transformerRecord.transformerList.remove(entry.getKey());
                }
                for (RegisteredTransformersRecord transformerRecord : otherTransformers.values()) {
                    transformerRecord.transformerList.remove(entry.getKey());
                }
            }
        }

        LOGGER.debug("All transformers removed for classLoader {}", classLoader);
    }


    @Override
    public byte[] transform(final ClassLoader classLoader, String className, Class<?> redefiningClass,
                            final ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {


        String classLoaderClassName = classLoader != null ? classLoader.getClass().getName() : null;
        if (skippedClassLoaders.contains(classLoaderClassName)) {
            return bytes;
        }

        LOGGER.trace("Transform on class '{}' @{} redefiningClass '{}'.", className, classLoader, redefiningClass);

        List<ClassFileTransformer> toApply = new ArrayList<>();
        List<PluginClassFileTransformer> pluginTransformers = new ArrayList<>();
        try {

            for (RegisteredTransformersRecord transformerRecord : new ArrayList<RegisteredTransformersRecord>(otherTransformers.values())) {
                if ((className != null && transformerRecord.pattern.matcher(className).matches()) ||
                        (redefiningClass != null && transformerRecord.pattern.matcher(redefiningClass.getName()).matches())) {
                    for (ClassFileTransformer transformer : new ArrayList<ClassFileTransformer>(transformerRecord.transformerList)) {
                        if(transformer instanceof PluginClassFileTransformer) {
                            PluginClassFileTransformer pcft = PluginClassFileTransformer.class.cast(transformer);
                            if(!pcft.isPluginDisabled(classLoader)) {
                                pluginTransformers.add(pcft);
                            }
                        } else {
                            toApply.add(transformer);
                        }
                    }
                }
            }

            if (redefiningClass != null && className != null) {
                for (RegisteredTransformersRecord transformerRecord : new ArrayList<RegisteredTransformersRecord>(redefinitionTransformers.values())) {
                    if (transformerRecord.pattern.matcher(className).matches()) {
                        for (ClassFileTransformer transformer : new ArrayList<ClassFileTransformer>(transformerRecord.transformerList)) {
                            if(transformer instanceof PluginClassFileTransformer) {
                                PluginClassFileTransformer pcft = PluginClassFileTransformer.class.cast(transformer);
                                if(!pcft.isPluginDisabled(classLoader)) {
                                    pluginTransformers.add(pcft);
                                }
                            } else {
                                toApply.add(transformer);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error transforming class '" + className + "'.", t);
        }

        if(!pluginTransformers.isEmpty()) {
            pluginTransformers =  reduce(classLoader, pluginTransformers, className);
        }


       ensureClassLoaderInitialized(classLoader, protectionDomain);

        if(toApply.isEmpty() && pluginTransformers.isEmpty()) {
            LOGGER.trace("No transformers defing for {} ", className);
            return bytes;
        }

       try {
           byte[] result = bytes;

           for(ClassFileTransformer transformer: pluginTransformers) {
               LOGGER.trace("Transforming class '" + className + "' with transformer '" + transformer + "' " + "@ClassLoader" + classLoader + ".");
               result = transformer.transform(classLoader, className, redefiningClass, protectionDomain, result);
           }

           for(ClassFileTransformer transformer: toApply) {
               LOGGER.trace("Transforming class '" + className + "' with transformer '" + transformer + "' " + "@ClassLoader" + classLoader + ".");
               result = transformer.transform(classLoader, className, redefiningClass, protectionDomain, result);
           }
           return result;
       } catch (Throwable t) {
           LOGGER.error("Error transforming class '" + className + "'.", t);
       }
       return bytes;
    }

    LinkedList<PluginClassFileTransformer> reduce(final ClassLoader classLoader, List<PluginClassFileTransformer> pluginCalls, String className) {
        LinkedList<PluginClassFileTransformer> reduced = new LinkedList<>();

        Map<String, PluginClassFileTransformer> fallbackMap = new HashMap<>();

        for (PluginClassFileTransformer pcft : pluginCalls) {
            try {
                String pluginGroup = pcft.getPluginGroup();
                if(pcft.versionMatches(classLoader)){
                    if (pluginGroup != null) {
                        fallbackMap.put(pluginGroup, null);
                    }
                    reduced.add(pcft);
                } else if(pcft.isFallbackPlugin()){
                    if (pluginGroup != null && !fallbackMap.containsKey(pluginGroup)) {
                        fallbackMap.put(pluginGroup, pcft);
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Error evaluating aplicability of plugin", e);
            }
        }

        for (PluginClassFileTransformer pcft: fallbackMap.values()) {
            if (pcft != null) {
                reduced.add(pcft);
            }
        }

        return reduced;
    }

    protected void ensureClassLoaderInitialized(final ClassLoader classLoader, final ProtectionDomain protectionDomain) {
        if (!seenClassLoaders.containsKey(classLoader)) {
            seenClassLoaders.put(classLoader, null);

            if (classLoader == null) {

                PluginManager.getInstance().initClassLoader(null, protectionDomain);
            } else {

                if (shouldScheduleClassLoader(classLoader)) {
                    PluginManager.getInstance().initClassLoader(classLoader, protectionDomain);
                }
            }
        }
    }

    private boolean shouldScheduleClassLoader(final ClassLoader classLoader) {
        String name = classLoader.getClass().getName();
        if (excludedClassLoaders.contains(name)) {
            return false;
        }

        if (includedClassLoaderPatterns != null) {
            for (Pattern pattern : includedClassLoaderPatterns) {
                if (pattern.matcher(name).matches()) {
                    return true;
                }
            }
            return false;
        }

        if (excludedClassLoaderPatterns != null) {
            for (Pattern pattern : excludedClassLoaderPatterns) {
                if (pattern.matcher(name).matches()) {
                    return false;
                }
            }
        }
        return true;
    }



    protected String normalizeTypeRegexp(String registeredType) {
        String regexp = registeredType;
        if (!registeredType.startsWith("^")){
            regexp = "^" + regexp;
        }
        if (!registeredType.endsWith("$")){
            regexp = regexp + "$";
        }

        return regexp;
    }

}
