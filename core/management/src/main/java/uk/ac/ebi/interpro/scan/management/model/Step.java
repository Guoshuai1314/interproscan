package uk.ac.ebi.interpro.scan.management.model;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

/**
 * Instances of this class describe / provide a template
 * for a step.  A single step corresponds to a single
 * message to a JMS queue.  Steps can be dependent upon
 * one another.
 *
 * Steps are always part of a Job, where a Job may comprise
 * one or more steps.
 *
 * To actually run
 * analyses against specific proteins (and perhaps specific models)
 * StepInstances are instantiated.  These instances are then
 * run as StepExecutions.  If a StepExecution fails, and the
 * Step is configured to be repeatable, then another attempt
 * to run the instance will be made.
 *
 * NOTE: Instances of Jobs and Steps are defined in Spring XML.  They
 * are NOT persisted to the database - only StepInstances and StepExecutions
 * are persisted.
 *
 * This class is abstract - it is expected that this will be
 * subclassed to allow additional parameters to be injected
 * and to implement the execute method.
 *
 * @author Phil Jones
 * @author David Binns
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public abstract class Step implements BeanNameAware {

    protected String id;

    protected Job job;

    protected String stepDescription;

    protected boolean parallel;


    /**
     * Number of retries
     */
    protected int retries;

    /**
     * Step which must be completed prior to this one.
     */
    protected List<Step> dependsUpon;

    /**
     * If not-null, this Step is run via Quartz using the cronSchedule
     * specified.  (Used for example to monitor new protein on UniParc.)
     */
    protected String cronSchedule;

    /**
     * Whenever new proteins are added to the database,
     * a routine should be called that iterates over all
     * Steps and creates StepTransactions appropriately.
     */
    protected boolean createStepInstancesForNewProteins = false;

    /**
     * Optional field indicating the maximum number of proteins
     * that a single StepTransaction should handle.
     * If null, then unlimited.
     */
    protected Integer maxProteins;

    /**
     * Optional field indicating the maximum number of models
     * that a single StepTransaction should handle.
     * If null, then unlimited.
     */
    protected Integer maxModels;

    /**
     * List of instances of this Step.
     */
    protected transient List<StepInstance> stepInstances;

    public String getId() {
        return id;
    }

    public void setBeanName(String s) {
        this.id = s;
    }

    public Job getJob() {
        return job;
    }

    @Required
    public void setJob(Job job) {
        this.job = job;
        job.addStep(this);
    }

    public List<Step> getDependsUpon() {
        return dependsUpon;
    }

    public void setDependsUpon(List<Step> dependsUpon) {
        this.dependsUpon = dependsUpon;
    }

    public boolean isCreateStepInstancesForNewProteins() {
        return createStepInstancesForNewProteins;
    }

    @Required
    public void setCreateStepInstancesForNewProteins(boolean createStepInstancesForNewProteins) {
        this.createStepInstancesForNewProteins = createStepInstancesForNewProteins;
    }

    public Integer getMaxProteins() {
        return maxProteins;
    }

    public void setMaxProteins(Integer maxProteins) {
        this.maxProteins = maxProteins;
    }

    public Integer getMaxModels() {
        return maxModels;
    }

    public void setMaxModels(Integer maxModels) {
        this.maxModels = maxModels;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    @Required
    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public boolean isParallel() {
        return parallel;
    }

    @Required
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public int getRetries() {
        return retries;
    }

    @Required
    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
     * This method is called to execute the action that the StepInstance must perform.
     *
     * @param stepInstance containing the parameters for executing.
     * @throws Exception could be anything thrown by the execute method.
     */
    public abstract void execute(StepInstance stepInstance) throws Exception;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Step)) return false;

        Step step = (Step) o;

        if (!id.equals(step.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Step");
        sb.append("{id='").append(id).append('\'');
        sb.append(", stepDescription='").append(stepDescription).append('\'');
        sb.append(", parallel=").append(parallel);
        sb.append(", retries=").append(retries);
        sb.append(", cronSchedule='").append(cronSchedule).append('\'');
        sb.append(", createStepInstancesForNewProteins=").append(createStepInstancesForNewProteins);
        sb.append(", maxProteins=").append(maxProteins);
        sb.append(", maxModels=").append(maxModels);
        sb.append(", stepInstances=").append(stepInstances);
        sb.append('}');
        return sb.toString();
    }
}
