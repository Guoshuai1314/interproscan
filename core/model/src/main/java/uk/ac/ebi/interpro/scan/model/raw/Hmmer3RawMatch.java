package uk.ac.ebi.interpro.scan.model.raw;

import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.HmmBounds;
import uk.ac.ebi.interpro.scan.model.Hmmer3Match;
import uk.ac.ebi.interpro.scan.model.PersistenceConversion;

import javax.persistence.Entity;
import java.util.*;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * <a href="http://hmmer.janelia.org/">HMMER 3</a> raw match.
 *
 * @author  Antony Quinn
 * @version $Id$
 */
@Entity
public abstract class Hmmer3RawMatch extends HmmerRawMatch {

    private int envelopeStart;
    private int envelopeEnd;
    
    private double expectedAccuracy;

    private double fullSequenceBias;

    private double domainCeValue;
    private double domainIeValue;
    private double domainBias;

    protected Hmmer3RawMatch() { }

    protected Hmmer3RawMatch(String sequenceIdentifier, String model,
                             String signatureLibraryName, String signatureLibraryRelease,
                             int locationStart, int locationEnd,
                             double evalue, double score,
                             int hmmStart, int hmmEnd, String hmmBounds,
                             double locationScore,
                             int envelopeStart, int envelopeEnd,
                             double expectedAccuracy, double fullSequenceBias,
                             double domainCeValue, double domainIeValue, double domainBias) {
        super(sequenceIdentifier, model, signatureLibraryName, signatureLibraryRelease, locationStart, locationEnd,
              evalue, score, hmmStart, hmmEnd, hmmBounds, locationScore);
        setEnvelopeStart(envelopeStart);
        setEnvelopeEnd(envelopeEnd);
        setExpectedAccuracy(expectedAccuracy);
        setFullSequenceBias(fullSequenceBias);
        setDomainCeValue(domainCeValue);
        setDomainIeValue(domainIeValue);
        setDomainBias(domainBias);
    }

    public int getEnvelopeStart() {
        return envelopeStart;
    }

    private void setEnvelopeStart(int envelopeStart) {
        this.envelopeStart = envelopeStart;
    }

    public int getEnvelopeEnd() {
        return envelopeEnd;
    }

    private void setEnvelopeEnd(int envelopeEnd) {
        this.envelopeEnd = envelopeEnd;
    }

    public double getExpectedAccuracy() {
        return expectedAccuracy;
    }

    private void setExpectedAccuracy(double expectedAccuracy) {
        this.expectedAccuracy = expectedAccuracy;
    }

    public double getFullSequenceBias() {
        return fullSequenceBias;
    }

    private void setFullSequenceBias(double fullSequenceBias) {
        this.fullSequenceBias = fullSequenceBias;
    }

    public double getDomainCeValue() {
        return PersistenceConversion.get(domainCeValue);
    }

    private void setDomainCeValue(double domainCeValue) {
        this.domainCeValue = PersistenceConversion.set(domainCeValue);
    }

    public double getDomainIeValue() {
        return PersistenceConversion.get(domainIeValue);
    }

    private void setDomainIeValue(double domainIeValue) {
        this.domainIeValue = PersistenceConversion.set(domainIeValue);
    }

    public double getDomainBias() {
        return domainBias;
    }

    private void setDomainBias(double domainBias) {
        this.domainBias = domainBias;
    }

    // TODO: Add hashCode(), equals() ...etc

    // TODO: Generalise this to RawMatch
    public static Collection<Hmmer3Match> getMatches(Collection<? extends Hmmer3RawMatch> rawMatches,
                                                     Listener rawMatchListener)  {
        Collection<Hmmer3Match> matches = new HashSet<Hmmer3Match>();
        // Get a list of unique model IDs
        String signatureLibraryName = null, signatureLibraryRelease = null;
        Map<String, Set<Hmmer3RawMatch>> matchesByModel = new HashMap<String, Set<Hmmer3RawMatch>>();
        for (Hmmer3RawMatch m : rawMatches)   {
            // Get signature library name and release
            if (signatureLibraryName == null) {
                signatureLibraryName    = m.getSignatureLibraryName();
                signatureLibraryRelease = m.getSignatureLibraryRelease();
            }
            else if (!signatureLibraryName.equals(m.getSignatureLibraryName()) ||
                     !signatureLibraryRelease.equals(m.getSignatureLibraryRelease())) {
                throw new IllegalArgumentException ("Filtered matches are from different signature library versions " +
                                                    "(more than one library version found)");
            }
            // Get unique list of model IDs
            String modelId = m.getModel();
            if (matchesByModel.containsKey(modelId))    {
                matchesByModel.get(modelId).add(m);
            }
            else    {
                Set<Hmmer3RawMatch> set = new HashSet<Hmmer3RawMatch>();
                set.add(m);
                matchesByModel.put(modelId, set);
            }
        }
        // Find the location(s) for each match and create a Match instance
        for (String key : matchesByModel.keySet())  {
            Signature signature = rawMatchListener.getSignature(key, signatureLibraryName, signatureLibraryRelease);
            matches.add(getMatch(signature, key, matchesByModel));
        }
        // Next step would be to link this with protein...
        return matches;

    }

    private static Hmmer3Match getMatch(Signature signature, String modelId, Map<String, Set<Hmmer3RawMatch>> matchesByModel){
        Set<Hmmer3Match.Hmmer3Location> locations = new HashSet<Hmmer3Match.Hmmer3Location>();
        double score = 0, evalue = 0;
        for (Hmmer3RawMatch m : matchesByModel.get(modelId))   {
            // Score and evalue should be the same (repeated for each location)
            score  = m.getScore();
            evalue = m.getEvalue();
            locations.add(getLocation(m));
        }
        return new Hmmer3Match(signature, score, evalue, locations);
    }

    private static Hmmer3Match.Hmmer3Location getLocation(Hmmer3RawMatch m){
        return new Hmmer3Match.Hmmer3Location(
                m.getLocationStart(),
                m.getLocationEnd(),
                m.getLocationScore(),
                m.getDomainIeValue(),
                m.getHmmStart(),
                m.getHmmEnd(),
                HmmBounds.parseSymbol(m.getHmmBounds()),
                m.getEnvelopeStart(),
                m.getEnvelopeEnd()
        );
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Hmmer3RawMatch))
            return false;
        final Hmmer3RawMatch m = (Hmmer3RawMatch) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(envelopeStart, m.envelopeStart)
                .append(envelopeEnd, envelopeEnd)
                .append(expectedAccuracy, m.expectedAccuracy)
                .append(fullSequenceBias, m.fullSequenceBias)
                .append(domainCeValue, m.domainCeValue)
                .append(domainIeValue, m.domainIeValue)
                .append(domainBias, m.domainBias)
                .isEquals();
    }

    @Override public int hashCode() {
        return new HashCodeBuilder(53, 59)
                .appendSuper(super.hashCode())
                .append(envelopeStart)
                .append(envelopeEnd)
                .append(expectedAccuracy)
                .append(fullSequenceBias)
                .append(domainCeValue)
                .append(domainIeValue)
                .append(domainBias)
                .toHashCode();
    }

    @Override public String toString()  {
        return ToStringBuilder.reflectionToString(this);
    }
    
}