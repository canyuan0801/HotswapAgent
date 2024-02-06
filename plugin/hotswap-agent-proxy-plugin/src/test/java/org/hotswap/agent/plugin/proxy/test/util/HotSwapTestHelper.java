
package org.hotswap.agent.plugin.proxy.test.util;

import org.hotswap.agent.plugin.proxy.HotSwapTool;
import org.hotswap.agent.plugin.proxy.api.MultistepProxyTransformer;
import org.hotswap.agent.plugin.proxy.ProxyPlugin;
import org.hotswap.agent.plugin.proxy.hscglib.CglibProxyTransformer;
import org.hotswap.agent.util.test.WaitHelper;


public class HotSwapTestHelper {

    public static int __version__() {
        return HotSwapTool.getCurrentVersion(determineOuter(0));
    }


    public static void __toVersion__Delayed(int versionNumber, Class<?>... extra) {
        MultistepProxyTransformer.addThirdStep = true;
        HotSwapTool.toVersion(determineOuter(0), versionNumber, extra);

        WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return !CglibProxyTransformer.isReloadingInProgress();
            }
        });
        MultistepProxyTransformer.addThirdStep = false;
    }

    public static boolean __toVersion__Delayed_JavaProxy(int versionNumber, Class<?>... extra) {
        ProxyPlugin.reloadFlag = true;
        HotSwapTool.toVersion(determineOuter(0), versionNumber, extra);

        WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return !ProxyPlugin.reloadFlag;
            }
        });
        boolean result = ProxyPlugin.reloadFlag;
        ProxyPlugin.reloadFlag = false;
        return result;
    }


    public static void __toVersion__(int versionNumber, Class<?>... extra) {
        HotSwapTool.toVersion(determineOuter(0), versionNumber, extra);
    }


    private static Class<?> determineOuter(int level) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();



        String callerName = stack[level + 3].getClassName();
        try {
            Class<?> clazz = cl.loadClass(callerName);
            while (clazz.getEnclosingClass() != null) {
                clazz = clazz.getEnclosingClass();
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find caller class: " + callerName, e);
        }
    }
}
