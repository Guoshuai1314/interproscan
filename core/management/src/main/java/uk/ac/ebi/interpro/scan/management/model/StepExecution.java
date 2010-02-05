package uk.ac.ebi.interpro.scan.management.model;

import org.apache.log4j.Logger;

import javax.swing.event.EventListenerList;
import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import uk.ac.ebi.interpro.scan.persistence.DAOManager;
import uk.ac.ebi.interpro.scan.management.dao.StepExecutionDAO;

/**
 * Abstract class for executing a Step.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table (name="step_execution")
@Inheritance (strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn (discriminatorType = DiscriminatorType.STRING)
public abstract class StepExecution<I extends StepInstance> implements Serializable {

    protected static final Logger LOGGER = Logger.getLogger(StepExecution.class);

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private String id;

    @ManyToOne (targetEntity = StepInstance.class, cascade = {}, optional = false)
    protected I stepInstance;

    @Enumerated(javax.persistence.EnumType.STRING)
    private StepExecutionState state = StepExecutionState.NEW_STEP_EXECUTION;

    @Column (nullable=true, name="time_created")
    private Date createdTime;

    @Column (nullable=true, name="time_started_running")
    private Date startedRunningTime;

    @Column (nullable=true, name="time_submitted")
    private Date submittedTime;

    @Column (nullable=true, name="time_completed")
    private Date completedTime;

    @Column (nullable=true, name="proportion_completed")
    private Double proportionCompleted;

    protected StepExecution(I stepInstance) {
        this.stepInstance = stepInstance;
        this.stepInstance.addStepExecution(this);
        createdTime = new Date();
    }

    /**
     * Don't use! Only here because required by JPA.
     */
    protected StepExecution() {
    }

    public void setStepInstance(I stepInstance) {
        this.stepInstance = stepInstance;
    }

    public void setState(StepExecutionState state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public I getStepInstance() {
        return stepInstance;
    }

    public StepExecutionState getState() {
        return state;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getSubmittedTime() {
        return submittedTime;
    }

    public Date getStartedRunningTime() {
        return startedRunningTime;
    }

    public Date getCompletedTime() {
        return completedTime;
    }

    public Double getProportionCompleted() {
        return proportionCompleted;
    }

    /**
     * If this method is called, the proportion complete is set and
     * all listeners to this StepExecution are informed that the
     * state has changed.
     * @param proportionCompleted between 0.0d and 1.0d to indicate
     * how much of the job has been completed.
     */
    protected void setProportionCompleted(Double proportionCompleted) {
        this.proportionCompleted = proportionCompleted;
    }

    /**
     * This method is called to execute the action that the StepExecution must perform.
     * This method should typically perform its activity in a try / catch / finally block
     * that sets the state of the step execution appropriately.
     *
     * Note that the implementation DOES have access to the protected stepInstance,
     * and from their to the protected Step, to allow it to access parameters for execution.
     * @param daoManager
     */
    public abstract void execute(DAOManager daoManager);

    public void submit(StepExecutionDAO stepExecutionDAO){
        if (state != StepExecutionState.NEW_STEP_EXECUTION){
            throw new IllegalStateException ("Attempting to submit a StepExecution to a queue, which is not in state 'NEW_STEP_EXECUTION'.");
        }
        state = StepExecutionState.STEP_EXECUTION_SUBMITTED;
        submittedTime = new Date();
        stepExecutionDAO.update(this);
    }

    public void setToRun(){
        if (state == StepExecutionState.STEP_EXECUTION_SUCCESSFUL || state == StepExecutionState.STEP_EXECUTION_FAILED){
            throw new IllegalStateException ("Attempting to set the state of this stepExecution to 'RUNNING', however it has already been completed.");
        }
        state = StepExecutionState.STEP_EXECUTION_RUNNING;
        startedRunningTime = new Date();
    }

    /**
     * Called by the execute() method implementation to indicate successful completion.
     */
    protected void completeSuccessfully(){
        if (state == StepExecutionState.STEP_EXECUTION_FAILED){
            throw new IllegalStateException("Try to set the state of this StepExecution to 'STEP_EXECUTION_SUCCESSFUL', however has previously been set to 'FAILED'.");
        }
        state = StepExecutionState.STEP_EXECUTION_SUCCESSFUL;
        completedTime = new Date();
    }

    /**
     * Called by the execute() method implementation to indicate a failure of execution.
     */
    protected void fail(){
        state = StepExecutionState.STEP_EXECUTION_FAILED;
        completedTime = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StepExecution)) return false;

        StepExecution that = (StepExecution) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (id != null){
            return id.hashCode();
        }
        else {
            return super.hashCode();
        }
    }

    @Override
    public String toString() {
        return "StepExecution{" +
                "id='" + id + '\'' +
                ", stepInstance=" + stepInstance +
                ", state=" + state +
                ", createdTime=" + createdTime +
                ", startedRunningTime=" + startedRunningTime +
                ", submittedTime=" + submittedTime +
                ", completedTime=" + completedTime +
                ", proportionCompleted=" + proportionCompleted +
                '}';
    }

    /**
     * Updates the state of this StepExecution based upon the state
     * of the freshStepExecution that has been returned from the
     * worker process.
     * @param freshStepExecution
     */
    public void refresh(StepExecution freshStepExecution) {
        assert (this.getId().equals(freshStepExecution.getId()));
        assert (this != freshStepExecution);
        this.completedTime = freshStepExecution.completedTime;
        this.createdTime = freshStepExecution.createdTime;
        this.proportionCompleted = freshStepExecution.proportionCompleted;
        this.startedRunningTime = freshStepExecution.startedRunningTime;
        this.submittedTime = freshStepExecution.submittedTime;
        this.state = freshStepExecution.state;
    }
}
