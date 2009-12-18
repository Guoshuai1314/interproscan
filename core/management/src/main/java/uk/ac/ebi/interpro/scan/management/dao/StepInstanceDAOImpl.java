package uk.ac.ebi.interpro.scan.management.dao;

import uk.ac.ebi.interpro.scan.genericjpadao.GenericDAOImpl;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepExecutionState;

import javax.persistence.Query;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.transaction.annotation.Transactional;

/**
 * Created by IntelliJ IDEA.
 * User: pjones
 * Date: Dec 8, 2009
 * Time: 11:10:39 AM
 */
public class StepInstanceDAOImpl extends GenericDAOImpl<StepInstance, String> implements StepInstanceDAO{
    /**
     * Sets the class of the model that the DOA instance handles.
     * Note that this has been set up to use constructor injection
     * because it makes it easy to sub-class GenericDAOImpl in a robust
     * manner.
     * <p/>
     * Model class specific sub-classes should define a no-argument constructor
     * that calls this constructor with the appropriate class.
     */
    public StepInstanceDAOImpl() {
        super(StepInstance.class);
    }

    /**
     * Retrieve the StepInstances from the database for a particular Step.
     * Populate these into the Collection of step instances in the Step.
     * Return the updated step.
     *
     * @param step to be updated with StepInstance objects from the database.
     * @param optionalStates if none provided, then <b>all</b> StepInstances are returned.
     * If one or more provided, then limited to StepInstances in the specified state.
     * @return the updated Step.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<StepInstance> retrieveInstances(Step step, StepExecutionState... optionalStates) {
        Query query = entityManager.createQuery("select i from StepInstance i where i.stepId = :stepId");
        query.setParameter("stepId", step.getId());
        List<StepInstance> stepInstances = query.getResultList();
        for (StepInstance instance : stepInstances){
            // Re-attach the StepInstance to its parent Step.
            instance.setStep(step);
        }
        // Note that StepExecutions are EAGERLY fetched - required for the filtering step below.
        if (optionalStates != null && optionalStates.length > 0){
            // Filter on state.
            List<StepExecutionState> stateList = Arrays.asList(optionalStates);
            List<StepInstance> filteredStepInstances = new ArrayList<StepInstance>();
            for (StepInstance candidate : stepInstances){
                 if (stateList.contains(candidate.getState())){
                     filteredStepInstances.add (candidate);
                 }
            }
            return filteredStepInstances;
        }
        return stepInstances;
    }
}
