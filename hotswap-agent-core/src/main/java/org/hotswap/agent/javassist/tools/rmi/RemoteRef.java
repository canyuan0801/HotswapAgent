

package org.hotswap.agent.javassist.tools.rmi;


public class RemoteRef implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    public int oid;
    public String classname;

    public RemoteRef(int i) {
        oid = i;
        classname = null;
    }

    public RemoteRef(int i, String name) {
        oid = i;
        classname = name;
    }
}
