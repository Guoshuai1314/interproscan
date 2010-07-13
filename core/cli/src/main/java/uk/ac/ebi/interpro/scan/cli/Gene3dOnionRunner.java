package uk.ac.ebi.interpro.scan.cli;

import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import uk.ac.ebi.interpro.scan.business.binary.RawMatchBinaryRunner;
import uk.ac.ebi.interpro.scan.business.filter.Gene3dRawMatchFilter;
import uk.ac.ebi.interpro.scan.io.ResourceWriter;
import uk.ac.ebi.interpro.scan.model.raw.Gene3dHmmer3RawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.io.*;
import java.util.Set;

/**
 * Runs Gene3D in "Onion" mode, writing raw and filtered results to separate text files.
 *
 * @author Antony Quinn
 * @version $Id$
 */
public class Gene3dOnionRunner {

    private static final Logger LOGGER = Logger.getLogger(Gene3dOnionRunner.class.getName());

    private RawMatchBinaryRunner<Gene3dHmmer3RawMatch> binaryRunner;
    private Gene3dRawMatchFilter filter;
    private ResourceWriter<Gene3dHmmer3RawMatch> analysisWriter;
    private ResourceWriter<Gene3dHmmer3RawMatch> resultsWriter;

    public void execute(Resource fastaFile, Resource hmmFile, Resource resultsDir) throws IOException {
        String resultsFileName = File.createTempFile("ipr-", null, resultsDir.getFile()).getName();
        execute(resultsFileName, fastaFile, hmmFile, resultsDir);
    }

    public void execute(String resultsFileName, Resource fastaFile, Resource hmmFile, Resource resultsDir) throws IOException {

        String resultsFilePath = resultsDir.getFile() + File.separator + resultsFileName;

        // Run HMMER
        binaryRunner.setTemporaryFilePath(resultsFilePath + ".bin.out");
        final Set<RawProtein<Gene3dHmmer3RawMatch>> rawProteins = binaryRunner.process(fastaFile, hmmFile);

        // Write TSV
        Resource rawResource = new FileSystemResource(resultsFilePath + ".raw");
        for (RawProtein<Gene3dHmmer3RawMatch> p : rawProteins) {
            analysisWriter.write(rawResource, p.getMatches(), true);
        }

        // Run DomainFinder
        filter.setTemporaryFilePath(resultsFilePath);
        final Set<RawProtein<Gene3dHmmer3RawMatch>> filteredProteins = filter.filter(rawProteins);

        // Write TSV
        Resource filteredResource = new FileSystemResource(resultsFilePath + ".fil");
        for (RawProtein<Gene3dHmmer3RawMatch> p : filteredProteins) {
            resultsWriter.write(filteredResource, p.getMatches(), true);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Raw matches [" + rawResource.getFilename() + "]:");
            LOGGER.debug(cat(rawResource.getInputStream()));
            LOGGER.debug("Filtered matches [" + filteredResource.getFilename() + "]:");
            if (filteredResource.exists()) {
                LOGGER.debug(cat(filteredResource.getInputStream()));
            } else {
                LOGGER.debug("No results");
            }
        }

    }

    public void setAnalysisWriter(ResourceWriter<Gene3dHmmer3RawMatch> analysisWriter) {
        this.analysisWriter = analysisWriter;
    }

    public void setResultsWriter(ResourceWriter<Gene3dHmmer3RawMatch> resultsWriter) {
        this.resultsWriter = resultsWriter;
    }

    public void setProcessor(RawMatchBinaryRunner<Gene3dHmmer3RawMatch> binaryRunner) {
        this.binaryRunner = binaryRunner;
    }

    public void setFilter(Gene3dRawMatchFilter filter) {
        this.filter = filter;
    }

    private String cat(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            while (reader.ready()) {
                sb.append(reader.readLine()).append("\n");
            }
            return sb.toString();
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void cat(InputStream in, PrintStream out) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            while (reader.ready()) {
                out.println(reader.readLine());
            }
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
