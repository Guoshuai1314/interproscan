package uk.ac.ebi.interpro.scan.parser.matchparser.hmmer3;

import uk.ac.ebi.interpro.scan.parser.matchparser.hmmer3.parsemodel.HmmsearchOutputMethod;
import uk.ac.ebi.interpro.scan.model.raw.RawSequenceIdentifier;

import java.util.Map;
import java.io.IOException;

/**
 * TODO: Description
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public interface Hmmer3ParserSupport {


    /**
     * Implemented for specific member databases.  Different databases use different specific model classes
     * and may need to filter the matches at this point, based upon different criteria.
     *
     * This responsibility is delegated to the sub-class.
     * @param methodMatches which contains a localised (parsing-only) data model of the information in
     * HMMER 3 hmmsearch output.
     * @param rawResults being the Map of protein accessions to RawSequenceIdentifiers
     * that the raw results should be added to.
     * @throws java.io.IOException
     */
    void addMatch(HmmsearchOutputMethod methodMatches, Map<String, RawSequenceIdentifier> rawResults)
            throws IOException;

    /**
     * For maximum efficiency, specific implementations can choose to ignore the
     * alignment section, in which case this method should return false.
     *
     * Implementations for member databases such as Gene3D should return true from this method.
     * @return boolean indicating if the alignment section should be parsed.
     */
    boolean parseAlignments();
}
