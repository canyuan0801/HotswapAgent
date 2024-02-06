
package org.hotswap.agent.versions.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;

import org.hotswap.agent.annotation.Manifest;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.spring.util.PatternMatchUtils;
import org.hotswap.agent.util.spring.util.StringUtils;
import org.hotswap.agent.versions.DeploymentInfo;
import org.hotswap.agent.versions.ArtifactVersion;
import org.hotswap.agent.versions.InvalidVersionSpecificationException;
import org.hotswap.agent.versions.ManifestInfo;
import org.hotswap.agent.versions.VersionMatchResult;
import org.hotswap.agent.versions.VersionMatcher;
import org.hotswap.agent.versions.VersionRange;


public class ManifestMatcher implements VersionMatcher {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ManifestMatcher.class);


    private final VersionRange includes;
    

    private final VersionRange excludes;
    

    private final Map<Name, String> properties;


    private final String includesString;
    

    private final String excludesString;


    private final Name[] version;


    public ManifestMatcher(Manifest cfg) throws InvalidVersionSpecificationException {
        if (StringUtils.hasText(cfg.value())) {
            this.includesString = cfg.value().trim();
            this.includes = VersionRange.createFromVersionSpec(includesString);
        } else {
            this.includes = null;
            this.includesString = null;
        }

        if (StringUtils.hasText(cfg.excludeVersion())) {
            this.excludesString = cfg.excludeVersion().trim();
            this.excludes = VersionRange.createFromVersionSpec(excludesString);
        } else {
            this.excludes = null;
            this.excludesString = null;
        }
        if(cfg.versionName() == null || cfg.versionName().length == 0) {
            version = null;
        } else {
            List<Name >versions = new ArrayList<>();
            for(String versionName: cfg.versionName()) {
                if (StringUtils.hasText(versionName)) {
                    versions.add(new Name(versionName));
                }
            }
            version = versions.toArray(new Name[versions.size()]);
        }
        
        if (cfg.names() != null && cfg.names().length > 0) {
            this.properties = new HashMap<>();
            for (org.hotswap.agent.annotation.Name name : cfg.names()) {
                if(StringUtils.hasText(name.key()) && StringUtils.hasText(name.value())) {
                    this.properties.put(new Name(name.key()), name.value());
                }
            }
        } else {
            this.properties = Collections.emptyMap();
        }
    }


    public VersionRange getIncludes() {
        return includes;
    }


    public VersionRange getExcludes() {
        return excludes;
    }


    public Map<Name, String> getProperties() {
        return properties;
    }


    public VersionMatchResult matches(DeploymentInfo info) {

        if(info.getManifest() == null  || info.getManifest().size() == 0) {
            return VersionMatchResult.SKIPPED;
        }
        
       	for (ManifestInfo manifest: info.getManifest()) {
       	    VersionMatchResult result = match(manifest);
       	 
       		if(VersionMatchResult.MATCHED.equals(result)){
       		    LOGGER.debug("Matched {} with {}", this, manifest);
       			return VersionMatchResult.MATCHED;
       		}
            if(VersionMatchResult.REJECTED.equals(result)){
                LOGGER.debug("Rejected {} with {}", this, manifest);
                return VersionMatchResult.REJECTED;
            }
       	}
       	

       	return VersionMatchResult.SKIPPED;
    }


    private VersionMatchResult match(ManifestInfo manifest) {
    	if(manifest == null) {
    		return VersionMatchResult.SKIPPED;
    	}

        String artifactVersion = manifest.getValue(this.version);
        if(StringUtils.isEmpty(artifactVersion)){
            return VersionMatchResult.SKIPPED;
        }
        

        if(properties.size() == 0) {
            return VersionMatchResult.SKIPPED;
        } else {
            for(Map.Entry<Name,String> e: properties.entrySet()) {
                String v = manifest.getValue(e.getKey());

                if(!StringUtils.hasText(v) || !PatternMatchUtils.regexMatch(e.getValue(), v)) {
                    return VersionMatchResult.SKIPPED;
                }
            }
        }
        ArtifactVersion version = new ArtifactVersion(artifactVersion);
        
        if(excludes != null && excludes.containsVersion(version)) {
            return VersionMatchResult.REJECTED;
        }
        
        if(includes != null && !includes.containsVersion(version)) {
            return VersionMatchResult.REJECTED;
        } else {
            return VersionMatchResult.MATCHED;
        }
        
    }
    

    @Override
    public String toString() {
        return "ManifestMatcher [properties=" + properties + ", includes=" + includes + ", excludes=" + excludes + "]";
    }


    @Override
    public boolean isApply() {
        return (StringUtils.hasText(includesString) || StringUtils.hasText(excludesString)) && (version != null);
    }

}