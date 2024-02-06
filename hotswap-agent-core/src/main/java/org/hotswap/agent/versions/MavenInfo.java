
package org.hotswap.agent.versions;



public class MavenInfo {


    private final String groupId;


    private final String artifactId;


    private final ArtifactVersion version;


    public MavenInfo(String groupId, String artifactId, String version) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = new ArtifactVersion(version);
    }


    public String getArtifactId() {
        return artifactId;
    }


    public String getGroupId() {
        return groupId;
    }


    public ArtifactVersion getVersion() {
        return version;
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
        MavenInfo other = (MavenInfo) obj;
        if (artifactId == null) {
            if (other.artifactId != null) {
                return false;
            }
        } else if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (groupId == null) {
            if (other.groupId != null) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }


    @Override
    public String toString() {
        return "MavenInfo [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + "]";
    }

}
