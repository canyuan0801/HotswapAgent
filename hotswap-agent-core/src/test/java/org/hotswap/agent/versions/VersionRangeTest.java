
package org.hotswap.agent.versions;

import java.util.List;

import junit.framework.TestCase;


public class VersionRangeTest extends TestCase {
    private static final String CHECK_NUM_RESTRICTIONS = "check number of restrictions";

    private static final String CHECK_UPPER_BOUND = "check upper bound";

    private static final String CHECK_UPPER_BOUND_INCLUSIVE = "check upper bound is inclusive";

    private static final String CHECK_LOWER_BOUND = "check lower bound";

    private static final String CHECK_LOWER_BOUND_INCLUSIVE = "check lower bound is inclusive";

    private static final String CHECK_VERSION_RECOMMENDATION = "check version recommended";

    private static final String CHECK_SELECTED_VERSION_KNOWN = "check selected version known";

    private static final String CHECK_SELECTED_VERSION = "check selected version";

    public void testRange() throws InvalidVersionSpecificationException {

        VersionRange range = VersionRange.createFromVersionSpec("(,1.0]");
        List<Restriction> restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        Restriction restriction = restrictions.get(0);
        assertNull(CHECK_LOWER_BOUND, restriction.getLowerBound());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("1.0");
        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.0", range.getRecommendedVersion().toString());
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());





        assertTrue(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertEquals(CHECK_SELECTED_VERSION, "1.0", range.getSelectedVersion().toString());

        range = VersionRange.createFromVersionSpec("[1.0]");
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.0", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("[1.2,1.3]");
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("[1.0,2.0)");
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.0", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "2.0", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("[1.5,)");
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.5", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("(,1.0],[1.2,)");
        restrictions = range.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertNull(CHECK_LOWER_BOUND, restriction.getLowerBound());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        assertNull(CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion());
        assertFalse(CHECK_SELECTED_VERSION_KNOWN, range.isSelectedVersionKnown());
        assertNull(CHECK_SELECTED_VERSION, range.getSelectedVersion());

        range = VersionRange.createFromVersionSpec("[1.0,)");
        assertFalse(range.containsVersion(new ArtifactVersion("1.0-SNAPSHOT")));

        range = VersionRange.createFromVersionSpec("[1.0,1.1-SNAPSHOT]");
        assertTrue(range.containsVersion(new ArtifactVersion("1.1-SNAPSHOT")));

        range = VersionRange.createFromVersionSpec("[5.0.9.0,5.0.10.0)");
        assertTrue(range.containsVersion(new ArtifactVersion("5.0.9.0")));
    }

    public void testInvalidRanges() {
        checkInvalidRange("(1.0)");
        checkInvalidRange("[1.0)");
        checkInvalidRange("(1.0]");
        checkInvalidRange("(1.0,1.0]");
        checkInvalidRange("[1.0,1.0)");
        checkInvalidRange("(1.0,1.0)");
        checkInvalidRange("[1.1,1.0]");
        checkInvalidRange("[1.0,1.2),1.3");

        checkInvalidRange("[1.0,1.2),(1.1,1.3]");

        checkInvalidRange("[1.1,1.3),(1.0,1.2]");

        checkInvalidRange("(1.1,1.2],[1.0,1.1)");
    }

    public void testIntersections() throws InvalidVersionSpecificationException {
        VersionRange range1 = VersionRange.createFromVersionSpec("[1.0,2.0]");
        VersionRange range2 = VersionRange.createFromVersionSpec("1.1");
        VersionRange mergedRange = range1.restrict(range2);




        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.1", mergedRange.getRecommendedVersion().toString());
        List<Restriction> restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());

        Restriction restriction = restrictions.get(0);
        assertNotNull(CHECK_LOWER_BOUND, restriction.getLowerBound());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNotNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        mergedRange = range2.restrict(range1);
        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.1", mergedRange.getRecommendedVersion().toString());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertNotNull(CHECK_LOWER_BOUND, restriction.getLowerBound());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNotNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());


        range1 = VersionRange.createFromVersionSpec("[1.0,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.1", mergedRange.getRecommendedVersion().toString());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNotNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.1", mergedRange.getRecommendedVersion().toString());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNotNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.1]");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertEquals(CHECK_VERSION_RECOMMENDATION, "1.1", mergedRange.getRecommendedVersion().toString());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertNotNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertNotNull(CHECK_UPPER_BOUND, restriction.getUpperBound());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
















































































































        range1 = VersionRange.createFromVersionSpec("[1.1,1.3]");
        range2 = VersionRange.createFromVersionSpec("(1.1,)");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("(,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.1,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.2,)");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("(,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("(1.2,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("(1.2,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.2,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.1]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.1", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.1)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.1", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 1, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.4", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2),(1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2),(1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertFalse(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertFalse(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("(,1.1),(1.4,)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());

        range1 = VersionRange.createFromVersionSpec("(,1.1],[1.4,)");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());

        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.6,]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 2, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.5,]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 3, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(2);
        assertEquals(CHECK_LOWER_BOUND, "1.5", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.5", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.7]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.5,1.6]");
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 3, restrictions.size());
        restriction = restrictions.get(0);
        assertEquals(CHECK_LOWER_BOUND, "1.1", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.2", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(1);
        assertEquals(CHECK_LOWER_BOUND, "1.3", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.4", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());
        restriction = restrictions.get(2);
        assertEquals(CHECK_LOWER_BOUND, "1.5", restriction.getLowerBound().toString());
        assertTrue(CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive());
        assertEquals(CHECK_UPPER_BOUND, "1.6", restriction.getUpperBound().toString());
        assertTrue(CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive());


        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        range1 = range1.restrict(range2);
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());

        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        range2 = range1.restrict(range2);
        mergedRange = range1.restrict(range2);
        assertNull(CHECK_VERSION_RECOMMENDATION, mergedRange.getRecommendedVersion());
        restrictions = mergedRange.getRestrictions();
        assertEquals(CHECK_NUM_RESTRICTIONS, 0, restrictions.size());
    }

    public void testReleaseRangeBoundsContainsSnapshots() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0,1.2]");

        assertTrue(range.containsVersion(new ArtifactVersion("1.1-SNAPSHOT")));
        assertTrue(range.containsVersion(new ArtifactVersion("1.2-SNAPSHOT")));
        assertFalse(range.containsVersion(new ArtifactVersion("1.0-SNAPSHOT")));
    }

    public void testSnapshotRangeBoundsCanContainSnapshots() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0,1.2-SNAPSHOT]");

        assertTrue(range.containsVersion(new ArtifactVersion("1.1-SNAPSHOT")));
        assertTrue(range.containsVersion(new ArtifactVersion("1.2-SNAPSHOT")));

        range = VersionRange.createFromVersionSpec("[1.0-SNAPSHOT,1.2]");

        assertTrue(range.containsVersion(new ArtifactVersion("1.0-SNAPSHOT")));
        assertTrue(range.containsVersion(new ArtifactVersion("1.1-SNAPSHOT")));
    }

    public void testSnapshotSoftVersionCanContainSnapshot() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("1.0-SNAPSHOT");

        assertTrue(range.containsVersion(new ArtifactVersion("1.0-SNAPSHOT")));
    }

    private void checkInvalidRange(String version) {
        try {
            VersionRange.createFromVersionSpec(version);
            fail("Version " + version + " should have failed to construct");
        } catch (InvalidVersionSpecificationException expected) {


        }
    }

    public void testContains() throws InvalidVersionSpecificationException {
        ArtifactVersion actualVersion = new ArtifactVersion("2.0.5.Final");
        assertTrue(enforceVersion("2.0.5", actualVersion));
        assertFalse(enforceVersion("2.0.4", actualVersion));
        assertTrue(enforceVersion("[2.0.5]", actualVersion));
        assertFalse(enforceVersion("[2.0.6,)", actualVersion));
        assertFalse(enforceVersion("[2.0.6]", actualVersion));
        assertTrue(enforceVersion("[2.0,2.1]", actualVersion));
        assertFalse(enforceVersion("[2.0,2.0.3]", actualVersion));
        assertTrue(enforceVersion("[2.0,2.0.5]", actualVersion));
        assertFalse(enforceVersion("[2.0,2.0.5)", actualVersion));
    }

    public boolean enforceVersion(String requiredVersionRange, ArtifactVersion actualVersion) throws InvalidVersionSpecificationException {
        VersionRange vr = null;

        vr = VersionRange.createFromVersionSpec(requiredVersionRange);

        return vr.containsVersion(actualVersion);
    }

    public void testOrder0() {


    }
}
