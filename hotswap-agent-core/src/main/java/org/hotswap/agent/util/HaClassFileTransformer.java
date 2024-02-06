package org.hotswap.agent.util;

import java.lang.instrument.ClassFileTransformer;


public interface HaClassFileTransformer extends ClassFileTransformer {


    public boolean isForRedefinitionOnly();

}
