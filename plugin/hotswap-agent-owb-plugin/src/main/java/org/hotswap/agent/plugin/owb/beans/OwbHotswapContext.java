
package org.hotswap.agent.plugin.owb.beans;

import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;



public interface OwbHotswapContext {


    void destroy(Contextual<?> contextual);


    boolean isActive();


    public <T> T get(Contextual<T> contextual);


    void $$ha$addBeanToReloadOwb(Contextual<Object> bean);


    Set<Contextual<Object>> $$ha$getBeansToReloadOwb();


    void $$ha$reloadOwb();


    boolean $$ha$isActiveOwb();
}
