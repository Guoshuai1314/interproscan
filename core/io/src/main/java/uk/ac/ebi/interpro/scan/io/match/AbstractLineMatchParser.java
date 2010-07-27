package uk.ac.ebi.interpro.scan.io.match;

import uk.ac.ebi.interpro.scan.model.SignatureLibrary;
import uk.ac.ebi.interpro.scan.model.raw.RawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This implementation parses match data on a line-by-line basis, and can be used to parse,
 * for example, ProDom and Phobius results.
 *
 * @author Antony Quinn
 * @author Phil Jones
 * @version $Id$
 */
abstract class AbstractLineMatchParser<T extends RawMatch> implements MatchParser<T> {

    private final SignatureLibrary signatureLibrary;
    private final String signatureLibraryRelease;

    private AbstractLineMatchParser() {
        this.signatureLibrary = null;
        this.signatureLibraryRelease = null;
    }

    protected AbstractLineMatchParser(SignatureLibrary signatureLibrary, String signatureLibraryRelease) {
        this.signatureLibrary = signatureLibrary;
        this.signatureLibraryRelease = signatureLibraryRelease;
    }

    @Override
    public SignatureLibrary getSignatureLibrary() {
        return signatureLibrary;
    }

    @Override
    public String getSignatureLibraryRelease() {
        return signatureLibraryRelease;
    }

    @Override
    public Set<RawProtein<T>> parse(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("InputStream is null");
        }
        Map<String, RawProtein<T>> proteins = new HashMap<String, RawProtein<T>>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                T match = createMatch(signatureLibrary, signatureLibraryRelease, line);
                if (match != null) {
                    String id = match.getSequenceIdentifier();
                    RawProtein<T> protein;
                    if (proteins.containsKey(id)) {
                        protein = proteins.get(id);
                    } else {
                        protein = new RawProtein<T>(id);
                        proteins.put(id, new RawProtein<T>(id));
                    }
                    protein.addMatch(match);
                }
            }
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
        return new HashSet<RawProtein<T>>(proteins.values());
    }

    /**
     * Returns {@link uk.ac.ebi.interpro.scan.model.raw.RawMatch} instance using values from parameters.
     *
     * @param signatureLibrary
     * @param signatureLibraryRelease Corresponds to {@link uk.ac.ebi.interpro.scan.model.SignatureLibraryRelease#getVersion()}
     * @param line                    Line read from input file.   @return {@link uk.ac.ebi.interpro.scan.model.raw.RawMatch} instance using values from parameters
     */
    protected abstract T createMatch(SignatureLibrary signatureLibrary,
                                     String signatureLibraryRelease,
                                     String line);

}
