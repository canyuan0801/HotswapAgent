
package org.hotswap.agent.versions;

import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;



public class ManifestMiniDumper {

    public static final Name EXTENSION_LIST = new Name("Extension-List");


    public static final Name EXTENSION_NAME = new Name("Extension-Name");


    public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");


    public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");


    public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");


    public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");


    public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");


    public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");


    public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");



    public static final Name BUNDLE_SYMBOLIC_NAME = new Name("Bundle-SymbolicName");



    public static final Name BUNDLE_NAME = new Name("Bundle-Name");



    public static final Name BUNDLE_VERSION = new Name("Bundle-Version");


    public static final Name[] VERSIONS = new Name[] { BUNDLE_VERSION, IMPLEMENTATION_VERSION, SPECIFICATION_VENDOR };


    public static final Name[] PACKAGE = new Name[] { BUNDLE_SYMBOLIC_NAME, IMPLEMENTATION_VENDOR_ID, SPECIFICATION_VENDOR };


    public static final Name[] TITLE = new Name[] { BUNDLE_NAME, IMPLEMENTATION_TITLE, SPECIFICATION_VENDOR };


    public static String dump(Attributes attr) {
        String version = getAttribute(attr, null, VERSIONS);
        String pack = getAttribute(attr, null, PACKAGE);
        String title = getAttribute(attr, null, TITLE);

        return "version=" + version + ", package=" + pack + ", title=" + title;
    }


    private static String getAttribute(Attributes main, Attributes attr, Name... names) {
        if (names == null) {
            return null;
        }

        if (main != null) {
            String value;
            for (Name name : names) {
                value = main.getValue(name);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
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
}
