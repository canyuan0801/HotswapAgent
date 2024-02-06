
package org.hotswap.agent.versions.matcher;

import java.util.ArrayList;
import java.util.List;

import org.hotswap.agent.annotation.Manifest;
import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.annotation.Versions;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.versions.DeploymentInfo;
import org.hotswap.agent.versions.InvalidVersionSpecificationException;
import org.hotswap.agent.versions.VersionMatchResult;
import org.hotswap.agent.versions.VersionMatcher;


public class AbstractMatcher implements VersionMatcher{
	

	protected AgentLogger LOGGER = AgentLogger.getLogger(getClass());
	

	protected final List<VersionMatcher> matchers = new ArrayList<>();
	

	protected boolean shouldApply = Boolean.FALSE;
	

	public AbstractMatcher(Versions versions) {
		if(versions == null) {
		    return;
		}
		Maven[] maven = versions.maven();
		
		Manifest[] manifest = versions.manifest();
		
		if (maven != null) {
			for (Maven cfg : maven) {
				try {
					MavenMatcher m = new MavenMatcher(cfg);
					if (m.isApply()) {
						matchers.add(m);
						shouldApply = true;
					}
				} catch (InvalidVersionSpecificationException e) {
					LOGGER.error("Unable to parse Maven info for {}", e, cfg);
				}
			}
		}
		if (manifest != null) {
			for (Manifest cfg : manifest) {
				try {
					ManifestMatcher m = new ManifestMatcher(cfg);
					if (m.isApply()) {
						matchers.add(m);
						shouldApply = true;
					}
				} catch (InvalidVersionSpecificationException e) {
					LOGGER.error("Unable to parse Manifest info for {}", e, cfg);
				}
			}
		}
	}
	

	@Override
	public boolean isApply() {
		return shouldApply;
	}



	@Override
	public VersionMatchResult matches(DeploymentInfo info) {
		if (matchers.size() == 0) {
			return VersionMatchResult.SKIPPED;
		}
		for (VersionMatcher m : matchers) {
		    VersionMatchResult result = m.matches(info);
		    if(VersionMatchResult.MATCHED.equals(result)) {
		        LOGGER.debug("Matched:{}", m);
		        return VersionMatchResult.MATCHED;
		    }else if(VersionMatchResult.REJECTED.equals(result)) {
		        LOGGER.debug("Rejected:{}", m);
		        return VersionMatchResult.REJECTED;
		    }
		}

		LOGGER.debug("Rejected: Matchers existed, none matched!");
		return VersionMatchResult.REJECTED;
	}


    @Override
    public String toString() {
        return "AbstractMatcher [matchers=" + matchers + ", shouldApply=" + shouldApply + "]";
    }
}
