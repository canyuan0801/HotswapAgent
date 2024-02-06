
package org.hotswap.agent.logging;

import org.hotswap.agent.testData.SimplePlugin;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;


public class AgentLoggerTest {
    Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    AgentLoggerHandler handler = context.mock(AgentLoggerHandler.class);


    @Test
    public void testDefaultLevel() throws Exception {
        final Class clazz = SimplePlugin.class;
        final String message = "Test";
        final Throwable error = new Throwable();
        AgentLogger.setHandler(handler);

        context.checking(new Expectations() {{
            oneOf(handler).print(clazz, AgentLogger.Level.ERROR, message, null);
            oneOf(handler).print(clazz, AgentLogger.Level.INFO, message, error);
            never(handler).print(clazz, AgentLogger.Level.TRACE, message, error);
        }});


        AgentLogger logger = AgentLogger.getLogger(clazz);
        logger.error(message);
        logger.info(message, error);
        logger.trace(message, error);


        AgentLogger.setHandler(new AgentLoggerHandler());

        context.assertIsSatisfied();
    }
}
