package uk.ac.ebi.interpro.scan.io.match.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import uk.ac.ebi.interpro.scan.model.Protein;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;


/**
 * Write matches as output for InterProScan user in JSON format.
 */
public class ProteinMatchesJSONResultWriter implements AutoCloseable {

    protected BufferedWriter fileWriter;

    protected DateFormat dmyFormat;
    protected static final Charset characterSet = Charset.defaultCharset();

    public ProteinMatchesJSONResultWriter(Path path) throws IOException {
        this.fileWriter = Files.newBufferedWriter(path, characterSet);
        this.dmyFormat = new SimpleDateFormat("dd-MM-yyyy");
    }


    /**
     * Writes out a set of proteins to a JSON file
     *
     * @param proteins containing matches to be written out
     * @throws IOException in the event of I/O problem writing out the file.
     */
    public void write(final Set<Protein> proteins, final boolean isSlimOutput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); // E.g. matches for un-integrated signatures have no InterPro entry assigned
        ObjectWriter objectWriter = (isSlimOutput ? mapper.writer() : mapper.writerWithDefaultPrettyPrinter());
        String json = objectWriter.writeValueAsString(proteins);
        fileWriter.write(json);
    }

    public void close() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }

}
