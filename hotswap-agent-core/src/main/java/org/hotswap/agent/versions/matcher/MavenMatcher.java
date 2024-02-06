
package org.hotswap.agent.versions.matcher;

import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.spring.util.PatternMatchUtils;
import org.hotswap.agent.util.spring.util.StringUtils;
import org.hotswap.agent.versions.DeploymentInfo;
import org.hotswap.agent.versions.InvalidVersionSpecificationException;
import org.hotswap.agent.versions.MavenInfo;
import org.hotswap.agent.versions.VersionMatchResult;
import org.hotswap.agent.versions.VersionMatcher;
import org.hotswap.agent.versions.VersionRange;


public class MavenMatcher implements VersionMatcher {
    private static AgentLogger LOGGER = AgentLogger.getLogger(MavenMatcher.class);


	private final VersionRange includes;
	

	private final VersionRange excludes;


	private final String artifactId;


	private final String groupId;


	private final String includesString;
	

	private final String excludesString;


	public MavenMatcher(Maven cfg) throws InvalidVersionSpecificationException {
        this.artifactId = cfg.artifactId();
        this.groupId = cfg.groupId();
        if(StringUtils.hasText(cfg.value())) {
        	 this.includesString = cfg.value().trim();
        	 this.includes = VersionRange.createFromVersionSpec(includesString);
        } else {
        	this.includes = null;
        	this.includesString = null;
        }
        
        if(StringUtils.hasText(cfg.excludeVersion())){
        	this.excludesString = cfg.excludeVersion().trim();
        	this.excludes = VersionRange.createFromVersionSpec(excludesString);
        } else {
        	this.excludes =  null;
        	this.excludesString = null;
        }
    }


	public VersionRange getIncludes() {
		return includes;
	}


	public VersionRange getExcludes() {
		return excludes;
	}


	public String getArtifactId() {
		return artifactId;
	}


	public String getGroupId() {
		return groupId;
	}


	@Override
	public String toString() {
		return "MavenMatcher [groupId=" + groupId + ", artifactId=" + artifactId + ", includes=" + includes
				+ ", excludes=" + excludes + "]";
	}


	@Override
	public VersionMatchResult matches(DeploymentInfo info) {
        if(info.getMaven() == null || info.getMaven().size() == 0) {
            return VersionMatchResult.SKIPPED;
        }
        


		for (MavenInfo mi : info.getMaven()) {
			if (PatternMatchUtils.regexMatch(groupId, mi.getGroupId()) && PatternMatchUtils.regexMatch(artifactId, mi.getArtifactId())) {
				
				if ((includes == null || includes.containsVersion(mi.getVersion())) && (excludes ==null || !excludes.containsVersion(mi.getVersion()))) {
				    LOGGER.debug("Matched {} with {}", this, mi);
				    return VersionMatchResult.MATCHED;
				}


				if (excludes !=null && excludes.containsVersion(mi.getVersion())) {
				    LOGGER.debug("Rejected {} with {}", this, mi);
					return VersionMatchResult.REJECTED;
				}
			}
		}
			

		return VersionMatchResult.SKIPPED;
	}


	@Override
	public boolean isApply() {
		return (StringUtils.hasText(artifactId) && StringUtils.hasText(groupId)) && (StringUtils.hasText(includesString) || StringUtils.hasText(excludesString));
	}
}