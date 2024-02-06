
package org.hotswap.agent.versions.matcher;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hotswap.agent.annotation.Manifest;
import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.annotation.Name;
import org.hotswap.agent.annotation.Versions;
import org.hotswap.agent.versions.DeploymentInfo;
import org.hotswap.agent.versions.ManifestInfo;
import org.hotswap.agent.versions.MavenInfo;
import org.hotswap.agent.versions.VersionMatchResult;
import org.junit.Test;

public class ManifestMatcherTest {


    @Test
    public void testMatches() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();


        ManifestInfo manifest = new ManifestInfo(new java.util.jar.Manifest(this.getClass().getResourceAsStream("/org/hotswap/agent/versions/matcher/TEST.MF")));

        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);
        PluginMatcher p = new PluginMatcher(MatchingPlugin.class);
        System.err.println(p);
        assertEquals("Matching",VersionMatchResult.MATCHED, p.matches(info));
    }


    @Test
    public void testMatchesManifestEmptyLine() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();


        ManifestInfo manifest = new ManifestInfo(new java.util.jar.Manifest(this.getClass().getResourceAsStream("/org/hotswap/agent/versions/matcher/TEST_EMPTYLINE.MF")));

        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);
        PluginMatcher p = new PluginMatcher(MatchingPlugin2.class);
        System.err.println(p);
        assertEquals("Matching",VersionMatchResult.MATCHED, p.matches(info));
    }


    @Test
    public void testFails() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();


        ManifestInfo manifest = new ManifestInfo(new java.util.jar.Manifest(this.getClass().getResourceAsStream("/org/hotswap/agent/versions/matcher/TEST.MF")));

        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);


        PluginMatcher p = new PluginMatcher(NotMatchingPlugin.class);
        assertEquals("Not Matching",VersionMatchResult.REJECTED, p.matches(info));

    }

    @Test
    public void testFailedEmptyArtifactInfo() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();
        ManifestInfo manifest = new ManifestInfo(null);
        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);
        PluginMatcher p = new PluginMatcher(NotMatchingPlugin.class);
        assertEquals("Failed Matching",VersionMatchResult.REJECTED, p.matches(info));
    }

    @Test
    public void testSkippedEmpty2() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();

        ManifestInfo manifest = new ManifestInfo(null);

        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);


        PluginMatcher p = new PluginMatcher(PluginMatcherTest.class);

        assertEquals("Skipped Matching",VersionMatchResult.SKIPPED, p.matches(info));
    }


    @Test
    public void testSkipped() throws IOException {
        Set<MavenInfo> maven = new HashSet<MavenInfo>();

        ManifestInfo manifest = new ManifestInfo(new java.util.jar.Manifest(this.getClass().getResourceAsStream("/org/hotswap/agent/versions/matcher/TEST.MF")));

        DeploymentInfo info = new DeploymentInfo(maven, Collections.singleton(manifest));
        System.err.println(info);

        PluginMatcher p = new PluginMatcher(PluginMatcherTest.class);

        assertEquals("Skipped Matching",VersionMatchResult.SKIPPED, p.matches(info));
    }

    @Versions(
            maven = {
                    @Maven(value = "[2.2,)", artifactId = "myfaces-api", groupId = "org.apache.myfaces.core"),
                    @Maven(value = "[2.2,)", artifactId = "myfaces-impl", groupId = "org.apache.myfaces.core")
            },
            manifest = {
                    @Manifest(names = {
                            @Name(key = Name.BundleSymbolicName, value = "org.apache.myfaces.core.impl"),
                            @Name(key=Name.ImplementationVendor, value="The.*Apache.*Software.*Foundation")
                    }, value = "[2.2,)")
            })
    private static class MatchingPlugin {

    }

@Versions(
        manifest = {
            @Manifest(value="[2.0,)",versionName = Name.SpecificationVersion, names={
                    @Name(key=Name.ImplementationTitle,value="javax.el"),
                    @Name(key=Name.ImplementationVendor, value="Apache.*Software.*Foundation")
            })
        }
    )
    private static class MatchingPlugin2 {

    }

    @Versions(
            maven = {
                    @Maven(value = "[3.2,)", artifactId = "myfaces-api", groupId = "org.apache.myfaces.core"),
                    @Maven(value = "[3.2,)", artifactId = "myfaces-impl", groupId = "org.apache.myfaces.core")
            },
            manifest = {
                    @Manifest(names = { @Name(key = Name.BundleSymbolicName, value = " org.apache.myfaces.core.impl") }, value = "[3.2,)")
            })
    private static class NotMatchingPlugin {

    }

}
