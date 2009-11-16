package uk.ac.ebi.interpro.scan.management.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 * TODO: Description
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public abstract class StepInstance<S extends Step, E extends StepExecution> implements Serializable {

    private static final String PROTEIN_BOTTOM_HOLDER = "\\[PROTSTART\\]";

    private static final String PROTEIN_TOP_HOLDER = "\\[PROTEND\\]";

    private static final String MODEL_BOTTOM_HOLDER = "\\[MODSTART\\]";

    private static final String MODEL_TOP_HOLDER = "\\[MODEND\\]";

    private String id;

    private S step;

    private Long bottomProtein;

    private Long topProtein;

    private Long bottomModel;

    private Long topModel;

    private List<StepInstance> dependencies = new ArrayList<StepInstance>();

    private List<E> executions = new ArrayList<E>();

    public StepInstance(UUID id, S step) {
        this.id = id.toString();
        this.step = step;
    }

    public void setBottomProtein(Long bottomProtein) {
        this.bottomProtein = bottomProtein;
    }

    public void setTopProtein(Long topProtein) {
        this.topProtein = topProtein;
    }

    public void setBottomModel(Long bottomModel) {
        this.bottomModel = bottomModel;
    }

    public void setTopModel(Long topModel) {
        this.topModel = topModel;
    }

    public void addDependentStepInstance(StepInstance dependentStepInstance){
        this.dependencies.add (dependentStepInstance);
    }

    public void addStepExecution(E stepExecution){
        // Sanity check
        for (E previousExecutions : executions){
            if (previousExecutions.getState() != StepExecutionState.STEP_EXECUTION_FAILED){
                throw new IllegalStateException ("Attempting to add a new StepExecution to step " + this + " when there is an existing (NON-STEP_EXECUTION_FAILED) step execution.");
            }
        }
        executions.add (stepExecution);
    }

    /**
     * Determines the state of this StepInstance from the states of all / any StepExecutions
     * @return the state of this StepInstance
     */
    public StepExecutionState getState(){
        if (executions.size() == 0){
            return StepExecutionState.NEW_STEP_INSTANCE;
        }
        for (E exec : executions){
            switch (exec.getState()){
                case NEW_STEP_EXECUTION:
                case STEP_EXECUTION_SUBMITTED:
                case STEP_EXECUTION_RUNNING:
                case STEP_EXECUTION_SUCCESSFUL:
                    return exec.getState();
                default:
                    break;
            }
        }
        return StepExecutionState.STEP_EXECUTION_FAILED;
    }

    public String getId() {
        return id;
    }

    public S getStep() {
        return step;
    }

    public Long getBottomProtein() {
        return bottomProtein;
    }

    public Long getTopProtein() {
        return topProtein;
    }

    public Long getBottomModel() {
        return bottomModel;
    }

    public Long getTopModel() {
        return topModel;
    }

    public List<StepInstance> stepInstanceDependsUpon() {
        return dependencies;
    }

    public List<E> getExecutions() {
        return executions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StepInstance)) return false;

        StepInstance that = (StepInstance) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public abstract E createStepExecution();

        /**
     * The format used for file names based upon integers
     * to ensure that they order correctly in the filesystem.
     */
    public static final NumberFormat TWELVE_DIGIT_INTEGER = new DecimalFormat("000000000000");

    public String filterFileNameProteinBounds (String fileNameTemplate, long bottomProteinId, long topProteinId){
        fileNameTemplate = fileNameTemplate.replaceAll(PROTEIN_BOTTOM_HOLDER, TWELVE_DIGIT_INTEGER.format(bottomProteinId));
        fileNameTemplate = fileNameTemplate.replaceAll(PROTEIN_TOP_HOLDER, TWELVE_DIGIT_INTEGER.format(topProteinId));
        return fileNameTemplate;
    }

     public String filterFileNameModelBounds (String fileNameTemplate, long bottomModelId, long topModelId){
        fileNameTemplate = fileNameTemplate.replaceAll(MODEL_BOTTOM_HOLDER, TWELVE_DIGIT_INTEGER.format(bottomModelId));
        fileNameTemplate = fileNameTemplate.replaceAll(MODEL_TOP_HOLDER, TWELVE_DIGIT_INTEGER.format(topModelId));
        return fileNameTemplate;
    }
}
