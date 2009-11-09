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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.SAXException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import uk.ac.ebi.interpro.scan.genericjpadao.GenericDAO;

/**
 * Tests cases for {@link Protein}.
 *
 * @author  Antony Quinn
 * @version $Id$
 * @since   1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ProteinTest extends AbstractTest<Protein> {

    private static Logger LOGGER = Logger.getLogger(ProteinTest.class);

    // http://www.uniprot.org/uniparc/UPI0000000001.fasta
    private static final String MULTILINE  =
                              "MGAAASIQTTVNTLSERISSKLEQEANASAQTKCDIEIGNFYIRQNHGCNLTVKNMCSAD\n" +
                              "ADAQLDAVLSAATETYSGLTPEQKAYVPAMFTAALNIQTSVNTVVRDFENYVKQTCNSSA\n" +
                              "VVDNKLKIQNVIIDECYGAPGSPTNLEFINTGSSKGNCAIKALMQLTTKATTQIAPKQVA\n" +
                              "GTGVQFYMIVIGVIILAALFMYYAKRMLFTSTNDKIKLILANKENVHWTTYMDTFFRTSP\n" +
                              "MVIATTDMQN";
    private static final String SINGLELINE  =
                               "MGAAASIQTTVNTLSERISSKLEQEANASAQTKCDIEIGNFYIRQNHGCNLTVKNMCSAD" +
                               "ADAQLDAVLSAATETYSGLTPEQKAYVPAMFTAALNIQTSVNTVVRDFENYVKQTCNSSA" +
                               "VVDNKLKIQNVIIDECYGAPGSPTNLEFINTGSSKGNCAIKALMQLTTKATTQIAPKQVA" +
                               "GTGVQFYMIVIGVIILAALFMYYAKRMLFTSTNDKIKLILANKENVHWTTYMDTFFRTSP" +
                               "MVIATTDMQN";

    // First line of UPI0000000001.fasta
    private static final String GOOD = "MGAAASIQTTVNTLSERISSKLEQEANASAQTKCDIEIGNFYIRQNHGCNLTVKNMCSAD";

    // echo -n "MGAAASIQTTVNTLSERISSKLEQEANASAQTKCDIEIGNFYIRQNHGCNLTVKNMCSAD" | md5sum
    private static final String GOOD_MD5 = "9d380adca504b0b1a2654975c340af78";


    // Contains "." so should fail when create protein
    private static final String BAD = "MGAAASIQTTVNTLSERISSKLEQE.ANASAQTKCDIEIGNFYIRQNHGCNLTVKNMCSAD";

    /**
     * Tests that protein can be instantiated with amino acids sequences with and without whitespace
     */
    @Test public void testGetSequence()   {
        // Should be OK
        Protein protein = new Protein(GOOD);
        assertEquals("Should be correct amino acid sequence", GOOD, protein.getSequence());
        protein = new Protein(MULTILINE);
        assertEquals("Should be correct amino acid sequence without whitespace", SINGLELINE, protein.getSequence());
        // Should fail
        try {
            new Protein(BAD);
        }
        catch (Exception e)    {
            assertTrue("Should be IllegalArgumentException", e instanceof IllegalArgumentException);
        }
    }

    @Test public void testCrossReferences() {
        final String id = "test";
        Protein protein = new Protein(GOOD);
        Xref xref = protein.addCrossReference(new Xref(id));
        assertEquals(1, protein.getCrossReferences().size());
        assertNotNull(xref);
        assertEquals(id, xref.getIdentifier());
        protein.removeCrossReference(xref);
        assertEquals(0, protein.getCrossReferences().size());
    }

    /**
     * Tests the equals() method works as expected
     */
    @Test public void testEquals() {
        Protein original = new Protein(GOOD);
        Protein copy = (Protein)SerializationUtils.clone(original);
        // Original should equal itself
        assertEquals(original, original);
        // Original and copy should be equal
        assertEquals(original, copy);
        // Original and copy should not be equal
        Xref xref = original.addCrossReference(new Xref("A0A000_9ACTO"));
        assertFalse("Original and copy should not be equal", original.equals(copy));
        //  Original and copy should be equal again
        copy.addCrossReference((Xref)SerializationUtils.clone(xref));
        assertEquals(original, copy);
        // Try with locations
        Set<HmmerMatch.HmmerLocation> locations = new HashSet<HmmerMatch.HmmerLocation>();
        locations.add(new HmmerMatch.HmmerLocation(3, 107, 3.0, 3.7e-9, 1, 104, HmmerMatch.HmmerLocation.HmmBounds.N_TERMINAL_COMPLETE));
        Match match = original.addMatch(new HmmerMatch(new Signature("PF02310", "B12-binding"), 0.035, 3.7e-9, locations));
        assertFalse("Original and copy should not be equal", original.equals(copy));
        copy.addMatch((HmmerMatch)SerializationUtils.clone(match));
        assertEquals(original, copy);
    }

    /**
     * Tests the equals() method works as expected using protein.match.addLocation()
     * As of 14 August this does not work. There may be an issue with generics.
     * See FilteredHmmMatchTest for tests -- it works in that context but for some reason not via Protein. Why??
     * Seems to be to do with Set: equals() works OK, but perhaps hashCode() does not behave as expected?
     */
    @Ignore
    @Test public void testEqualsAddLocation() {
        Protein original = new Protein(GOOD);
        Protein copy     = (Protein)SerializationUtils.clone(original);
        HmmerMatch.HmmerLocation location       = new HmmerMatch.HmmerLocation(3, 107, 3.0, 3.7e-9, 1, 104, HmmerMatch.HmmerLocation.HmmBounds.N_TERMINAL_COMPLETE);
        Set<HmmerMatch.HmmerLocation> locations = new HashSet<HmmerMatch.HmmerLocation>(Arrays.asList(location));
        HmmerMatch match     = new HmmerMatch(new Signature("PF02310", "B12-binding"), 0.035, 3.7e-9, locations);
        HmmerMatch matchCopy = (HmmerMatch)SerializationUtils.clone(match);
        original.addMatch(match);
        copy.addMatch(matchCopy);
        // Locations look OK, but get warning about Locations type -- generics problem?
        // TODO: We can't do the following -- need to change declaration of Location class
        // TODO: to Location<T extends Match>? But how would we know in eg. HmmerLocation that Match class is
        // TODO: HmmerMatch or RawHmmMatch? Would need to parameterise HmmerLocation and other sub-classes of Location
        // Trouble is a protein can have different types of filtered match...
        //Set<HmmerMatch> matchesOriginal = original.getFilteredMatches();
        //Set<HmmerMatch> matchesCopy     = copy.getFilteredMatches();
        Set<Match> matchesOriginal = original.getMatches();
        Set<Match> matchesCopy     = copy.getMatches();
        // TODO: Whether we remove addLocation and removeLocation or not, we still need to solve generics warning here:
        Set<HmmerMatch.HmmerLocation> locationsOriginal = matchesOriginal.iterator().next().getLocations();
        Set<HmmerMatch.HmmerLocation> locationsCopy     = matchesCopy.iterator().next().getLocations();
        assertEquals("Locations should be equal", locationsOriginal, locationsCopy);
        assertEquals("Location hashcodes should be equal", locationsOriginal.hashCode(), locationsCopy.hashCode());
        assertTrue("Original locations should contain locations copy element", locationsOriginal.contains(locationsCopy.iterator().next()));
        // Matches not OK
        assertEquals("Matches hashcodes should be equal", matchesOriginal.hashCode(), matchesCopy.hashCode());
        assertEquals("Original matches should be equal", matchesOriginal, matchesOriginal);
        // TODO: Following fails -- did manual eyeball and found no differences!
        assertEquals("Original and matches copy should be equal", matchesOriginal, matchesCopy);        
        assertTrue("Original matches should contain original matches element", matchesOriginal.contains(matchesCopy.iterator().next()));
        assertTrue("Original matches should contain matches copy element", matchesOriginal.contains(matchesCopy.iterator().next()));
        assertEquals("Original and copy should be equal", original, copy);
    }


    /**
     * Tests that MD5 checksum can be calculated for the protein sequence
     */
    @Test public void testGetMd5()   {
        Protein ps = new Protein(GOOD);
        assertEquals("MD5 checksums should be same", GOOD_MD5, ps.getMd5());
    }

    @Test public void testRemoveMatch()    {
        Protein protein = new Protein(GOOD);
        Set<HmmerMatch.HmmerLocation> locations = new HashSet<HmmerMatch.HmmerLocation>();
        locations.add(new HmmerMatch.HmmerLocation(3, 107, 3.0, 3.7e-9, 1, 104, HmmerMatch.HmmerLocation.HmmBounds.N_TERMINAL_COMPLETE));
        Match match = protein.addMatch(new HmmerMatch(new Signature("PF00155"), 0.035, 4.3e-61, locations));
        assertEquals("Protein should have one match", 1, protein.getMatches().size());
        protein.removeMatch(match);
        assertEquals("Protein should have no matches", 0, protein.getMatches().size());
    }

    // Note: The following does not work perhaps because IllegalArgumentException is a runtime exception, and only
    //       checked exception such as IOException can be used with @Text(expected)
    /*
    @Test(expected=java.lang.IllegalArgumentException.class)
    public void testGetSequenceBad()   {
        Protein ps;
        ps = new Protein(BAD);
    }
    */

    @Test public void testXml() throws IOException, SAXException {
        super.testSupportsMarshalling(Protein.class);
        super.testXmlRoundTrip();
    }

    // TODO: Re-enable when JPA works OK with FilteredMatch interface
    @Test
    @Ignore ("Fails due to problems with the structure and JPA-annotation of Match and Location.")
    public void testJpa() {
        super.testJpaXmlObjects(new ObjectRetriever<Protein>(){
            public Protein getObjectByPrimaryKey(GenericDAO<Protein, Long> dao, Long primaryKey) {
                return dao.readDeep(primaryKey, "rawMatches", "filteredMatches");
            }

            public Long getPrimaryKey(Protein protein) {
                return protein.getId();
            }
        });
    }

}
