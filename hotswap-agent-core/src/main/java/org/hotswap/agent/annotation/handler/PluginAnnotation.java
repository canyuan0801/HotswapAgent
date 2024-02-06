
package org.hotswap.agent.annotation.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.versions.DeploymentInfo;
import org.hotswap.agent.versions.VersionMatchResult;
import org.hotswap.agent.versions.matcher.MethodMatcher;
import org.hotswap.agent.versions.matcher.PluginMatcher;


public class PluginAnnotation<T extends Annotation> {
    private static AgentLogger LOGGER = AgentLogger.getLogger(PluginAnnotation.class);


    Class<?> pluginClass;


    Object plugin;


    T annotation;


    Field field;


    Method method;


    final PluginMatcher pluginMatcher;


    final MethodMatcher methodMatcher;


    final String group;


    final boolean fallback;

    public PluginAnnotation(Class<?> pluginClass, Object plugin, T annotation, Method method) {
        this.pluginClass = pluginClass;
        this.plugin = plugin;
        this.annotation = annotation;
        this.method = method;

        Plugin pluginAnnotation = pluginClass.getAnnotation(Plugin.class);
        this.group = (pluginAnnotation.group() != null && !pluginAnnotation.group().isEmpty()) ? pluginAnnotation.group() : null;
        this.fallback = pluginAnnotation.fallback();

        if(method != null && (Modifier.isStatic(method.getModifiers()))) {
            this.pluginMatcher = new PluginMatcher(pluginClass);
            this.methodMatcher= new MethodMatcher(method);
        } else {
            this.pluginMatcher = null;
            this.methodMatcher = null;
        }
    }

    public PluginAnnotation(Class<?> pluginClass, Object plugin, T annotation, Field field) {

        this.pluginClass = pluginClass;
        this.plugin = plugin;
        this.annotation = annotation;
        this.field = field;
        this.pluginMatcher = null;
        this.methodMatcher = null;
        this.fallback  = false;
        this.group = null;
    }


    public Class<?> getPluginClass() {
        return pluginClass;
    }

    public Object getPlugin() {
        return plugin;
    }

    public T getAnnotation() {
        return annotation;
    }

    public Method getMethod() {
        return method;
    }

    public Field getField() {
        return field;
    }

    public boolean shouldCheckVersion() {
        return
        (this.plugin == null)
                &&
                (
                        (pluginMatcher != null && pluginMatcher.isApply())
                        ||
                        (methodMatcher != null && methodMatcher.isApply())
                );
    }


    public boolean isFallBack() {
       return fallback;
    }


    public String getGroup() {
        return group;
    }


    public boolean matches(DeploymentInfo deploymentInfo){
        if(deploymentInfo == null || (pluginMatcher == null && methodMatcher == null)) {
            LOGGER.debug("No matchers, apply");
            return true;
        }

        if(pluginMatcher != null && pluginMatcher.isApply()) {
            if(VersionMatchResult.REJECTED.equals(pluginMatcher.matches(deploymentInfo))){
                LOGGER.debug("Plugin matcher rejected");
                return false;
            }
        }
        if(methodMatcher != null && methodMatcher.isApply()) {
            if(VersionMatchResult.REJECTED.equals(methodMatcher.matches(deploymentInfo))){
                LOGGER.debug("Method matcher rejected");
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginAnnotation<?> that = (PluginAnnotation<?>) o;

        if (!annotation.equals(that.annotation)) return false;
        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (!plugin.equals(that.plugin)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = plugin.hashCode();
        result = 31 * result + annotation.hashCode();
        result = 31 * result + (field != null ? field.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluginAnnotation{" +
                "plugin=" + plugin +
                ", annotation=" + annotation +
                ", field=" + field +
                ", method=" + method +
                '}';
    }
}
