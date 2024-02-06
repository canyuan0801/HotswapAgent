
package org.hotswap.agent.plugin.hibernate3.session;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.annotation.Versions;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.AnnotationHelper;


@Plugin(name = "Hibernate3",
        group = "groupHibernate",
        description = "Reload Hibernate configuration after entity create/change.",
        testedVersions = { "3.6" },
        expectedVersions = { "3.6" },
        supportClass = { Hibernate3Transformers.class })
@Versions(maven = { @Maven(value = "[3.0,4.0)", artifactId = "hibernate-core", groupId = "org.hibernate") })
public class Hibernate3Plugin {


    private static final String ENTITY_ANNOTATION = "javax.persistence.Entity";


    private static AgentLogger LOGGER = AgentLogger.getLogger(Hibernate3Plugin.class);


    @Init
    Scheduler scheduler;


    @Init
    ClassLoader appClassLoader;


    String version;



    private final Command reloadSessionFactoryCommand = new ReflectionCommand(this, Hibernate3RefreshCommand.class.getName(), "reloadSessionFactory");


    @Init
    public void init() {
        LOGGER.info("Hibernate3 Session plugin initialized", version);
    }


    boolean enabled = true;


    public void disable() {
        LOGGER.info("Disabling Hibernate3 Session plugin since JPA is active");
        this.enabled = false;
    }


    public void setVersion(String v) {
        this.version = v;
        LOGGER.info("Hibernate Core version '{}'", version);
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void entityReload(CtClass clazz, Class<?> original) {


        if (AnnotationHelper.hasAnnotation(original, ENTITY_ANNOTATION) || AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)) {
            LOGGER.debug("Entity reload class {}, original classloader {}", clazz.getName(), original.getClassLoader());
            refresh(500);
        }
    }


    @OnClassFileEvent(classNameRegexp = ".*", events = { FileEvent.CREATE })
    public void newEntity(CtClass clazz) throws Exception {
        if (AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)) {
            refresh(500);
        }
    }


    @OnResourceFileEvent(path = "/", filter = ".*.hbm.xml")
    public void refreshOnHbm() {
        refresh(500);
    }


    @OnResourceFileEvent(path = "/", filter = ".*.cfg.xml")
    public void refreshOnCfg() {
        refresh(500);
    }




    public void refresh(int timeout) {
        if (enabled) {
            scheduler.scheduleCommand(reloadSessionFactoryCommand, timeout);
        }
    }

}
