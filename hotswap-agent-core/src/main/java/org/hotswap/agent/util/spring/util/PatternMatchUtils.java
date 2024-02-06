

package org.hotswap.agent.util.spring.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public abstract class PatternMatchUtils {

    private final static Map<String, Pattern> patterns = new HashMap<>();

    public static boolean regexMatch(String pattern, String str) {
        if (StringUtils.isEmpty(pattern)) {
            return true;
        }

        Pattern p = patterns.get(pattern);
        if (p == null) {
            p = Pattern.compile(pattern);
            patterns.put(pattern, p);
        }
        boolean matched = p.matcher(str).matches();
        return matched;
    }


    public static boolean simpleMatch(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }
        int firstIndex = pattern.indexOf('*');
        if (firstIndex == -1) {
            return pattern.equals(str);
        }
        if (firstIndex == 0) {
            if (pattern.length() == 1) {
                return true;
            }
            int nextIndex = pattern.indexOf('*', firstIndex + 1);
            if (nextIndex == -1) {
                return str.endsWith(pattern.substring(1));
            }
            String part = pattern.substring(1, nextIndex);
            if ("".equals(part)) {
                return simpleMatch(pattern.substring(nextIndex), str);
            }
            int partIndex = str.indexOf(part);
            while (partIndex != -1) {
                if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
                    return true;
                }
                partIndex = str.indexOf(part, partIndex + 1);
            }
            return false;
        }
        return (str.length() >= firstIndex && pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) && simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
    }


    public static boolean simpleMatch(String[] patterns, String str) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (simpleMatch(pattern, str)) {
                    return true;
                }
            }
        }
        return false;
    }

}