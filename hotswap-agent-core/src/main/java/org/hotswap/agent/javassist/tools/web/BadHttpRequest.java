

package org.hotswap.agent.javassist.tools.web;


public class BadHttpRequest extends Exception {

    private static final long serialVersionUID = 1L;
    private Exception e;

    public BadHttpRequest() { e = null; }

    public BadHttpRequest(Exception _e) { e = _e; }

    @Override
    public String toString() {
        if (e == null)
            return super.toString();
        return e.toString();
    }
}
