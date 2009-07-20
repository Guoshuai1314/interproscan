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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Location(s) of match on protein sequence
 *
 * @author  Antony Quinn
 * @version $Id$
 * @since   1.0
 */
@XmlType(name="FingerPrintsLocationType", propOrder={"start", "end"})
@Entity
public class FingerPrintsLocation
        extends AbstractLocation
        implements Location {

    @Column (nullable = false)
    private double pvalue;

    /**
     * protected no-arg constructor required by JPA - DO NOT USE DIRECTLY.
     */
    protected FingerPrintsLocation() {}

    public FingerPrintsLocation(int start, int end, double pvalue) {
        super(start, end);
        this.pvalue = pvalue;
    }

    @XmlAttribute(name="pvalue", required=true)
    public double getPvalue() {
        return pvalue;
    }

    private void setPvalue(double pvalue) {
        this.pvalue = pvalue;
    }

    // TODO: Figure out which class to use (FingerPrintsMatch replaced by RawFingerPrintsMatch and FilteredFingerPrintsMatch)
    //@ManyToOne(targetEntity = FingerPrintsMatch.class)
    @XmlTransient
    @Override public Match getMatch() {
        return super.getMatch();
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FingerPrintsLocation))
            return false;
        final FingerPrintsLocation f = (FingerPrintsLocation) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(pvalue, f.pvalue)
                .isEquals();
    }

    @Override public int hashCode() {
        return new HashCodeBuilder(19, 61)
                .appendSuper(super.hashCode())
                .append(pvalue)
                .toHashCode();
    }

    @Override public String toString()  {
        return ToStringBuilder.reflectionToString(this);
    }

}