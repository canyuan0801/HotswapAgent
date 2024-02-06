
package org.hotswap.agent.plugin.weld_jakarta.beans;


import java.util.Set;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Contextual;



public interface WeldHotswapContext {


    void destroy(Contextual<?> contextual);


    boolean isActive();


    public <T> T get(Contextual<T> contextual);


    void $$ha$addBeanToReloadWeld(Contextual<Object> bean);


    Set<Contextual<Object>> $$ha$getBeansToReloadWeld();


    void $$ha$reloadWeld();


    boolean $$ha$isActiveWeld();
}
