package uk.ac.ebi.interpro.scan.model.raw;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Entity;
import java.io.Serializable;

/**
 * TODO: Add class description
 *
 * @author  Antony Quinn
 * @version $Id$
 */
@Entity
public class ProDomRawMatch extends RawMatch implements Serializable {

    // TODO: No need for member DBs where algorithm is unique to them, eg. PRINTS, Panther

    //@Column(name="SCORE")
    private double score;   // location.score

    protected ProDomRawMatch() { }    

    public ProDomRawMatch(String sequenceIdentifier, String model,
                          String signatureLibraryName, String signatureLibraryRelease,
                          long locationStart, long locationEnd,
                          double score, String generator) {
        super(sequenceIdentifier, model, signatureLibraryName, signatureLibraryRelease, locationStart, locationEnd, generator);
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    private void setScore(double score) {
        this.score = score;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProDomRawMatch))
            return false;
        final ProDomRawMatch m = (ProDomRawMatch) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(score, m.score)
                .isEquals();
    }

    @Override public int hashCode() {
        return new HashCodeBuilder(11, 81)
                .appendSuper(super.hashCode())
                .append(score)
                .toHashCode();
    }    

}
