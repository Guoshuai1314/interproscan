package uk.ac.ebi.interpro.scan.management.model.implementations.gene3d;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.io.gene3d.CathResolveHitsOutputParser;
import uk.ac.ebi.interpro.scan.io.gene3d.CathResolverRecord;
import uk.ac.ebi.interpro.scan.io.match.hmmer.hmmer3.Hmmer3DomTblParser;
import uk.ac.ebi.interpro.scan.io.match.hmmer.hmmer3.parsemodel.DomTblDomainMatch;
import uk.ac.ebi.interpro.scan.io.match.hmmer.hmmer3.parsemodel.DomainMatch;
import uk.ac.ebi.interpro.scan.io.match.hmmer.hmmer3.parsemodel.HmmSearchRecord;
import uk.ac.ebi.interpro.scan.io.match.hmmer.hmmer3.parsemodel.SequenceMatch;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.model.HmmBounds;
import uk.ac.ebi.interpro.scan.model.Hmmer3Match;
import uk.ac.ebi.interpro.scan.model.SignatureLibrary;
import uk.ac.ebi.interpro.scan.model.raw.*;
import uk.ac.ebi.interpro.scan.persistence.FilteredMatchDAO;
import uk.ac.ebi.interpro.scan.persistence.raw.RawMatchDAO;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This step parses the output from the gene3d hmmer3 and cath resolver  and then persists the matches.
 * No match filtering post processing required.
 *
 * @author Gift Nuka
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class Gene3DParseAndPersistOutputStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(Gene3DParseAndPersistOutputStep.class.getName());

    private Hmmer3DomTblParser hmmer3DomTblParser;

    private CathResolveHitsOutputParser cathResolveHitsOutputParser;

    private RawMatchDAO<Gene3dHmmer3RawMatch> rawMatchDAO;

    private FilteredMatchDAO<Gene3dHmmer3RawMatch, Hmmer3Match> filteredMatchDAO;

    private String signatureLibraryRelease;

    private String cathResolveHitsOutputFileNameTemplate;

    private String outputFileNameDomTbloutTemplate;

    @Required
    public void setCathResolveHitsOutputFileNameTemplate(String cathResolveHitsOutputFileNameTemplate) {
        this.cathResolveHitsOutputFileNameTemplate = cathResolveHitsOutputFileNameTemplate;
    }

    @Required
    public void setOutputFileNameDomTbloutTemplate(String outputFileNameDomTbloutTemplate) {
        this.outputFileNameDomTbloutTemplate = outputFileNameDomTbloutTemplate;
    }

    @Required
    public void setRawMatchDAO(RawMatchDAO<Gene3dHmmer3RawMatch> rawMatchDAO) {
        this.rawMatchDAO = rawMatchDAO;
    }

    @Required
    public void setFilteredMatchDAO(FilteredMatchDAO<Gene3dHmmer3RawMatch, Hmmer3Match> filteredMatchDAO) {
        this.filteredMatchDAO = filteredMatchDAO;
    }

    public void setHmmer3DomTblParser(Hmmer3DomTblParser hmmer3DomTblParser) {
        this.hmmer3DomTblParser = hmmer3DomTblParser;
    }

    public void setCathResolveHitsOutputParser(CathResolveHitsOutputParser cathResolveHitsOutputParser) {
        this.cathResolveHitsOutputParser = cathResolveHitsOutputParser;
    }

    public void setSignatureLibraryRelease(String signatureLibraryRelease) {
        this.signatureLibraryRelease = signatureLibraryRelease;
    }

    /**
     * Parse the output file from the SuperFamily binary and persist the results in the database.
     *
     * @param stepInstance           containing the parameters for executing. Provides utility methods as described
     *                               above.
     * @param temporaryFileDirectory which can be passed into the
     *                               stepInstance.buildFullyQualifiedFilePath(String temporaryFileDirectory, String fileNameTemplate) method
     */
    public void execute(StepInstance stepInstance, String temporaryFileDirectory) {

        // Retrieve raw matches from the SuperFamily binary output file
        InputStream domTblInputStream = null;
        InputStream cathResolverRecordInputStream = null;
        final String cathResolveHitsOutputFileName = stepInstance.buildFullyQualifiedFilePath(temporaryFileDirectory, cathResolveHitsOutputFileNameTemplate);
        final String domTblOutputFileName = stepInstance.buildFullyQualifiedFilePath(temporaryFileDirectory, outputFileNameDomTbloutTemplate);


        Map<String, DomTblDomainMatch> domainTblLineMatchMap = null;
        Map<String, CathResolverRecord> cathResolverRecordMap = null;

        try {
            cathResolverRecordInputStream = new FileInputStream(cathResolveHitsOutputFileName);
            cathResolverRecordMap = cathResolveHitsOutputParser.parse(cathResolverRecordInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("IOException thrown when attempting to parse " + cathResolveHitsOutputFileName, e);
        } finally {
            try {
                if (cathResolverRecordInputStream != null) {
                    cathResolverRecordInputStream.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream for " + cathResolveHitsOutputFileName, e);
            }
        }


        Map<String, RawProtein<Gene3dHmmer3RawMatch>> matchData = new HashMap();

        if (cathResolverRecordMap != null) {
            Utilities.verboseLog("cath-resolve-hits-map-size: " + cathResolverRecordMap.values().size());
            BufferedReader reader = null;
            try {
                //domTblInputStream = new FileInputStream(domTblOutputFileName);
                //domainTblLineMatchMap = hmmer3DomTblParser.parse(domTblInputStream);
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(domTblOutputFileName)));
                int lineNumber = 0;
                int domtblMatchCount = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    // Look for a domain data line.
                    Matcher domainDataLineMatcher = DomTblDomainMatch.DOMAIN_LINE_PATTERN.matcher(line);
                    if (domainDataLineMatcher.matches()) {
                        domtblMatchCount ++;
                        DomTblDomainMatch domTblDomainMatch = new DomTblDomainMatch(domainDataLineMatcher);
                        String domainLineKey = domTblDomainMatch.getDomTblDominLineKey();
                        CathResolverRecord cathResolverRecord = cathResolverRecordMap.get(domainLineKey);
                        if (cathResolverRecord != null && domTblDomainMatch.getSequenceEValue() < 0.001 ) {

                            //Utilities.verboseLog(cathResolverRecord.toString());
                            //DomTblDomainMatch domTblDomainMatch = domainTblLineMatchMap.get(cathResolverRecord.getRecordKey());
                            Utilities.verboseLog(domTblDomainMatch.toString());
                            String modelAccession = domTblDomainMatch.getQueryName();
//                            String gene3dModelAccession = (modelAccession.split("\\|")[2]).split("/")[0];
                            String gene3dModelAccession = modelAccession.split("\\-")[0];
                            LOGGER.debug("gene3d modelAccession: " + gene3dModelAccession + " from - " + modelAccession);
                            Utilities.verboseLog("gene3d modelAccession: " + gene3dModelAccession + " from - " + modelAccession );
                            modelAccession = gene3dModelAccession;
                            String[] locations = cathResolverRecord.getResolvedStartsStopsPosition().split("-");
                            int locationStart = Integer.parseInt(locations[0]);
                            int locationEnd = Integer.parseInt(locations[1]);

                            Gene3dHmmer3RawMatch gene3dHmmer3RawMatch = createMatch(signatureLibraryRelease, domTblDomainMatch, modelAccession, locationStart, locationEnd);
                            String sequenceIdentifier = gene3dHmmer3RawMatch.getSequenceIdentifier();

                            if (matchData.containsKey(sequenceIdentifier)) {
                                RawProtein<Gene3dHmmer3RawMatch> rawProtein = matchData.get(sequenceIdentifier);
                                rawProtein.addMatch(gene3dHmmer3RawMatch);
                            } else {
                                RawProtein<Gene3dHmmer3RawMatch> rawProtein = new RawProtein<Gene3dHmmer3RawMatch>(sequenceIdentifier);
                                rawProtein.addMatch(gene3dHmmer3RawMatch);
                                matchData.put(sequenceIdentifier, rawProtein);
                            }
                        }
                    }
                }
                Utilities.verboseLog(10, "DomTblDomainMatch count : " + domtblMatchCount);
            } catch (IOException e) {
                throw new IllegalStateException("IOException thrown when attempting to parse " + domTblOutputFileName);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error closing input stream", e);
                }
            }


            //now persists the rawmatches
            Set<RawProtein<Gene3dHmmer3RawMatch>> rawProteins = new HashSet<RawProtein<Gene3dHmmer3RawMatch>>(matchData.values());
            Utilities.verboseLog("rawProteins # :" + rawProteins.size());
            int count = 0;
            RawMatch represantiveRawMatch = null;
            for (RawProtein<Gene3dHmmer3RawMatch> rawProtein : rawProteins) {
                count += rawProtein.getMatches().size();
                if (represantiveRawMatch == null) {
                    if (rawProtein.getMatches().size() > 0) {
                        represantiveRawMatch = rawProtein.getMatches().iterator().next();
                    }
                }
            }


            if (rawProteins != null && rawProteins.size() > 0) {
                filteredMatchDAO.persist(rawProteins);
                Long now = System.currentTimeMillis();
                if (count > 0) {
                    int matchesFound = 0;
                    int waitTimeFactor = Utilities.getWaitTimeFactor(count).intValue();
                    if (represantiveRawMatch != null) {
                        Utilities.verboseLog("represantiveRawMatch :" + represantiveRawMatch.toString());
                        String signatureLibraryRelease = represantiveRawMatch.getSignatureLibraryRelease();
                        Utilities.sleep(waitTimeFactor * 1000);
                        //ignore the usual check until refactoring of the parse step
                    } else {
                        LOGGER.warn("Check if Raw matches committed " + count + " rm: " + represantiveRawMatch);
                        Utilities.verboseLog("Check if Raw matches committed " + count + " rm: " + represantiveRawMatch);
                    }
                    Long timeTaken = System.currentTimeMillis() - now;
                    Utilities.verboseLog("ParseStep: count: " + count + " represantiveRawMatch : " + represantiveRawMatch.toString()
                            + " time taken: " + timeTaken);
                }

            }
        }

    }


    protected Gene3dHmmer3RawMatch createMatch(final String signatureLibraryRelease,
                                               final DomTblDomainMatch domTblDomainMatch,
                                               String modelAccession,
                                               int locationStart,
                                               int locationEnd
    ) {
        return new Gene3dHmmer3RawMatch(
                domTblDomainMatch.getTargetIdentifier(),
                modelAccession,
                signatureLibraryRelease,
                locationStart,
                locationEnd,
                domTblDomainMatch.getSequenceEValue(),
                domTblDomainMatch.getSequenceScore(),
                domTblDomainMatch.getDomainHmmfrom(),
                domTblDomainMatch.getDomainHmmto(),
                "[]",
                domTblDomainMatch.getDomainScore(),
                locationStart,
                locationEnd,
                domTblDomainMatch.getDomainAccuracy(),
                domTblDomainMatch.getSequenceBias(),
                domTblDomainMatch.getDomainCEvalue(),
                domTblDomainMatch.getDomainIEvalue(),
                domTblDomainMatch.getDomainBias(),
                ""
        );
    }

}
