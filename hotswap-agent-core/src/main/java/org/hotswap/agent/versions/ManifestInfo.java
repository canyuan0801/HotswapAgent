
package org.hotswap.agent.versions;

import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.hotswap.agent.util.spring.util.StringUtils;



public class ManifestInfo {

    
    private final Manifest mf;

    
    private final Attributes attr;

    
    private final Attributes main;

    private final Map<String, Attributes> entries;

    
    public ManifestInfo(Manifest mf) {
        this.mf = mf;
        if (mf != null) {
            attr = mf.getAttributes("");
            main = mf.getMainAttributes();
            entries = mf.getEntries();
        } else {
            attr = null;
            main = null;
            entries = null;
        }
    }

    
    public boolean isEmpty() {
        return mf == null || ((attr == null || attr.size() == 0) && (main == null || main.size() == 0));
    }

    
    public String getValue(Name... name) {
        if (name == null || isEmpty()) {
            return null;
        }
        return getAttribute(attr, main, entries, name);
    }

    
    public String getValue(String path, Name... name) {
        if (name == null || isEmpty()) {
            return null;
        }
        return getAttribute(StringUtils.isEmpty(path) ? attr : mf.getAttributes(path), main, entries, name);
    }

    
    private static String getAttribute(Attributes attr, Attributes main, Map<String, Attributes> entries, Name... names) {
        if (names == null || names.length == 0) {
            return null;
        }

        String ret = getAttributeByName(main, names);

        if (ret != null) {
            return ret;
        }

        ret = getAttributeByName(attr, names);

        if (ret != null) {
            return ret;
        }

        if (entries != null) {
            for (Iterator<Map.Entry<String, Attributes>> it = entries.entrySet().iterator();it.hasNext();) {
                Map.Entry<String, Attributes> entry = it.next();
                ret = getAttributeByName(entry.getValue(), names);
                if (ret != null) {
                    return ret;
                }

            }
        }

        return null;
    }

    private static String getAttributeByName(Attributes attr, Name... names) {
        if (attr != null) {
            String value;
            for (Name name : names) {
                value = attr.getValue(name);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ManifestInfo other = (ManifestInfo) obj;
        if (mf == null) {
            if (other.mf != null) {
                return false;
            }
        } else if (!mf.equals(other.mf)) {
            return false;
        }
        return true;
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mf == null) ? 0 : mf.hashCode());
        return result;
    }

    
    @Override
    public String toString() {
        if (mf != null) {
            return "ManifestInfo [" + ManifestMiniDumper.dump(mf.getMainAttributes()) + ", entries:" + mf.getEntries() + "]";
        } else {
            return "ManifestInfo [null]";
        }
    }

    
    
    
    
    
    
    
    
    
    
}
