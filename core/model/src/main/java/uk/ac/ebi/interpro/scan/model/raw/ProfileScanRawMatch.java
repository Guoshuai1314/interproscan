package uk.ac.ebi.interpro.scan.model.raw;

import javax.persistence.Entity;
import javax.persistence.Column;

/**
 * TODO: Add class description
 *
 * @author  Antony Quinn
 * @version $Id$
 */
@Entity
public abstract class ProfileScanRawMatch extends RawMatch {

    @Column
    private double score; // location.score

    protected ProfileScanRawMatch() { }

    protected ProfileScanRawMatch(String sequenceIdentifier, String model,
                                  String signatureLibraryName, String signatureLibraryRelease,
                                  int locationStart, int locationEnd,
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
    
}
