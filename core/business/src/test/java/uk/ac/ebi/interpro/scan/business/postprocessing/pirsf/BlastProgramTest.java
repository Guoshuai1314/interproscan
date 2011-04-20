package uk.ac.ebi.interpro.scan.business.postprocessing.pirsf;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.interpro.scan.business.binary.BinaryRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Matthew Fraser
 * @author Maxim Scheremetjew
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BlastProgramTest extends TestCase {

    @javax.annotation.Resource
    private BinaryRunner binRunner;

    @javax.annotation.Resource(name = "inputFile")
    private Resource inputFileResource;

    @javax.annotation.Resource(name = "blastDbResource")
    private Resource blastDbResource;

    @Before
    public void init() {
        assertNotNull(binRunner);
    }


    @Test
    @Ignore
    public void testBlastProgram() {
        //TODO Finish this test!

        File inputFile = null;
        File blastDBFile = null;
        try {
            inputFile = inputFileResource.getFile();
            blastDBFile = blastDbResource.getFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        StringBuffer additionalArgs = null;
        InputStream is = null;
        if (inputFile != null) {
            additionalArgs = new StringBuffer();
            additionalArgs.append("-i " + inputFile.getAbsolutePath());
            additionalArgs.append(" -d " + blastDBFile.getAbsolutePath());
//            additionalArgs.append(" -o /tmp/blast/test.out");

            try {
                is = binRunner.run(additionalArgs.toString());
                Thread.sleep(1000L);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Ignore
    @Test
    public void testRunSimpleBlastProgram() {

//        try {
//            File outFile = File.createTempFile("blastout-", ".out");
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }

        try {
            binRunner.run();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
