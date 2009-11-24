package uk.ac.ebi.interpro.scan.jms.master;

import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.business.sequence.fasta.LoadFastaFile;
import uk.ac.ebi.interpro.scan.jms.SessionHandler;
import uk.ac.ebi.interpro.scan.management.model.*;
import uk.ac.ebi.interpro.scan.management.model.implementations.WriteFastaFileStep;
import uk.ac.ebi.interpro.scan.management.model.implementations.WriteFastaFileStepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.RunHmmer3Step;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.RunHmmer3StepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.ParseHMMER3OutputStep;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.ParseHMMER3OutputStepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.PfamA.Pfam_A_PostProcessingStepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.hmmer3.PfamA.Pfam_A_PostProcessingStep;
import uk.ac.ebi.interpro.scan.model.Protein;
import uk.ac.ebi.interpro.scan.persistence.ProteinDAO;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import java.util.*;

/**
 * Pretending to be the InterProScan master application.
 *
 * @author Phil Jones
 * @version $Id: TestMaster.java,v 1.4 2009/10/28 15:04:00 pjones Exp $
 * @since 1.0
 */
public class InterProScanMaster implements Master {

    private SessionHandler sessionHandler;

    private String jobSubmissionQueueName;

    private ResponseMonitor responseMonitor;

    private List<Job> jobs;

    private volatile Map<String, StepInstance> stepInstances = new HashMap<String, StepInstance>();

    private volatile Map<String, StepExecution> stepExecutions = new HashMap<String, StepExecution>();

    private String managementRequestTopicName;

    private LoadFastaFile loader;

    private ProteinDAO proteinDAO;

    private MessageProducer producer;

    @Required
    public void setLoader(LoadFastaFile loader) {
        this.loader = loader;
    }

    /**
     * Sets the SessionHandler.  This looks after connecting to the
     * Broker and allowing messages to be put on the queue / taken off the queue.
     * @param sessionHandler  looks after connecting to the
     * Broker and allowing messages to be put on the queue / taken off the queue.
     */
    @Required
    public void setSessionHandler(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    /**
     * Sets the task submission queue name.  This is the queue that new
     * jobs are placed on to, prior to be pushed on to the requestQueue
     * from where they are taken by a worker node.
     * @param jobSubmissionQueueName
     */
    @Required
    public void setJobSubmissionQueueName(String jobSubmissionQueueName) {
        this.jobSubmissionQueueName = jobSubmissionQueueName;
    }

    /**
     * Sets the name of the topic to which Worker management requests
     * should be sent, for multicast to all of the Worker clients.
     *
     * @param managementRequestTopicName the name of the topic to which Worker management requests
     *                                   should be sent, for multicast to all of the Worker clients.
     */
    @Override
    public void setManagementRequestTopicName(String managementRequestTopicName) {
        this.managementRequestTopicName = managementRequestTopicName;
    }

    /**
     * Sets the ResponseMonitor which will handle any responses from
     * the Worker nodes.
     * @param responseMonitor which will handle any responses from
     * the Worker nodes.
     */
    @Required
    public void setResponseMonitor(ResponseMonitor responseMonitor){
        this.responseMonitor = responseMonitor;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    @Required
    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public ProteinDAO getProteinDAO() {
        return proteinDAO;
    }

    @Required
    public void setProteinDAO(ProteinDAO proteinDAO) {
        this.proteinDAO = proteinDAO;
    }

    /**
     * Run the Master Application.
     */
    public void start(){
        try {
            // Start the response monitor thread
            Thread responseMonitorThread = new Thread(responseMonitor);
            responseMonitor.setStepExecutionMap(stepExecutions);
            responseMonitorThread.start();
            // Initialise the sessionHandler for the master thread
            sessionHandler.init();

            producer = sessionHandler.getMessageProducer(jobSubmissionQueueName);
            buildStepInstancesTheStupidWay();
            System.out.println("Returned from building step instances method.");
            while(true){
//                sendMessage(jobSubmissionQueueName, "Message number " + i);  // Send a message every second or so.
                for (StepInstance stepInstance : stepInstances.values()){
                    if (stepInstance.canBeSubmitted()){
                        StepExecution stepExecution = stepInstance.createStepExecution();
                        stepExecutions.put(stepExecution.getId(), stepExecution);
                        // TODO - for the moment, just sending to the default job submission queue.
                        System.out.println("Submitting "+ stepExecution.getStepInstance().getStep().getStepDescription());
                        sendMessage(jobSubmissionQueueName, stepExecution);
                    }
                }
                Thread.sleep(2000);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            if (sessionHandler != null){
                try {
                    sessionHandler.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void buildStepInstancesTheStupidWay() {
        // TODO - Note that this method is just a HACK to create a structure of steps that
        // TODO - can be run for the demonstration - the mechanism to build a real set of
        // TODO - steps, following the addition of new proteins has yet to be written.

        Job job = jobs.iterator().next();

        // Load some proteins into the database.
        loader.loadSequences();

        // Then retrieve the first 99.
        for (long bottomProteinId = 1; bottomProteinId <= 5000l; bottomProteinId+=100l){
            final long topProteinId = bottomProteinId + 99l;
            WriteFastaFileStepInstance fastaStepInstance = null;
            RunHmmer3StepInstance hmmer3StepInstance = null;
            ParseHMMER3OutputStepInstance hmmer3ParserStepInstance = null;
            Pfam_A_PostProcessingStepInstance ppStepInstance = null;

            // Create the fastafilestep
            for (Step step : job.getSteps()){
                if (step instanceof WriteFastaFileStep){
                    List<Protein> proteins = proteinDAO.getProteinsBetweenIds(bottomProteinId, topProteinId);

                    System.out.println("Creating WriteFastaFileStepInstance for range "+ bottomProteinId + " - " + topProteinId);
                    fastaStepInstance = new WriteFastaFileStepInstance(
                            UUID.randomUUID(),
                            (WriteFastaFileStep)step,
                            proteins,
                            bottomProteinId,
                            topProteinId
                    );
                    stepInstances.put(fastaStepInstance.getId(), fastaStepInstance);
                }
            }
            // Create the RunHmmer3Step
            for (Step step : job.getSteps()){
                if (step instanceof RunHmmer3Step){
                    System.out.println("Creating RunHmmer3StepInstance for range "+ bottomProteinId + " - " + topProteinId);
                    hmmer3StepInstance = new RunHmmer3StepInstance(
                            UUID.randomUUID(),
                            (RunHmmer3Step)step,
                            bottomProteinId,
                            topProteinId
                    );
                    hmmer3StepInstance.addDependentStepInstance(fastaStepInstance);
                    stepInstances.put(hmmer3StepInstance.getId(), hmmer3StepInstance);
                }
            }

            // Create the ParseHmmer3Output step
            for (Step step : job.getSteps()){
                if (step instanceof ParseHMMER3OutputStep){
                    System.out.println("Creating ParseHMMER3OutputStepInstance for range "+ bottomProteinId + " - " + topProteinId);
                    hmmer3ParserStepInstance = new ParseHMMER3OutputStepInstance(
                            UUID.randomUUID(),
                            (ParseHMMER3OutputStep)step,
                            bottomProteinId,
                            topProteinId
                    );
                    hmmer3ParserStepInstance.addDependentStepInstance(hmmer3StepInstance);
                    stepInstances.put(hmmer3ParserStepInstance.getId(), hmmer3ParserStepInstance);
                }
            }

            // Create the Pfam_A_Post_Processing step
            for (Step step : job.getSteps()){
                if (step instanceof Pfam_A_PostProcessingStep){
                    System.out.println("Creating Pfam_A_PostProcessingStepInstance for range "+ bottomProteinId + " - " + topProteinId);
                    ppStepInstance = new Pfam_A_PostProcessingStepInstance(
                            UUID.randomUUID(),
                            (Pfam_A_PostProcessingStep)step,
                            bottomProteinId,
                            topProteinId
                    );
                    ppStepInstance.addDependentStepInstance(hmmer3ParserStepInstance);
                    stepInstances.put(ppStepInstance.getId(), ppStepInstance);
                }
            }
        }
        System.out.println("Built Collection of stepInstances");
    }

    /**
     * Just creates simple text messages to be sent to Worker nodes.
     * @param stepExecution being the StepExecution to send as a message
     * @throws JMSException in the event of a failure sending the message to the JMS Broker.
     */
    private void sendMessage(String destination, StepExecution stepExecution) throws JMSException {
        stepExecution.submit();
        ObjectMessage message = sessionHandler.createObjectMessage(stepExecution);
        if (message.getObject() == null){
            System.out.println("message.getObject() is null.  Throwing IllegalStateException.");
            throw new IllegalStateException ("The object message is empty:" + message.toString());
        }
        StepExecution retrievedStepExec = (StepExecution) message.getObject();
        if (! retrievedStepExec.equals(stepExecution)){
            System.out.println("message.getObject not equals stepExecution.  Throwing IllegalStateException.");
            throw new IllegalStateException("The StepExecution object in the message is not equal to the StepExecution object placed into the message.");
        }
        producer.send(message);
    }
}
