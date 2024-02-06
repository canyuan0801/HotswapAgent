
package org.hotswap.agent.plugin.jvm;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.FieldAccess;
import org.hotswap.agent.logging.AgentLogger;


@Plugin(name = "ClassInitPlugin",
        description = "Initialize empty static fields (left by DCEVM) using code from <clinit> method.",
        testedVersions = {"DCEVM"})
public class ClassInitPlugin {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassInitPlugin.class);

    private static final String HOTSWAP_AGENT_CLINIT_METHOD = "$$ha$clinit";

    public static boolean reloadFlag;

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public static void patch(final CtClass ctClass, final ClassLoader classLoader, final Class<?> originalClass) throws IOException, CannotCompileException, NotFoundException {

        if (isSyntheticClass(originalClass)) {
            return;
        }

        final String className = ctClass.getName();

        try {
            CtMethod origMethod = ctClass.getDeclaredMethod(HOTSWAP_AGENT_CLINIT_METHOD);
            ctClass.removeMethod(origMethod);
        } catch (org.hotswap.agent.javassist.NotFoundException ex) {

        }

        CtConstructor clinit = ctClass.getClassInitializer();

        if (clinit != null) {
            LOGGER.debug("Adding " + HOTSWAP_AGENT_CLINIT_METHOD + " to class: {}", className);
            CtConstructor haClinit = new CtConstructor(clinit, ctClass, null);
            haClinit.getMethodInfo().setName(HOTSWAP_AGENT_CLINIT_METHOD);
            haClinit.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            ctClass.addConstructor(haClinit);

            final boolean reinitializeStatics[] = new boolean[] { false };

            haClinit.instrument(
                new ExprEditor() {
                    public void edit(FieldAccess f) throws CannotCompileException {
                        try {
                            if (f.isStatic() && f.isWriter()) {
                                Field originalField = null;
                                try {
                                    originalField = originalClass.getDeclaredField(f.getFieldName());
                                } catch (NoSuchFieldException e) {
                                    LOGGER.debug("New field will be initialized {}", f.getFieldName());
                                    reinitializeStatics[0] = true;
                                }
                                if (originalField != null) {


                                    if (originalClass.isEnum() && f.getSignature().startsWith("[L")
                                            && (f.getFieldName().startsWith("$VALUES")
                                                || f.getFieldName().startsWith("ENUM$VALUES"))) {
                                        if (reinitializeStatics[0]) {
                                            LOGGER.debug("New field will be initialized {}", f.getFieldName());
                                        } else {
                                            reinitializeStatics[0] = checkOldEnumValues(ctClass, originalClass);
                                        }
                                    } else {
                                        LOGGER.debug("Skipping old field {}", f.getFieldName());
                                        f.replace("{}");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Patching " + HOTSWAP_AGENT_CLINIT_METHOD + " method failed.", e);
                        }
                    }

                }
            );

            if (reinitializeStatics[0]) {
                PluginManager.getInstance().getScheduler().scheduleCommand(new Command() {
                    @Override
                    public void executeCommand() {
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            Method m = clazz.getDeclaredMethod(HOTSWAP_AGENT_CLINIT_METHOD, new Class[] {});
                            if (m != null) {
                                m.setAccessible(true);
                                m.invoke(null, new Object[] {});
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error initializing redefined class {}", e, className);
                        } finally {
                            reloadFlag = false;
                        }
                    }
                }, 150);


            } else {
                reloadFlag = false;
            }
        }
    }

    private static boolean checkOldEnumValues(CtClass ctClass, Class<?> originalClass) {
        if (ctClass.isEnum()) {

            Enum<?>[] enumConstants = (Enum<?>[]) originalClass.getEnumConstants();
            for (Enum<?> en : enumConstants) {
                try {
                    CtField existing = ctClass.getDeclaredField(en.toString());
                } catch (NotFoundException e) {
                    LOGGER.debug("Enum field deleted. $VALUES will be reinitialized {}", en.toString());
                    return true;
                }
            }
        } else {
            LOGGER.error("Patching " + HOTSWAP_AGENT_CLINIT_METHOD + " method failed. Enum type expected {}", ctClass.getName());
        }
        return false;
    }

    private static boolean isSyntheticClass(Class<?> classBeingRedefined) {
        return classBeingRedefined.getSimpleName().contains("$$_javassist")
                || classBeingRedefined.getSimpleName().contains("$$_jvst")
                || classBeingRedefined.getName().startsWith("com.sun.proxy.$Proxy")
                || classBeingRedefined.getSimpleName().contains("$$");
    }

}
