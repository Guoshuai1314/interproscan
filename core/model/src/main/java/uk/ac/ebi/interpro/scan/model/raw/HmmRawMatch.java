package uk.ac.ebi.interpro.scan.model.raw;

import javax.persistence.Column;
import javax.persistence.InheritanceType;
import javax.persistence.Inheritance;
import javax.persistence.Entity;

/**
 * TODO: Add class description
 *
 * @author Antony Quinn
 * @version $Id$
 * @since 1.0
 */
@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class HmmRawMatch extends RawMatch {

    // TODO: evalue and score can be calculated -- do we need to store? Needs testing
    //@Column(name="EVALUE",nullable = false, updatable = false)
    protected double evalue;

    //@Column(name="SCORE",nullable = false, updatable = false)
    protected double score;
    //@Column(name="HMM_START")
    protected long hmmStart;
    //@Column(name="HMM_END")
    protected long hmmEnd;
    //@Column(name="HMM_BOUNDS")
    protected String hmmBounds;
    //@Column(name="LOCATION_EVALUE")
    protected double locationEvalue;
    //@Column(name="SEQ_SCORE")
   protected double locationScore;
    //@Column(name="ALIGNMENT")
   protected String alignment;   // CIGAR format

    public HmmRawMatch() { }
    public HmmRawMatch(String seqIdentifier, String model,String dbname,String dbversion, String generator, long start, long end) {
        super(seqIdentifier,model,dbname,dbversion,generator,start,end);
    }
    public HmmRawMatch(String model) {
        super(model);
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public double getEvalue() {
        return evalue;
    }

    public void setEvalue(double evalue) {
        this.evalue = evalue;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public long getHmmStart() {
        return hmmStart;
    }

    public void setHmmStart(long hmmStart) {
        this.hmmStart = hmmStart;
    }

    public long getHmmEnd() {
        return hmmEnd;
    }

    public void setHmmEnd(long hmmEnd) {
        this.hmmEnd = hmmEnd;
    }

    public String getHmmBounds() {
        return hmmBounds;
    }

    public void setHmmBounds(String hmmBounds) {
        this.hmmBounds = hmmBounds;
    }

    public double getLocationEvalue() {
        return locationEvalue;
    }

    public void setLocationEvalue(double locationEvalue) {
        this.locationEvalue = locationEvalue;
    }

    public double getLocationScore() {
        return locationScore;
    }

    public void setLocationScore(double locationScore) {
        this.locationScore = locationScore;
    }

}
