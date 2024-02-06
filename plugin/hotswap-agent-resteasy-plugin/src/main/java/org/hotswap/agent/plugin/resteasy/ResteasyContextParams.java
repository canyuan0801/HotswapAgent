

package org.hotswap.agent.plugin.resteasy;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;


public class ResteasyContextParams {

    public static Set<String> init(final ServletContext context, final Set<String> existing) {
        if(existing != null) {
            return existing;
        }
        final Set<String> ret = new HashSet<String>();
        Enumeration names = context.getAttributeNames();
        while (names.hasMoreElements()) {
            ret.add((String) names.nextElement());
        }
        return ret;
    }
}
