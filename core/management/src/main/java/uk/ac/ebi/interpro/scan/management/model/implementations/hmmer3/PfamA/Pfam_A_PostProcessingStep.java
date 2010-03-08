package uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.PfamA;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.business.postprocessing.pfam_A.PfamHMMER3PostProcessing;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.model.SignatureLibrary;
import uk.ac.ebi.interpro.scan.model.raw.PfamHmmer3RawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;
import uk.ac.ebi.interpro.scan.persistence.PfamFilteredMatchDAO;
import uk.ac.ebi.interpro.scan.persistence.raw.PfamHmmer3RawMatchDAO;

import java.util.Map;

/**
 * This class performs post-processing (including data persistence)
 * for Pfam A.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class Pfam_A_PostProcessingStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(Pfam_A_PostProcessingStep.class);

    private PfamHMMER3PostProcessing postProcessor;

    private SignatureLibrary signatureLibrary;

    private String signatureLibraryRelease;

    private PfamHmmer3RawMatchDAO rawMatchDAO;

    private PfamFilteredMatchDAO filteredMatchDAO;

    @Required
    public void setSignatureLibrary(SignatureLibrary signatureLibrary) {
        this.signatureLibrary = signatureLibrary;
    }

    @Required
    public void setSignatureLibraryRelease(String signatureLibraryRelease) {
        this.signatureLibraryRelease = signatureLibraryRelease;
    }

    public PfamHMMER3PostProcessing getPostProcessor() {
        return postProcessor;
    }

    @Required
    public void setPostProcessor(PfamHMMER3PostProcessing postProcessor) {
        this.postProcessor = postProcessor;
    }

    public SignatureLibrary getSignatureLibrary() {
        return signatureLibrary;
    }

    public String getSignatureLibraryRelease() {
        return signatureLibraryRelease;
    }

    @Required
    public void setRawMatchDAO(PfamHmmer3RawMatchDAO rawMatchDAO) {
        this.rawMatchDAO = rawMatchDAO;
    }

    @Required
    public void setFilteredMatchDAO(PfamFilteredMatchDAO filteredMatchDAO) {
        this.filteredMatchDAO = filteredMatchDAO;
    }

    /**
     * This method is called to execute the action that the StepInstance must perform.
     *
     * @param stepInstance containing the parameters for executing.
     * @param temporaryFileDirectory
     * @throws Exception could be anything thrown by the execute method.
     */
    @Override
    public void execute(StepInstance stepInstance, String temporaryFileDirectory) throws InterruptedException {
        Thread.sleep(2000);  // Have a snooze to allow NFS to catch up.
        // Retrieve raw results for protein range.
        Map<String, RawProtein<PfamHmmer3RawMatch>> rawMatches = rawMatchDAO.getRawMatchesForProteinIdsInRange(
                Long.toString(stepInstance.getBottomProtein()),
                Long.toString(stepInstance.getTopProtein()),
                getSignatureLibraryRelease()
        );

        // Post process
        Map<String, RawProtein<PfamHmmer3RawMatch>> filteredMatches = getPostProcessor().process(rawMatches);
        filteredMatchDAO.persistFilteredMatches(filteredMatches.values());
    }

    private int countMatches(Map<String, RawProtein<PfamHmmer3RawMatch>> matches) {
        int count = 0;
        for (RawProtein<PfamHmmer3RawMatch> protein : matches.values()){
            if (protein.getMatches() != null) count += protein.getMatches().size();
        }
        return count;
    }
}