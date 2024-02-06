
package org.hotswap.agent.versions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.spring.io.resource.Resource;
import org.hotswap.agent.util.spring.path.PathMatchingResourcePatternResolver;


public class DeploymentInfo {


    private static AgentLogger LOGGER = AgentLogger.getLogger(DeploymentInfo.class);
    

    private Set<MavenInfo> maven = new LinkedHashSet<>();


    private Set<ManifestInfo> manifest;


    public DeploymentInfo(Set<MavenInfo> maven, Set<ManifestInfo> manifest) {
        this.maven = maven;
        this.manifest = manifest;
    }


    public Set<ManifestInfo> getManifest() {
        return manifest;
    }


    public Set<MavenInfo> getMaven() {
        return maven;
    }


    public void setMaven(Set<MavenInfo> maven) {
        this.maven = maven;
    }


    public boolean isEmpty() {
        return maven == null || maven.size() == 0 || manifest == null || manifest.isEmpty();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeploymentInfo other = (DeploymentInfo) obj;
        if (manifest == null) {
            if (other.manifest != null) {
                return false;
            }
        } else if (!manifest.equals(other.manifest)) {
            return false;
        }
        if (maven == null) {
            if (other.maven != null) {
                return false;
            }
        } else if (!maven.equals(other.maven)) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((manifest == null) ? 0 : manifest.hashCode());
        result = prime * result + ((maven == null) ? 0 : maven.hashCode());
        return result;
    }


    public void setManifest(Set<ManifestInfo> manifest) {
        this.manifest = manifest;
    }


    public static DeploymentInfo fromClassLoader(ClassLoader classloader) {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classloader);

            Set<MavenInfo> maven = new LinkedHashSet<>();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classloader);

            try {
                Enumeration<URL> urls = classloader.getResources("META-INF/maven/");
                while (urls.hasMoreElements()) {
                    URL u = urls.nextElement();
                    try {
                        Resource[] resources = resolver.getResources(u.toExternalForm() + "**/pom.properties");
                        if (resources != null) {
                            if(LOGGER.isDebugEnabled()){
                                LOGGER.debug("META-INF/mavenpom.properties FOUND:{}", Arrays.toString(resources));
                            }
                            for (Resource resource : resources) {
                                MavenInfo m = getMavenInfo(resource);
                                if (m != null) {
                                    maven.add(m);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Error trying to find maven properties", e);
            }

            return new DeploymentInfo(maven, getManifest(classloader));
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }


    private static MavenInfo getMavenInfo(Resource resource) {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("RESOURCE_MAVEN:" + resource.getClass() + "-->" + resource.getDescription() + "----" + resource.getFilename());
        }
        try (InputStream is = resource.getInputStream()) {
            Properties p = new Properties();
            p.load(is);

            return new MavenInfo(p.getProperty("groupId"), p.getProperty("artifactId"), p.getProperty("version"));
        } catch (IOException e) {
            LOGGER.debug("Error trying to read maven properties", e);
        }
        return null;
    }


    public static Set<ManifestInfo> getManifest(ClassLoader classloader) {

        Set<ManifestInfo> manifests = new LinkedHashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classloader);

        try {
            Enumeration<URL> urls = classloader.getResources("META-INF/MANIFEST.MF");
            while (urls.hasMoreElements()) {
                URL u = urls.nextElement();
                try {
                    Resource[] resources = resolver.getResources(u.toExternalForm());
                    if (resources != null) {
                        if(LOGGER.isDebugEnabled()){
                            LOGGER.debug("META-INF/MANIFEST.MF FOUND:\n" + Arrays.toString(resources));
                        }
                        for (Resource resource : resources) {
                            ManifestInfo m = getManifest(resource);
                            if (m != null) {
                                manifests.add(m);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Error trying to get manifest entries", e);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Error trying to get manifest entries", e);
        }
        return manifests;
    }


    public static ManifestInfo getManifest(Resource resource) {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("RESOURCE_MANIFEST:" + resource.getClass() + "-->" + resource.getDescription() + "----" + resource.getFilename());
        }
        try (InputStream is = resource.getInputStream()) {
            Manifest man = new Manifest(is);
            if (man != null) {
                return new ManifestInfo(man);
            }
        } catch (IOException e) {
            LOGGER.debug("Error trying to read manifest", e);
        }
        return null;
    }


    @Override
    public String toString() {
        return "DeploymentInfo [maven=" + maven + ", manifest=" + manifest + "]";
    }
}
