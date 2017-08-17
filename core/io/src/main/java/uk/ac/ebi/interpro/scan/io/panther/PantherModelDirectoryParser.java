package uk.ac.ebi.interpro.scan.io.panther;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import uk.ac.ebi.interpro.scan.io.AbstractModelFileParser;
import uk.ac.ebi.interpro.scan.model.Model;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.SignatureLibraryRelease;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.io.*;
import java.util.*;

/**
 * Class to parse PANTHER model directory, so that Signatures / Models can be loaded into I5.
 * <p/>
 * The directory is structured like this:
 * <p/>
 * ../model/books
 * ../model/books/PTHR10000/
 * ../model/books/PTHR10000/SF0
 * ../model/books/PTHR10003/
 * ../model/books/PTHR10003/SF10
 * ../model/books/PTHR10003/SF11
 * ../model/books/PTHR10003/SF27
 * ../model/books/PTHR10004/SF0
 * etc.
 * ../model/globals/
 * <p/>
 * So each model signature / Panther family has its own directory and even the sub families have their own directory,
 * which contains the HMMER model..
 *
 * @author Maxim Scheremetjew
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class PantherModelDirectoryParser extends AbstractModelFileParser {

    private static final Logger LOGGER = Logger.getLogger(PantherModelDirectoryParser.class.getName());

    private String namesTabFileStr;

    @Required
    public void setNamesTabFile(String namesTabFile) {
        this.namesTabFileStr = namesTabFile;
    }

    public String getNamesTabFileStr() {
        return namesTabFileStr;
    }

    /**
     * Method to parse a model file and return a SignatureLibraryRelease.
     *
     * @return a complete SignatureLibraryRelease object
     */
    @Override
    public SignatureLibraryRelease parse() throws IOException {
        LOGGER.debug("Starting to parse hmm file.");
        SignatureLibraryRelease release = new SignatureLibraryRelease(library, releaseVersion);
<<<<<<< HEAD
        Map<String, String> familyIdFamilyNameMap = readInPantherFamilyNames();

        LOGGER.debug(familyIdFamilyNameMap);
        LOGGER.warn("number of panther families: " + familyIdFamilyNameMap.keySet().size());
        Map<String, List<String>> pantherParentChildMap = getPantherParentChildMap(familyIdFamilyNameMap.keySet());
        for (String parent : pantherParentChildMap.keySet()) {
            String signatureAcc = parent;
            String signatureName = familyIdFamilyNameMap.get(signatureAcc);
            release.addSignature(createSignature(signatureAcc, signatureName, release));
            List<String> children = pantherParentChildMap.get(parent);
            for (String childSignatureAcc : children) {
                String childSignatureName = familyIdFamilyNameMap.get(childSignatureAcc);
                release.addSignature(createSignature(childSignatureAcc, childSignatureName, release));
            }
        }
        return release;
    }


    private Map<String, List<String>> getPantherParentChildMap(Set<String> pantherFamilyNames){
        String book = "";
        Map<String, List<String>> parentChildFamilyMap = new HashMap<>();

        for (String family: pantherFamilyNames) {
            String parent = null;
            if (! family.contains(":SF")) {
                //this is a parent
                parent = family;
            }else {
                //this is a child
                parent = family.split(":")[0];
            }

            List<String> parentChildren = parentChildFamilyMap.get(parent);
            if (parentChildren == null ){
                parentChildren = new ArrayList<>();
            }
            if (family.contains(":SF")) {
                // add this child to the parent
                parentChildren.add(family);
            }
            parentChildFamilyMap.put(parent, parentChildren);
        }

        return parentChildFamilyMap;
    }



    /**
     * Handles parsing process of the specified file resource.
     *
     * param namesTabPath Tab separated file resource with 2 columns (headers: accession, names).
     * @return A map of signature accessions and names.
     * @throws IOException
     */
    private Map<String, String> readInPantherFamilyNames() throws IOException {
        Map<String, String> result = null;
        String namesTabPath = modelFiles + File.pathSeparator + namesTabFileStr;
        File namesTabFile = new File(namesTabPath);
        if (namesTabFile.exists()) {
            result = parseTabFile(namesTabFile);
        }
        LOGGER.debug(namesTabPath);
        return result;
    }

    /**
     * Parses signature accessions and names out of the specified tab separated file.
     *
     * @param namesTabFile Tab separated file with 2 columns.
     * @return A map of signature accessions and names.
     */
    private Map<String, String> parseTabFile(File namesTabFile) {
        Map<String, String> result = new HashMap<String, String>();
        BufferedReader reader = null;
        int lineNumber = 0;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(namesTabFile)));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                lineNumber++;
                if (line.length() > 0 && line.startsWith("PTHR")) {
                    String[] columns = line.split("\t");
                    if (columns.length == 2) {
                        String familyId = columns[0];
                        familyId = familyId.replace(".mod", "");
                        //family Id is a super family
                        if (familyId.contains(".mag")) {
                            familyId = familyId.replace(".mag", "");
                        }
                        //family Id is a sub family
                        else {
                            familyId = familyId.replace(".", ":");
                        }
                        String familyName = columns[1];
                        result.put(familyId, familyName);
                    } else {
                        LOGGER.warn("Columns is Null OR unexpected splitting of line. Line is splitted into " + columns.length + "columns!");
                    }
                } else {
                    LOGGER.warn("Unexpected start of line: " + line);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't parse tab separated file with family names!", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.warn("Couldn't close buffered reader correctly!", e);
                }
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(lineNumber + " lines parsed.");
            LOGGER.info(result.size() + " entries created in the map.");
        }
        return result;
    }

    /**
     * Creates and returns an instance of signature.
     */
    private Signature createSignature(String accession, String name, SignatureLibraryRelease release) {
        Model model = new Model(accession, name, null);
        return new Signature(accession, name, null, null, null, release, Collections.singleton(model));
    }
}
