
package org.hotswap.agent.plugin.weld.beans;


import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;



public interface WeldHotswapContext {


    void destroy(Contextual<?> contextual);


    boolean isActive();


    public <T> T get(Contextual<T> contextual);


    void $$ha$addBeanToReloadWeld(Contextual<Object> bean);


    Set<Contextual<Object>> $$ha$getBeansToReloadWeld();


    void $$ha$reloadWeld();


    boolean $$ha$isActiveWeld();
}
