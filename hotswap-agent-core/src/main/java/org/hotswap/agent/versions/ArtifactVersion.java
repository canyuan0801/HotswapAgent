package org.hotswap.agent.versions;



import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.NoSuchElementException;



public class ArtifactVersion implements Comparable<ArtifactVersion> {


    private final String version;


    private Integer majorVersion;


    private Integer minorVersion;


    private Integer incrementalVersion;


    private Integer buildNumber;


    private String qualifier;


    private ComparableVersion comparable;


    public ArtifactVersion(String version) {
        this.version = version != null ? version.trim() : "";
        parseVersion(version);
    }


    public String getVersion() {
        return version;
    }


    @Override
    public int hashCode() {
        return 11 + comparable.hashCode();
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ArtifactVersion)) {
            return false;
        }

        return compareTo(ArtifactVersion.class.cast(other)) == 0;
    }


    public int compareTo(ArtifactVersion otherVersion) {
        return this.comparable.compareTo(otherVersion.comparable);
    }


    public int getMajorVersion() {
        return majorVersion != null ? majorVersion : 0;
    }


    public int getMinorVersion() {
        return minorVersion != null ? minorVersion : 0;
    }


    public int getIncrementalVersion() {
        return incrementalVersion != null ? incrementalVersion : 0;
    }


    public int getBuildNumber() {
        return buildNumber != null ? buildNumber : 0;
    }


    public String getQualifier() {
        return qualifier;
    }


    public final void parseVersion(String version) {
        comparable = new ComparableVersion(version);

        int index = version.indexOf("-");

        String part1;
        String part2 = null;

        if (index < 0) {
            part1 = version;
        } else {
            part1 = version.substring(0, index);
            part2 = version.substring(index + 1);
        }

        if (part2 != null) {
            try {
                if ((part2.length() == 1) || !part2.startsWith("0")) {
                    buildNumber = Integer.valueOf(part2);
                } else {
                    qualifier = part2;
                }
            } catch (NumberFormatException e) {
                qualifier = part2;
            }
        }

        if ((!part1.contains(".")) && !part1.startsWith("0")) {
            try {
                majorVersion = Integer.valueOf(part1);
            } catch (NumberFormatException e) {

                qualifier = version;
                buildNumber = null;
            }
        } else {
            boolean fallback = false;

            StringTokenizer tok = new StringTokenizer(part1, ".");
            try {
                majorVersion = getNextIntegerToken(tok);
                if (tok.hasMoreTokens()) {
                    minorVersion = getNextIntegerToken(tok);
                }
                if (tok.hasMoreTokens()) {
                    incrementalVersion = getNextIntegerToken(tok);
                }
                if (tok.hasMoreTokens()) {
                    qualifier = tok.nextToken();
                    fallback = Pattern.compile("\\d+").matcher(qualifier).matches();
                }


                if (part1.contains("..") || part1.startsWith(".") || part1.endsWith(".")) {
                    fallback = true;
                }
            } catch (NumberFormatException e) {
                fallback = true;
            }

            if (fallback) {

                qualifier = version;
                majorVersion = null;
                minorVersion = null;
                incrementalVersion = null;
                buildNumber = null;
            }
        }
    }


    private static Integer getNextIntegerToken(StringTokenizer tok) {
        try {
            String s = tok.nextToken();
            if ((s.length() > 1) && s.startsWith("0")) {
                throw new NumberFormatException("Number part has a leading 0: '" + s + "'");
            }
            return Integer.valueOf(s);
        } catch (NoSuchElementException e) {
            throw new NumberFormatException("Number is invalid");
        }
    }


    public String dump() {
        return "ArtifactVersion [version=" + version + ", majorVersion=" + majorVersion + ", minorVersion=" + minorVersion + ", incrementalVersion=" + incrementalVersion + ", buildNumber=" + buildNumber + ", qualifier=" + qualifier + ", comparable=" + comparable + "]";
    }


    @Override
    public String toString() {
        return comparable.toString();
    }
}
