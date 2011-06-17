/*
 * Copyright 2011 the original author or authors.
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


import org.apache.commons.lang.SerializationUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link NucleotideSequence}
 *
 * @author  Antony Quinn
 * @version $Id$
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NucleotideSequenceTest extends AbstractTest<NucleotideSequence> {

    private static final Logger LOGGER = Logger.getLogger(NucleotideSequenceTest.class.getName());
    
    @Test
    public void testEquals() throws IOException, ParseException {
        // Original
        NucleotideSequence original = getPfamObject();
        // Copy
        NucleotideSequence copy = (NucleotideSequence) SerializationUtils.clone(original);
        // Should be equal
        assertEquals("Original and copy should be equal", original, copy);
        // Print
        if (LOGGER.isDebugEnabled())    {
            LOGGER.debug(original);
            LOGGER.debug(super.marshal(original));
        }
    }

    @Test
    public void testXml() throws IOException, SAXException {
        super.testSupportsMarshalling(NucleotideSequence.class);
        super.testXmlRoundTrip();
    }    

    private NucleotideSequence getPfamObject() {
        // Create ORF
        OpenReadingFrame orf = new OpenReadingFrame(3, 63, NucleotideSequenceStrand.SENSE);
        orf.addProtein(getPfamProtein());
        // Create nucleotide sequence
        NucleotideSequence ns = new NucleotideSequence("CCGGAAGTTATTCACATTTATATGCGGAACCTCATATAAAATGTGAAAAGAAGGAATGCATGGAATGAATATTGGACAAAAAGTACTGTTCGAACTTAAAA");
        ns.addCrossReference(new NucleotideSequenceXref("ENA", "AP009179", "AP009179.1"));
        ns.addOpenReadingFrame(orf);
        return ns;
    }

    private Protein getPfamProtein() {
        Protein p = new Protein("MDLSALRVEEVQNVINAMQKILECPICLELIKEPVSTKCDHIFCKFCMLKLLNQKKGPSQCPLCKNDI");
        Signature signature = new Signature("PF02310", "B12-binding");
        p.addCrossReference(new ProteinXref("A0A000_9ACTO"));
        Set<Hmmer3Match.Hmmer3Location> locations = new HashSet<Hmmer3Match.Hmmer3Location>();
        locations.add(new Hmmer3Match.Hmmer3Location(3, 107, 3.0, 3.7e-9, 1, 104, 121, 2, 108));
        p.addMatch(new Hmmer3Match(signature, 0.035, 3.7e-9, locations));
        return p;
    }

}
