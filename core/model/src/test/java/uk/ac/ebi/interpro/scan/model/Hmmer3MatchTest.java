/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.interpro.scan.model;

import org.junit.Test;
import org.apache.commons.lang.SerializationUtils;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;

/**
 * Tests cases for {@link Hmmer3Match}.
 *
 * @author  Antony Quinn
 * @version $Id$
 */
public class Hmmer3MatchTest extends TestCase {

    /**
     * Tests the equals() method works as expected
     */
    @Test
    public void testMatchEquals() {
        Hmmer3Match original = new Hmmer3Match(
                new Signature("PF02310", "B12-binding"), 0.035, 3.7e-9,
                new HashSet<Hmmer3Match.Hmmer3Location>(Arrays.asList(
                        new Hmmer3Match.Hmmer3Location(3, 107, 3.0, 3.7e-9, 1, 104, HmmBounds.N_TERMINAL_COMPLETE, 1, 2)
                ))
        );
        Hmmer3Match copy = (Hmmer3Match)SerializationUtils.clone(original);
        assertEquals("Original should equal itself", original, original);
        assertEquals("Original and copy should be equal", original, copy);
        assertFalse("Original and copy should not be equal",
                original.equals(
                        new Hmmer3Match(new Signature("1", "A"), 1, 2, 
                                (Set<Hmmer3Match.Hmmer3Location>)SerializationUtils.clone(
                                        new HashSet<Hmmer3Match.Hmmer3Location>(original.getLocations())))
                ));
        // Test sets
        Set<Match> originalSet = new HashSet<Match>();
        Set<Match> copySet     = new HashSet<Match>();
        originalSet.add(original);
        copySet.add(copy);
        assertEquals("Original set should equal itself", originalSet, originalSet);
        assertEquals("Original and copy sets should be equal", originalSet, copySet);
    }

    /**
     * Tests the equals() method works as expected
     */
    @Test
    public void testLocationEquals() {
        HmmerLocation original = new Hmmer3Match.Hmmer3Location(3, 107, 3.0, 3.7e-9, 1, 104, HmmBounds.N_TERMINAL_COMPLETE, 1, 2);
        HmmerLocation copy = (HmmerLocation)SerializationUtils.clone(original);
        // Original should equal itself
        assertEquals(original, original);
        // Original and copy should be equal
        assertEquals(original, copy);
        // Original and copy should not be equal
        copy = new Hmmer3Match.Hmmer3Location(1, 2, 3, 4, 5, 6, HmmBounds.COMPLETE, 7, 8);
        assertFalse("Original and copy should not be equal", original.equals(copy));
    }
    
}