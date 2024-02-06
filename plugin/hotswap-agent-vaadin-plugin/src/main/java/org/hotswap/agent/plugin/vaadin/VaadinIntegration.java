
package org.hotswap.agent.plugin.vaadin;

import javax.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hotswap.agent.logging.AgentLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

public class VaadinIntegration {

    private static final AgentLogger LOGGER = AgentLogger
            .getLogger(VaadinIntegration.class);

    private VaadinServlet vaadinServlet = null;


    public void servletInitialized(VaadinServlet servlet) {
        vaadinServlet = servlet;
        LOGGER.info("{} initialized for servlet {}", getClass(), servlet);
    }


    public void updateRoutes(HashSet<Class<?>> addedClasses,
                             HashSet<Class<?>> modifiedClasses) {
        assert (vaadinServlet != null);

        LOGGER.debug("The following classes were added:");
        addedClasses.forEach(clazz -> LOGGER.debug("+ {}", clazz));

        LOGGER.debug("The following classes were modified:");
        modifiedClasses.forEach(clazz -> LOGGER.debug("# {}", clazz));

        Method getInstanceMethod = null;
        Object getInstanceMethodParam = null;
        try {

            getInstanceMethod = ApplicationRouteRegistry.class.getMethod("getInstance", VaadinContext.class);
            getInstanceMethodParam = vaadinServlet.getService().getContext();
        } catch (NoSuchMethodException ex1) {

            LOGGER.debug("ApplicationRouteRegistry::getInstance(VaadinContext) not found");
            try {
                getInstanceMethod = ApplicationRouteRegistry.class.getMethod("getInstance", ServletContext.class);
                getInstanceMethodParam = vaadinServlet.getServletContext();
            } catch (NoSuchMethodException ex2) {

                LOGGER.warning("Unable to obtain ApplicationRouteRegistry instance; routes are not updated ");
                return;
            }
        }

        try {
            ApplicationRouteRegistry registry = (ApplicationRouteRegistry)
                    getInstanceMethod.invoke(null, getInstanceMethodParam);
            updateRouteRegistry(registry, addedClasses, modifiedClasses,
                    Collections.emptySet());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warning("Unable to obtain ApplicationRouteRegistry instance; routes are not updated:", ex);
        }
    }


    public void reload() {
        VaadinService vaadinService = vaadinServlet.getService();
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor.getLiveReloadFromService(vaadinService);
        if (liveReload.isPresent()) {
            liveReload.get().reload();
            LOGGER.info("Live reload triggered");
        }
    }


    private static void updateRouteRegistry(RouteRegistry registry,
                                            Set<Class<?>> addedClasses,
                                            Set<Class<?>> modifiedClasses,
                                            Set<Class<?>> deletedClasses) {
        RouteConfiguration routeConf = RouteConfiguration.forRegistry(registry);

        registry.update(() -> {

            Stream.concat(deletedClasses.stream(),
                    modifiedClasses.stream().filter(
                            clazz -> !clazz.isAnnotationPresent(Route.class)))
                    .filter(Component.class::isAssignableFrom)
                    .forEach(clazz -> {
                        Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
                        routeConf.removeRoute(componentClass);
                    });


            Stream.concat(addedClasses.stream(), modifiedClasses.stream())
                    .distinct()
                    .filter(Component.class::isAssignableFrom)
                    .filter(clazz -> clazz.isAnnotationPresent(Route.class))
                    .forEach(clazz -> {
                        Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
                        routeConf.removeRoute(componentClass);
                        routeConf.setAnnotatedRoute(componentClass);
                    });
        });
    }
}
