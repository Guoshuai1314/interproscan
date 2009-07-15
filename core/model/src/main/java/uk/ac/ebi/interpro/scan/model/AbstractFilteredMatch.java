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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a filtered protein match.
 *
 * @author  Antony Quinn
 * @version $Id: AbstractFilteredMatch.java,v 1.3 2009/07/10 13:24:41 aquinn Exp $
 * @since   1.0
 */
@XmlTransient
abstract class AbstractFilteredMatch<T extends Location>
        extends AbstractMatch<T>
        implements FilteredMatch<T>, Serializable {
    
    private Signature signature;

    AbstractFilteredMatch() {}

    public AbstractFilteredMatch(Signature signature)  {
        this.signature = signature;
    }

    @XmlElement(required=true)
    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    @XmlTransient
    public String getKey() {
        return signature.getKey();
    }

    /**
     *  Ensure sub-classes of AbstractFilteredMatch are represented correctly in XML.
     */
    @XmlTransient
    static final class FilteredMatchAdapter extends XmlAdapter<FilteredMatchesType, Set<FilteredMatch>> {

        // Adapt original Java construct to a type (MatchesType)
        // which we can easily map to the XML output we want
        @Override public FilteredMatchesType marshal(Set<FilteredMatch> matches) {
            Set<FilteredHmmMatch> hmmMatches = new LinkedHashSet<FilteredHmmMatch>();
            Set<FilteredFingerPrintsMatch> fingerPrintsMatches = new LinkedHashSet<FilteredFingerPrintsMatch>();
            for (FilteredMatch m : matches) {
                if (m instanceof FilteredHmmMatch) {
                    hmmMatches.add((FilteredHmmMatch)m);
                }
                else {
                    fingerPrintsMatches.add((FilteredFingerPrintsMatch)m);
                }
            }
            return new FilteredMatchesType(hmmMatches, fingerPrintsMatches);
        }

        // map XML type to Java
        @Override public Set<FilteredMatch> unmarshal(FilteredMatchesType matchTypes) {
            // TODO: Test unmarshal
            Set<FilteredMatch> matches = new HashSet<FilteredMatch>(matchTypes.getHmmMatches().size() + matchTypes.getFingerPrintsMatches().size());
            for (FilteredMatch m : matchTypes.getHmmMatches()) {
                matches.add(m);
            }
            for (FilteredMatch m : matchTypes.getFingerPrintsMatches()) {
                matches.add(m);
            }
            return matches;
        }

    }

    /**
     * Helper class for MatchAdapter
     */
    private final static class FilteredMatchesType {

        private Set<FilteredHmmMatch> hmmMatches;
        private Set<FilteredFingerPrintsMatch> fingerPrintsMatches;

        public FilteredMatchesType() {}

        public FilteredMatchesType(Set<FilteredHmmMatch> hmmMatches, Set<FilteredFingerPrintsMatch> fingerPrintsMatches) {
            this.hmmMatches          = hmmMatches;
            this.fingerPrintsMatches = fingerPrintsMatches;
        }

        @XmlElement(name = "hmm-match")
        public Set<FilteredHmmMatch> getHmmMatches() {
            return hmmMatches;
        }

        @XmlElement(name = "fingerprints-match")
        public Set<FilteredFingerPrintsMatch> getFingerPrintsMatches() {
            return fingerPrintsMatches;
        }

    }

}
