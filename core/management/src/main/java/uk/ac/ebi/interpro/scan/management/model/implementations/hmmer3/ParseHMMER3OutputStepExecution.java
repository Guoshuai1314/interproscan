package uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3;

import uk.ac.ebi.interpro.scan.management.model.StepExecution;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;
import uk.ac.ebi.interpro.scan.persistence.DAOManager;

import java.io.Serializable;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: pjones
 * Date: 23-Nov-2009
 * Time: 15:36:48
 */
public class ParseHMMER3OutputStepExecution extends StepExecution<ParseHMMER3OutputStepInstance> implements Serializable {

        private Set<RawProtein> parsedResults;

        public Set<RawProtein> getParsedResults() {
            return parsedResults;
        }

        protected ParseHMMER3OutputStepExecution(UUID id, ParseHMMER3OutputStepInstance stepInstance) {
            super(id, stepInstance);
        }

        /**
         * This method is called to execute the action that the StepExecution must perform.
         * This method should typically perform its activity in a try / catch / finally block
         * that sets the state of the step execution appropriately.
         * <p/>
         * Note that the implementation DOES have access to the protected stepInstance,
         * and from their to the protected Step, to allow it to access parameters for execution.
         * @param daoManager
         */
        @Override
        public void execute(DAOManager daoManager) {

            this.setToRun();
            InputStream is = null;
            try{
                if (daoManager == null){
                    throw new IllegalStateException ("The Hmmer3 Output Parser cannot run without a DAOManager.");
                }
                if (daoManager.getRawMatchDAO() == null){
                    throw new IllegalStateException ("The DAOManager is present, but does not contain a RawMatchDAO.");
                }
                is = new FileInputStream(this.getStepInstance().getHmmerOutputFilePath());
                Set<RawProtein> parsedResults = this.getStepInstance().getStep().getParser().parse(is);
                // TODO - Need to get the RawMatchDAO from the object calling execute - cannot be passed over the wire.

                daoManager.getRawMatchDAO().insertRawSequenceIdentifiers(parsedResults);
                this.completeSuccessfully();
            } catch (Exception e) {
                this.fail();
                System.out.println("Doh.  Hmmer output parsing failed.");
                e.printStackTrace();
                LOGGER.error ("Doh." , e);
            }
            finally {
                if (is != null){
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOGGER.error ("Duh - parsed OK, but can't close the input stream?" , e);
                    }
                }
            }
        }
    }
