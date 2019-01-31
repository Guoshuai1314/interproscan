package uk.ac.ebi.interpro.scan.management.model.implementations.stepInstanceCreation.proteinLoad;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.interpro.scan.business.sequence.SequenceLoadListener;
import uk.ac.ebi.interpro.scan.management.model.Job;
import uk.ac.ebi.interpro.scan.management.model.Jobs;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.stepInstanceCreation.AbstractStepInstanceCreator;
import uk.ac.ebi.interpro.scan.management.model.implementations.stepInstanceCreation.StepInstanceCreatingStep;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener implementation that creates all required StepInstances
 * for the range of proteins passed in.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0
 */
public class StepCreationSequenceLoadListener
        extends AbstractStepInstanceCreator
        implements SequenceLoadListener {

    private static final Logger LOGGER = Logger.getLogger(StepCreationSequenceLoadListener.class.getName());


    private Job completionJob;
    private Job prepareOutputJob;
    private Job matchLookupJob;
    private Job finaliseInitialStepsJob;
    private boolean initialSetupSteps;

    public void setCompletionJob(Job completionJob) {
        this.completionJob = completionJob;
    }

    public void setPrepareOutputJob(Job prepareOutputJob) {
        this.prepareOutputJob = prepareOutputJob;
    }

    public void setMatchLookupJob(Job matchLookupJob) {
        this.matchLookupJob = matchLookupJob;
    }

    public void setFinaliseInitialStepsJob(Job finaliseInitialStepsJob) {
        this.finaliseInitialStepsJob = finaliseInitialStepsJob;
    }

    public void setInitialSetupSteps(boolean initialSetupSteps) {
        this.initialSetupSteps = initialSetupSteps;
    }

    /**
     * Optional constructor for use by Spring - do not remove.
     */
    public StepCreationSequenceLoadListener() {
    }

    public StepCreationSequenceLoadListener(Jobs analysisJobs, Job completionJob, Job prepareOutputJob, Job matchLookupJob, Job finaliseInitialStepsJob, boolean initialSetupSteps,  Map<String, String> parameters) {
        this.parameters = parameters;
        this.jobs = analysisJobs;
        this.completionJob = completionJob;
        this.prepareOutputJob = prepareOutputJob;
        this.matchLookupJob = matchLookupJob;
        this.finaliseInitialStepsJob = finaliseInitialStepsJob;
        this.initialSetupSteps = initialSetupSteps;
    }

    public StepCreationSequenceLoadListener(Jobs analysisJobs,  Map<String, String> parameters) {
        this.parameters = parameters;
        this.jobs = analysisJobs;
        this.completionJob = null;
        this.prepareOutputJob = null;
        this.matchLookupJob = null;
        this.finaliseInitialStepsJob = null;
        this.initialSetupSteps = false;
    }

    @Override
    @Transactional
    public void sequencesLoaded(final Long bottomNewSequenceId, final Long topNewSequenceId,
                                final Long bottomPrecalculatedSequenceId, final Long topPrecalculatedSequenceId, boolean useMatchLookupService, List <Long> idsWithoutLookupHit) {
        try {

            // These two variables capture the FULL RANGE of proteins that have been inserted into the database,
            // irrespective of whether they are new or precalculated.
            // This allows the whole range to be included in the 'completion job'.
            //TODO - Check this is correct behaviour - the full range may include proteins not intended to be included?
            final Long bottomProteinId = min(bottomNewSequenceId, bottomPrecalculatedSequenceId);
            final Long topProteinId = max(topNewSequenceId, topPrecalculatedSequenceId);

            if (bottomProteinId == null || topProteinId == null) {
                LOGGER.debug("Appear to be no proteins being analysed in this process - but this may be a bug.");
                return;
            }

            final Map<Step, List<StepInstance>> stepToStepInstances = new HashMap<Step, List<StepInstance>>();

            final List<StepInstance> completionStepInstances = new ArrayList<StepInstance>();

            final List<StepInstance> prepareOutputStepInstances = new ArrayList<StepInstance>();

            Utilities.verboseLog("Range of protein database IDs for which analysis StepInstances need to be created: " + bottomNewSequenceId + " - " + topNewSequenceId);
            Utilities.verboseLog("Range of protein database IDs for which NO StepInstances need to be created: " + bottomPrecalculatedSequenceId + " - " + topPrecalculatedSequenceId);
            Utilities.verboseLog("Range of protein database IDs for which the COMPLETION StepInstances need to be created: " + bottomProteinId + " - " + topProteinId);
            int idsWithoutLookupHitSize = 0;
            if (idsWithoutLookupHit != null) {
                idsWithoutLookupHitSize = idsWithoutLookupHit.size();
                Utilities.verboseLog("Protein without Lookup Hit (" + idsWithoutLookupHit.size() + ") range: " + idsWithoutLookupHit.get(0) + " - "
                        + idsWithoutLookupHit.get(idsWithoutLookupHitSize -1));
            }else{
                Utilities.verboseLog("idsWithoutLookupHit is NULL");
            }
            int percentageOfProteinsinLookup = (topProteinId.intValue() - idsWithoutLookupHitSize) *  100 / topProteinId.intValue();
            Utilities.verboseLog("Lookup hits: " + percentageOfProteinsinLookup +  "% of the input sequences are in the Lookup Match Server");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Completion Job:" + completionJob);

                if (parameters != null) {
                    for (String key : parameters.keySet()) {
                        LOGGER.debug("setparameter:" + key + " " + parameters.get(key));
                    }
                }

                LOGGER.debug("Range of protein database IDs for which analysis StepInstances need to be created: " + bottomNewSequenceId + " - " + topNewSequenceId);
                LOGGER.debug("Range of protein database IDs for which NO StepInstances need to be created: " + bottomPrecalculatedSequenceId + " - " + topPrecalculatedSequenceId);
                LOGGER.debug("Range of protein database IDs for which the COMPLETION StepInstances need to be created: " + bottomProteinId + " - " + topProteinId);
            }

            int workerNumber = Integer.parseInt(this.parameters.get(StepInstanceCreatingStep.WORKER_NUMBER_KEY));

            if (completionJob != null && ! initialSetupSteps) {
                LOGGER.debug("Have a completion Job.");
                LOGGER.warn("Have a completionJob Job: " + completionJob);
                //TODO this is temp for now

                if(prepareOutputJob != null){
                    LOGGER.warn("Have a PrepareOutputJob Job :" + prepareOutputJob);
                    //round this number to nearest thousand
                    int rawMaxProteins = (int) (topProteinId / workerNumber);

                    if (rawMaxProteins < 1){
                        LOGGER.warn("rawMaxProteins <= 1, rawMaxProteins for matchLookup:- " + rawMaxProteins);
                        rawMaxProteins = 1;
                    }
                    int maxProteins = (int) (Math.ceil(rawMaxProteins / 1000.0) * 1000);
                    LOGGER.warn("workerNumber =  " + workerNumber + ", maxProteins for matchLookup:- " + maxProteins);
                    LOGGER.warn("Create prepare output jobs for this run ...");

                    for (Step step : prepareOutputJob.getSteps()) {
                        //StepInstance stepInstance = new StepInstance(step, bottomProteinId, topProteinId, null, null);
                        step.setMaxProteins(maxProteins);

                        final List<StepInstance> jobStepInstances = createStepInstances(step, bottomProteinId, topProteinId);
                        for (StepInstance jobStepInstance:jobStepInstances ){
//                        if(jobStepInstance.getParameters() == null){
//                            LOGGER.warn("MatchLookup Parameters is NULL : " );
//                        }
                            jobStepInstance.addParameters(parameters);
//                        LOGGER.warn("MatchLookup Parameters: " + jobStepInstance.getParameters().toString());
                        }
                        stepToStepInstances.put(step, jobStepInstances);
                        prepareOutputStepInstances.addAll(jobStepInstances);
                        Utilities.verboseLog("Created " + prepareOutputStepInstances.size() + " prepareOutput StepInstances");
                    }
                }else{
                    LOGGER.warn("PrepareOutputJob Job is NULL ");
                }


                for (Step step : completionJob.getSteps()) {
                    StepInstance stepInstance = new StepInstance(step, bottomProteinId, topProteinId, null, null);
                    stepInstance.addParameters(parameters);
                    completionStepInstances.add(stepInstance);
                }
                //add dependencies
                for (StepInstance jobStepInstance : prepareOutputStepInstances) {
                    for (StepInstance completionStepInstance : completionStepInstances) {
                        completionStepInstance.addDependentStepInstance(jobStepInstance);
                    }
                }


            }

            // We will always have the FiniliseStepsJobs
            final List<StepInstance> initialSetupStepInstances = new ArrayList<StepInstance>();

            if (matchLookupJob != null) {
                LOGGER.debug("Have a matchLookupJob Job.");
                LOGGER.warn("Have a matchLookupJob Job: " + matchLookupJob);

                //round this number to nearest thousand
                int rawMaxProteins = (int) (topProteinId / workerNumber);

                if (rawMaxProteins < 1){
                    LOGGER.warn("rawMaxProteins <= 1, rawMaxProteins for matchLookup:- " + rawMaxProteins);
                    rawMaxProteins = 1;
                }
                int maxProteins = (int) (Math.ceil(rawMaxProteins / 1000.0) * 1000);
                LOGGER.warn("workerNumber =  " + workerNumber + ", maxProteins for matchLookup:- " + maxProteins);
                for (Step step : matchLookupJob.getSteps()) {
                    //StepInstance stepInstance = new StepInstance(step, bottomProteinId, topProteinId, null, null);
                    step.setMaxProteins(maxProteins);

                    final List<StepInstance> jobStepInstances = createStepInstances(step, bottomProteinId, topProteinId);
                    for (StepInstance jobStepInstance:jobStepInstances ){
//                        if(jobStepInstance.getParameters() == null){
//                            LOGGER.warn("MatchLookup Parameters is NULL : " );
//                        }
                        jobStepInstance.addParameters(parameters);
//                        LOGGER.warn("MatchLookup Parameters: " + jobStepInstance.getParameters().toString());
                    }
                    stepToStepInstances.put(step, jobStepInstances);
                    initialSetupStepInstances.addAll(jobStepInstances);
                }
            }else{
                LOGGER.warn("matchLookupJob is null.");
            }

            if (finaliseInitialStepsJob != null) {
                LOGGER.warn("Have a finaliseInitialStepsJob Job: " + finaliseInitialStepsJob);
                for (Step step : finaliseInitialStepsJob.getSteps()) {
                    //StepInstance stepInstance = new StepInstance(step, bottomProteinId, topProteinId, null, null);
                    LOGGER.warn("step : " + step.getMaxProteins());
                    LOGGER.warn("step max proteins: " + step);
//                    final List<StepInstance> jobStepInstances = createStepInstances(step, bottomProteinId, topProteinId);
                    StepInstance jobStepInstance = new StepInstance(step, bottomProteinId, topProteinId, null, null);
                    jobStepInstance.addParameters(parameters);
//                    LOGGER.warn("FinaliseInitialStepsJob Parameters: " + parameters.toString());

                    LOGGER.warn("step max proteins: " + step.getMaxProteins());
                    LOGGER.warn("finaliseInitialStepsJob stepInstance " + jobStepInstance);
                    List<StepInstance> finaliseInitialStepsSingletonStepInstance = new ArrayList<>();
                    finaliseInitialStepsSingletonStepInstance.add(jobStepInstance);
                    stepToStepInstances.put(step, finaliseInitialStepsSingletonStepInstance);
                    initialSetupStepInstances.add(jobStepInstance);
                }
                LOGGER.warn("initialSetupStepInstances:- " + initialSetupStepInstances);
                for (StepInstance stepInstance : initialSetupStepInstances) {
                    LOGGER.warn("stepId: " + stepInstance.getStepId());
                }
            }else{
                LOGGER.warn("finaliseInitialStepsJob is null.");
            }

            LOGGER.warn("initialSetupStepInstances Steps: " + initialSetupStepInstances.size());

            if (! stepToStepInstances.isEmpty()) {
                addDependenciesAndStore(stepToStepInstances);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Completion Steps:" + completionStepInstances.size());
            }

            if (initialSetupSteps){
                stepInstanceDAO.flush();
                return;
            }
            //else
            Utilities.verboseLog("Now create StepInstances for the regular jobs ... : useMatchLookupService: " + useMatchLookupService
                    + " idsWithoutLookupHit: " + (idsWithoutLookupHit != null));
            if (bottomNewSequenceId != null && topNewSequenceId != null) {
                if(! useMatchLookupService){
                    LOGGER.warn("Not useMatchLookupService  so create jobs for all analyses.");
                }
                if(idsWithoutLookupHit != null){
                    LOGGER.warn("idsWithoutLookupHit is NOT NULL, so create jobs for all analyses.");
                }

                for (Job job : jobs.getJobList()) {
                    //Only create new step instances for analysis which aren't integrated in the lookup service
                    //These jobs are flagged with 'doRunLocally'=TRUE
                    //or when we have idsWithoutLookupHit
                    //or when we are not using the lookup service
                    if (job.isDoRunLocally() || idsWithoutLookupHit != null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Job for which StepInstances are being created: " + job.getId());
                        }
                        Utilities.verboseLog("Job for which StepInstances are being created: " + job.getId());
                        for (Step step : job.getSteps()) {
                            if (step.isCreateStepInstancesForNewProteins()) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Creating StepInstance for step " + step.getId() + " protein range " + bottomNewSequenceId + " - " + topNewSequenceId);
                                }
                                Utilities.verboseLog("Creating StepInstance for step " + step.getId() + " protein range " + bottomNewSequenceId + " - " + topNewSequenceId);
                                final List<StepInstance> jobStepInstances = createStepInstances(step, bottomNewSequenceId, topNewSequenceId);
                                stepToStepInstances.put(step, jobStepInstances);
                                for (StepInstance jobStepInstance : jobStepInstances) {
//                                    for (StepInstance completionStepInstance : completionStepInstances) {
//                                        completionStepInstance.addDependentStepInstance(jobStepInstance);
//                                    }
                                    for (StepInstance prepareOutputStepInstance : prepareOutputStepInstances) {
                                        prepareOutputStepInstance.addDependentStepInstance(jobStepInstance);
                                    }
                                }
                            }
                        }
                    }
                }
                if(stepToStepInstances.isEmpty()){
                    Utilities.verboseLog("stepToStepInstances is emnpty");
                    //return; //is there anything else to do??
                }else{
                    Utilities.verboseLog("stepToStepInstances  size:" + stepToStepInstances.size());
                }
                LOGGER.debug("1. stepToStepInstances.keySet() size:" + stepToStepInstances.keySet().size());
                Utilities.verboseLog("1. stepToStepInstances.keySet() size:" + stepToStepInstances.keySet().size());
                addDependenciesAndStore(stepToStepInstances);
            }
            /* old way
            if (bottomNewSequenceId != null && topNewSequenceId != null) {
                // Instantiate the StepInstances - no dependencies yet.
                for (Job job : jobs.getJobList()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Job for which StepInstances are being created: " + job.getId());
                    }
                    Utilities.verboseLog("Job for which StepInstances are being created: " + job.getId());
                    for (Step step : job.getSteps()) {
                        if (step.isCreateStepInstancesForNewProteins()) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Creating StepInstance for step " + step.getId() + " protein range " + bottomNewSequenceId + " - " + topNewSequenceId);
                            }
                            Utilities.verboseLog("Creating StepInstance for step " + step.getId() + " protein range " + bottomNewSequenceId + " - " + topNewSequenceId);
                            final long newSliceSize = getNewSliceSize(bottomProteinId,  topProteinId, step.getMaxProteins());

                            final List<StepInstance> jobStepInstances = createStepInstances(step, bottomNewSequenceId, topNewSequenceId);
                            stepToStepInstances.put(step, jobStepInstances);
                            for (StepInstance jobStepInstance : jobStepInstances) {
                                for (StepInstance completionStepInstance : completionStepInstances) {
                                    completionStepInstance.addDependentStepInstance(jobStepInstance);
                                }
                            }
                        }
                    }
                }
                if(stepToStepInstances.isEmpty()){
                    Utilities.verboseLog("stepToStepInstances is emnpty");
                    //return; //is there anything else to do??
                }else{
                    Utilities.verboseLog("stepToStepInstances  size:" + stepToStepInstances.size());
                }
                LOGGER.debug("1. stepToStepInstances.keySet() size:" + stepToStepInstances.keySet().size());
                Utilities.verboseLog("1. stepToStepInstances.keySet() size:" + stepToStepInstances.keySet().size());
                addDependenciesAndStore(stepToStepInstances);
            }

            //TODO: Quick fix solution, which allows to run analysis locally for analysis which aren't integrated in the lookup service or are licensed
            //At the moment these are SignalP, TMHMM, Coils and  Phobius
            if (bottomPrecalculatedSequenceId != null && topPrecalculatedSequenceId != null) {
                // Instantiate the StepInstances - no dependencies yet.
                for (Job job : jobs.getJobList()) {
                    //Only create new step instances for analysis which aren't integrated in the lookup service
                    //These jobs are flagged with 'doRunLocally'=TRUE
                    if (job.isDoRunLocally()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Job for which StepInstances are being created: " + job.getId());
                        }
                        for (Step step : job.getSteps()) {
                            if (step.isCreateStepInstancesForNewProteins()) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Creating StepInstance for step " + step.getId() + " protein range " + bottomPrecalculatedSequenceId + " - " + topPrecalculatedSequenceId);
                                }
                                final List<StepInstance> jobStepInstances = createStepInstances(step, bottomPrecalculatedSequenceId, topPrecalculatedSequenceId);
                                stepToStepInstances.put(step, jobStepInstances);
                                for (StepInstance jobStepInstance : jobStepInstances) {
                                    for (StepInstance completionStepInstance : completionStepInstances) {
                                        completionStepInstance.addDependentStepInstance(jobStepInstance);
                                    }
                                }
                            }
                        }
                    }
                }
                addDependenciesAndStore(stepToStepInstances);
            }
            */
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Storing Completion StepInstances");
            }
            Utilities.verboseLog("Storing Completion StepInstances");
            stepInstanceDAO.insert(completionStepInstances);
            stepInstanceDAO.flush();
        } catch (Exception e) {
            LOGGER.error("Exception thrown in createStepInstances() method: ", e);
            throw new IllegalStateException("Caught and logged Exception, re-thrown so things work properly.", e);
        }
    }

    private long getNewSliceSize(long bottomProteinId, long topProteinId, long maxProteins){
        long newSlice = 1l;

        return newSlice;
    }
}
