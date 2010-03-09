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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.Serializable;
import java.util.*;

/**
 * Represents a signature match on a protein sequence. 
 *
 * @author  Antony Quinn
 * @author  Phil Jones
 * @version $Id$
 * @since   1.0
 */

@Entity
@Inheritance (strategy=InheritanceType.TABLE_PER_CLASS)
@XmlType(name="MatchType", propOrder={"signature", "locations"})
public abstract class Match<T extends Location> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator="MATCH_IDGEN")
    @TableGenerator(name="MATCH_IDGEN", table="KEYGEN", pkColumnValue="match", initialValue = 0, allocationSize = 50)
    private Long id;

    @ManyToOne(cascade=CascadeType.REFRESH, optional = false)
    @ForeignKey(name="fk_protein")
    private Protein protein;

    @ManyToOne(cascade= CascadeType.PERSIST, optional = false)
    @ForeignKey(name="fk_signature")
    private Signature signature;

    @OneToMany(cascade = CascadeType.PERSIST, targetEntity = Location.class)
    private Set<T> locations = new LinkedHashSet<T>();
   
    protected Match() {}

    protected Match(Signature signature, Set<T> locations)  {
        setLocations(locations);
        setSignature(signature);
    }

    @XmlTransient                                                                          
    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    @XmlTransient
    public Protein getProtein()  {
        return protein;
    }

    void setProtein(Protein protein) {
        this.protein = protein;

    }

    @XmlElement(required=true)
    public Signature getSignature() {
        return signature;
    }

    private void setSignature(Signature signature) {
        this.signature = signature;
    }

    @Transient    
    @XmlJavaTypeAdapter(Location.LocationAdapter.class)
    public Set<T> getLocations() {
        return Collections.unmodifiableSet(locations);
    }

    // Private so can only be set by JAXB, Hibernate ...etc via reflection
    protected void setLocations(final Set<T> locations) {
        if (locations.isEmpty())    {
            throw new IllegalArgumentException("There must be at least one location for the match");
        }
        for (T location : locations)    {
            this.locations.add(location);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Match))
            return false;
        final Match m = (Match) o;
        return new EqualsBuilder()
                .append(locations, m.locations)
                .append(signature, m.signature)
                .isEquals();
    }

    @Override public int hashCode() {
        return new HashCodeBuilder(19, 51)
                .append(locations)
                .append(signature)
                .toHashCode();
    }

    @Override public String toString()  {
        return ToStringBuilder.reflectionToString(this);
    }

    // TODO: Now we're using abstract classes instead of interfaces, can we get rid of the following?
    /**
     *  Ensure sub-classes of Match are represented correctly in XML.
     *
     * @author  Antony Quinn
     */
    @XmlTransient
    static final class MatchAdapter extends XmlAdapter<MatchesType, Set<Match>> {

        /** Map Java to XML type */
        @Override public MatchesType marshal(Set<Match> matches) {
            Set<Hmmer2Match> hmmer2Matches = new LinkedHashSet<Hmmer2Match>();
            Set<Hmmer3Match> hmmer3Matches = new LinkedHashSet<Hmmer3Match>();
            Set<FingerPrintsMatch> fingerPrintsMatches = new LinkedHashSet<FingerPrintsMatch>();
            Set<BlastProDomMatch> proDomMatches      = new LinkedHashSet<BlastProDomMatch>();
            Set<PatternScanMatch> patternScanMatches = new LinkedHashSet<PatternScanMatch>();
            Set<ProfileScanMatch> profileScanMatches = new LinkedHashSet<ProfileScanMatch>();
            for (Match m : matches) {
                if (m instanceof Hmmer2Match) {
                    hmmer2Matches.add((Hmmer2Match)m);
                }
                else if (m instanceof Hmmer3Match) {
                    hmmer3Matches.add((Hmmer3Match)m);
                }
                else if (m instanceof FingerPrintsMatch) {
                    fingerPrintsMatches.add((FingerPrintsMatch)m);
                }
                else if (m instanceof BlastProDomMatch) {
                    proDomMatches.add((BlastProDomMatch)m);
                }
                else if (m instanceof PatternScanMatch) {
                    patternScanMatches.add((PatternScanMatch)m);
                }
                else if (m instanceof ProfileScanMatch) {
                    profileScanMatches.add((ProfileScanMatch)m);
                }
                else    {
                    throw new IllegalArgumentException("Unrecognised Match class: " + m);
                }
            }
            return new MatchesType(hmmer2Matches, hmmer3Matches, fingerPrintsMatches, proDomMatches,
                    patternScanMatches, profileScanMatches);
        }

        /** Map XML type to Java */
        @Override public Set<Match> unmarshal(MatchesType matchTypes) {
            Set<Match> matches = new HashSet<Match>();
            matches.addAll(matchTypes.getHmmer2Matches());
            matches.addAll(matchTypes.getHmmer3Matches());
            matches.addAll(matchTypes.getFingerPrintsMatches());
            matches.addAll(matchTypes.getProDomMatches());
            matches.addAll(matchTypes.getPatternScanMatches());
            matches.addAll(matchTypes.getProfileScanMatches());
            return matches;
        }

    }

    /**
     * Helper class for MatchAdapter
     */
    private final static class MatchesType {

        @XmlElement(name = "hmmer2-match")
        private final Set<Hmmer2Match> hmmer2Matches;

        @XmlElement(name = "hmmer3-match")
        private final Set<Hmmer3Match> hmmer3Matches;        

        @XmlElement(name = "fingerprints-match")
        private final Set<FingerPrintsMatch> fingerPrintsMatches;

        @XmlElement(name = "blastprodom-match")
        private final Set<BlastProDomMatch> proDomMatches;

        @XmlElement(name = "patternscan-match")
        private final Set<PatternScanMatch> patternScanMatches;

        @XmlElement(name = "profilescan-match")
        private final Set<ProfileScanMatch> profileScanMatches;

        private MatchesType() {
            hmmer2Matches       = null;
            hmmer3Matches       = null;
            fingerPrintsMatches = null;
            proDomMatches       = null;
            patternScanMatches  = null;
            profileScanMatches  = null;
        }

        public MatchesType(Set<Hmmer2Match> hmmer2Matches,
                           Set<Hmmer3Match> hmmer3Matches,
                           Set<FingerPrintsMatch> fingerPrintsMatches,
                           Set<BlastProDomMatch> proDomMatches,
                           Set<PatternScanMatch> patternScanMatches,
                           Set<ProfileScanMatch> profileScanMatches) {
            this.hmmer2Matches       = hmmer2Matches;
            this.hmmer3Matches       = hmmer3Matches;
            this.fingerPrintsMatches = fingerPrintsMatches;
            this.proDomMatches       = proDomMatches;
            this.patternScanMatches  = patternScanMatches;
            this.profileScanMatches  = profileScanMatches;
        }

        public Set<Hmmer2Match> getHmmer2Matches() {
            return (hmmer2Matches == null ? Collections.<Hmmer2Match>emptySet() : hmmer2Matches);
        }

        public Set<Hmmer3Match> getHmmer3Matches() {
            return (hmmer3Matches == null ? Collections.<Hmmer3Match>emptySet() : hmmer3Matches);
        }        

        public Set<FingerPrintsMatch> getFingerPrintsMatches() {
            return (fingerPrintsMatches == null ? Collections.<FingerPrintsMatch>emptySet() : fingerPrintsMatches);
        }

        public Set<BlastProDomMatch> getPatternScanMatches() {
            return (proDomMatches == null ? Collections.<BlastProDomMatch>emptySet() : proDomMatches);
        }

        public Set<PatternScanMatch> getProDomMatches() {
            return (patternScanMatches == null ? Collections.<PatternScanMatch>emptySet() : patternScanMatches);
        }

        public Set<ProfileScanMatch> getProfileScanMatches() {
            return (profileScanMatches == null ? Collections.<ProfileScanMatch>emptySet() : profileScanMatches);
        }

    }

}
