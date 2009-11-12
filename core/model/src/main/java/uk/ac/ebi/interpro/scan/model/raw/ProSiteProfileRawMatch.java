package uk.ac.ebi.interpro.scan.model.raw;

import javax.persistence.Entity;

/**
 * TODO: Add class description
 *
 * @author  Antony Quinn
 * @version $Id$
 */
@Entity
public class ProSiteProfileRawMatch extends ProfileScanRawMatch {

    protected ProSiteProfileRawMatch() { }

    public ProSiteProfileRawMatch(String sequenceIdentifier, String model,
                                  String signatureLibraryName, String signatureLibraryRelease,
                                  long locationStart, long locationEnd,
                                  double score, String generator) {
        super(sequenceIdentifier, model, signatureLibraryName, signatureLibraryRelease, locationStart, locationEnd, score, generator);
    }
}