package uk.ac.ebi.interpro.scan.model.raw;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.io.Serializable;

/**
 * Stores raw matches associated with a protein sequence identifier.
 *
 * We cannot associate raw matches with a {@link uk.ac.ebi.interpro.scan.model.Protein} when
 * parsing output files because we do not have the protein sequence and cannot therefore create a Protein
 * object. We must therefore use this class when parsing because all we have is a protein identifier. 
 *
 * Note: Not stored in database, just returned by DAO as a convenience class.
 *
 * Type T is the type of RawMatch object that this instance references.
 *
 * @author  Antony Quinn
 * @author Phil Jones
 * @version $Id$
 */
public final class RawProtein <T extends RawMatch> implements Serializable {

    private final String proteinIdentifier;
    private final Collection<T> matches = new HashSet<T>();

    private RawProtein() {
        this.proteinIdentifier = null;
    }

    public RawProtein(String proteinIdentifier) {
        this.proteinIdentifier = proteinIdentifier;
    }

    public String getProteinIdentifier() {
        return proteinIdentifier;
    }

    public void addMatch(T match)  {
        matches.add(match);
    }    

    public Collection<T> getMatches() {
        return Collections.unmodifiableCollection(matches);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RawProtein))
            return false;
        final RawProtein s = (RawProtein) o;
        return new EqualsBuilder()
                .append(proteinIdentifier, s.proteinIdentifier)
                .append(matches, s.matches)
                .isEquals();
    }

    @Override public int hashCode() {
        return new HashCodeBuilder(21, 53)
                .append(proteinIdentifier)
                .append(matches)
                .toHashCode();
    }

    @Override public String toString()  {
        return ToStringBuilder.reflectionToString(this);
    }

}
