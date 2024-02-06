
package org.hotswap.agent.versions;


public interface VersionMatcher {


    boolean isApply();


    VersionMatchResult matches(DeploymentInfo info);
}