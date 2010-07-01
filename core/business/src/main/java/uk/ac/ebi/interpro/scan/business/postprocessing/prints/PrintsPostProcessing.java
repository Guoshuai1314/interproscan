package uk.ac.ebi.interpro.scan.business.postprocessing.prints;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import uk.ac.ebi.interpro.scan.io.prints.FingerPRINTSHierarchyDBParser;
import uk.ac.ebi.interpro.scan.model.raw.PrintsRawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author Phil Jones
 * @version $Id$
 * @since 1.0
 *        Date: 29-Jun-2010
 *        Time: 16:35:40
 *        <p/>
 *        PRINTS Post Processing Algorithm:
 *        <p/>
 *        Matches to "Domain" PRINTS models can match outside hierarchies - don't apply hierarchy rule.  evalue filtering still pertains.
 *        <p/>
 *        Algorithm:
 *        <p/>
 *        1. Filter by evalue <= cutoff
 *        2. Filter by motif count >= min motif count
 *        3. Order by evalue descending
 *        4. if domain:
 *        pass
 *        5. else if has hierarchy:
 *        FIRST (for protein), store hierarchy members, this match passes
 *        NOT FIRST , if in currently stored hierarchy members list, PASS and replace hierarchy members list from this one.
 */
public class PrintsPostProcessing implements Serializable {

    private static Logger LOGGER = Logger.getLogger(PrintsPostProcessing.class);

    private FingerPRINTSHierarchyDBParser hierarchyDBParser;

    private String fingerPRINTSHierarchyDB;

    private Map<String, FingerPRINTSHierarchyDBParser.HierachyDBEntry> printsModelData;

    private final Object hierchDbLock = new Object();

    @Required
    public void setHierarchyDBParser(FingerPRINTSHierarchyDBParser hierarchyDBParser) {
        this.hierarchyDBParser = hierarchyDBParser;
    }

    @Required
    public void setFingerPRINTSHierarchyDB(String fingerPRINTSHierarchyDB) {
        this.fingerPRINTSHierarchyDB = fingerPRINTSHierarchyDB;
    }

    /**
     * Post-processes raw results for Pfam HMMER3 in the batch requested.
     *
     * @param proteinIdToRawMatchMap being a Map of protein IDs to a List of raw matches
     * @return a Map of proteinIds to a List of filtered matches.
     */
    public Map<String, RawProtein<PrintsRawMatch>> process(Map<String, RawProtein<PrintsRawMatch>> proteinIdToRawMatchMap) throws IOException {
        if (printsModelData == null) {
            synchronized (hierchDbLock) {
                if (printsModelData == null) {
                    if (hierarchyDBParser == null || fingerPRINTSHierarchyDB == null) {
                        throw new IllegalStateException("The PrintsPostProcessing class requires the injection of a FingerPRINTSHierarchyDBParser and a fingerPRINTSHierarchyDB resource.");
                    }
                    Resource resource = new FileSystemResource(fingerPRINTSHierarchyDB);
                    printsModelData = hierarchyDBParser.parse(resource);
                }
            }
        }


        Map<String, RawProtein<PrintsRawMatch>> proteinIdToFilteredMatch = new HashMap<String, RawProtein<PrintsRawMatch>>();
        for (String proteinId : proteinIdToRawMatchMap.keySet()) {
            proteinIdToFilteredMatch.put(proteinId, processProtein(proteinIdToRawMatchMap.get(proteinId)));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total of " + getMatchCountDEBUG(proteinIdToRawMatchMap) + " raw matches passed in and " + getMatchCountDEBUG(proteinIdToFilteredMatch) + " passed out.");
        }

        return proteinIdToFilteredMatch;
    }

    /**
     * This method is for debugging only - counts the number of matches in the Collection.
     *
     * @param proteinCollection
     * @return
     */
    private int getMatchCountDEBUG(Map<String, RawProtein<PrintsRawMatch>> proteinCollection) {
        int count = 0;
        for (RawProtein<PrintsRawMatch> protein : proteinCollection.values()) {
            if (protein != null && protein.getMatches() != null) {
                count += protein.getMatches().size();
            }
        }
        return count;
    }

    /**
     * Algorithm:
     * <p/>
     * 1. Filter by evalue <= cutoff
     * 2. Filter by motif count >= min motif count
     * 3. Order by evalue descending
     * 4. if domain:
     * pass
     * 5. else if has hierarchy:
     * FIRST (for protein), store hierarchy members, this match passes
     * NOT FIRST , if in currently stored hierarchy members list, PASS and replace hierarchy members list from this one.
     *
     * @param rawProteinUnfiltered
     * @return
     */
    private RawProtein<PrintsRawMatch> processProtein(final RawProtein<PrintsRawMatch> rawProteinUnfiltered) {

        final RawProtein<PrintsRawMatch> filteredMatches = new RawProtein<PrintsRawMatch>(rawProteinUnfiltered.getProteinIdentifier());
        final Set<PrintsRawMatch> sortedRawMatches = new TreeSet<PrintsRawMatch>(PRINTS_RAW_MATCH_COMPARATOR); // Gets the raw matches into the correct order for processing.
        sortedRawMatches.addAll(rawProteinUnfiltered.getMatches());
        LOGGER.debug("New 'sortedRawMatches' set contains " + sortedRawMatches.size() + " matches.");
        String currentModelAccession = null;
        Set<PrintsRawMatch> motifMatchesForCurrentModel = new HashSet<PrintsRawMatch>();
        boolean currentMatchesPass = true;
        FingerPRINTSHierarchyDBParser.HierachyDBEntry currentHierachyDBEntry = null;
        final List<String> hierarchyModelIDLimitation = new ArrayList<String>();
        for (PrintsRawMatch rawMatch : sortedRawMatches) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Iterating over sorted raw matches.  Currently looking at protein " + rawProteinUnfiltered.getProteinIdentifier() + " model " + rawMatch.getModel());
            }
            if (currentModelAccession == null || !currentModelAccession.equals(rawMatch.getModel())) {
                // Either just started, or got to the end of the matches for one model, so filter & reset.

                // Process matches
                if (currentMatchesPass && currentModelAccession != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("In processProtein method, calling filterModelMatches for protein " + rawProteinUnfiltered.getProteinIdentifier() + " and model " + currentModelAccession);
                    }
                    filteredMatches.addAllMatches(filterModelMatches(motifMatchesForCurrentModel, currentHierachyDBEntry, hierarchyModelIDLimitation));
                }

                // Reset
                currentMatchesPass = true;
                motifMatchesForCurrentModel.clear();
                currentModelAccession = rawMatch.getModel();
                currentHierachyDBEntry = printsModelData.get(currentModelAccession);
                if (currentHierachyDBEntry == null) {
                    throw new IllegalStateException("There is no entry in the FingerPRINThierarchy.db file for model accession " + rawMatch.getModel());
                }
            }
            // Fail any matches that do not hit the evalue cutoff - first filter..
            if (currentMatchesPass)
                currentMatchesPass = rawMatch.getEvalue() <= currentHierachyDBEntry.getEvalueCutoff();
            if (currentMatchesPass) motifMatchesForCurrentModel.add(rawMatch);
        }
        // Don't forget to process the last set of matches!
        if (currentMatchesPass) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("In processProtein method, calling filterModelMatches for protein " + rawProteinUnfiltered.getProteinIdentifier() + " and model " + currentModelAccession);
            }
            filteredMatches.addAllMatches(filterModelMatches(motifMatchesForCurrentModel, currentHierachyDBEntry, hierarchyModelIDLimitation));
        }

        return filteredMatches;
    }

    /**
     * For a single protein & PRINTS model, filters the set of motif matches according the PRINTS PP algorithm.
     *
     * @param motifMatchesForCurrentModel being the Set of motif match records for a single protein / PRINTS model.
     * @param hierachyDBEntry             Details of the FingerPRINTShierarchy.db record for the current model.
     * @param hierarchyModelIDLimitation  List of model IDs that limit passing matches.  If empty, there is no restriction.
     * @return an empty set if the raw matches fail the filter, or the set of raw matches if they pass.
     */
    private Set<PrintsRawMatch> filterModelMatches(final Set<PrintsRawMatch> motifMatchesForCurrentModel,
                                                   final FingerPRINTSHierarchyDBParser.HierachyDBEntry hierachyDBEntry,
                                                   final List<String> hierarchyModelIDLimitation) {

        // Belt and braces - if the matches passed in are null / empty, just pass back nothing.
        if (motifMatchesForCurrentModel == null || motifMatchesForCurrentModel.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No matches passed into filterModelMatches method - exiting.");
            }
            return Collections.emptySet();
        }

        // Second filter - number of matched motifs must exceed the minimum number.
        if (motifMatchesForCurrentModel.size() < hierachyDBEntry.getMinimumMotifCount()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not enough motif matches: " + motifMatchesForCurrentModel.size() + " matched, but must be " + hierachyDBEntry.getMinimumMotifCount());
            }
            return Collections.emptySet();  // Failed filter.
        }

        // Third filter - pass if domain... (by definition, has no hierarchy to record).
        if (hierachyDBEntry.isDomain()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Match passes as is domain model.");
            }
            return motifMatchesForCurrentModel; // Passed filter, nothing else to do.
        }

        // Fourth filter - if there is a limitation from the previous model hierarchy, enforce
        if (hierarchyModelIDLimitation.size() > 0 && !hierarchyModelIDLimitation.contains(hierachyDBEntry.getId())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Match FAILED model hierarchy test, model ID " + hierachyDBEntry.getId());
            }
            return Collections.emptySet();       // Failed filter.
        }

        // Passed filter and may have its own hierarchy.  Check and set the current hierarchy limitation
        // if this is the case.
        if (hierachyDBEntry.getHierarchicalRelations() != null && hierachyDBEntry.getHierarchicalRelations().size() > 0) {
            hierarchyModelIDLimitation.clear();
            hierarchyModelIDLimitation.addAll(hierachyDBEntry.getHierarchicalRelations());
        } else if (hierarchyModelIDLimitation.size() > 0) {  // And this passing model has no relations! (Which is an error)
            throw new IllegalStateException("Have found inconsistencies in the FingerPRINTSHierarchy.db file.  Check hierarchy including PRINTS ID " + hierachyDBEntry.getId());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Matches passed!");
        }
        return motifMatchesForCurrentModel;
    }


    private static final Comparator<PrintsRawMatch> PRINTS_RAW_MATCH_COMPARATOR = new Comparator<PrintsRawMatch>() {

        /**
         * This comparator is CRITICAL to the working of PRINTS post-processing, so it has been defined
         * here rather than being the 'natural ordering' of PrintsRawMatch objects so it is not
         * accidentally modified 'out of context'.
         *
         * Sorts the raw matches by:
         *
         * evalue (best first)
         * model accession
         * motif number (ascending)
         * location start
         * location end
         *
         * @param o1 the first PrintsRawMatch to be compared.
         * @param o2 the second PrintsRawMatch to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first PrintsRawMatch is less than, equal to, or greater than the
         *         second PrintsRawMatch.
         */
        @Override
        public int compare(PrintsRawMatch o1, PrintsRawMatch o2) {
            int comparison = o1.getSequenceIdentifier().compareTo(o2.getSequenceIdentifier());
            if (comparison == 0) {
                if (o1.getEvalue() < o2.getEvalue()) comparison = -1;
                else if (o1.getEvalue() > o2.getEvalue()) comparison = 1;
            }
            if (comparison == 0) {
                comparison = o1.getModel().compareTo(o2.getModel());
            }
            if (comparison == 0) {
                if (o1.getMotifNumber() < o2.getMotifNumber()) comparison = -1;
                else if (o1.getMotifNumber() > o2.getMotifNumber()) comparison = 1;
            }
            if (comparison == 0) {
                if (o1.getLocationStart() < o2.getLocationStart()) comparison = -1;
                else if (o1.getLocationStart() > o2.getLocationStart()) comparison = 1;
            }
            if (comparison == 0) {
                if (o1.getLocationEnd() < o2.getLocationEnd()) comparison = -1;
                else if (o1.getLocationEnd() > o2.getLocationEnd()) comparison = 1;
            }
            return comparison;
        }
    };
}
