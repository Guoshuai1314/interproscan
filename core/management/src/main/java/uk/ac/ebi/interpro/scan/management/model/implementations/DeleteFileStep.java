package uk.ac.ebi.interpro.scan.management.model.implementations;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;

import java.io.File;

/**
 * Simply deletes a File located at the path provided.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0
 */
public class DeleteFileStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(DeleteFileStep.class);

    private String filePathTemplate;

    @Required
    public void setFilePathTemplate(String filePathTemplate) {
        this.filePathTemplate = filePathTemplate;
    }

    /**
     * This method is called to execute the action that the StepInstance must perform.
     * <p/>
     * If an error occurs that cannot be immediately recovered from, the implementation
     * of this method MUST throw a suitable Exception, as the call
     * to execute is performed within a transaction with the reply to the JMSBroker.
     *
     * @param stepInstance containing the parameters for executing.
     * @throws Exception could be anything thrown by the execute method.
     */
    @Override
    public void execute(StepInstance stepInstance) throws Exception {
        final String filePathName = stepInstance.filterFileNameProteinBounds(filePathTemplate);
        File file = new File(filePathName);
        if (! file.delete()){
            throw new IllegalStateException ("Unable to delete the file located at " + filePathName);
        }
    }
}
