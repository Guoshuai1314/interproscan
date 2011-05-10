package uk.ac.ebi.interpro.scan.business.postprocessing.pirsf;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link BlastPostProcessor}.
 *
 * @author Matthew Fraser
 * @author Maxim Scheremetjew
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */

public class BlastPostProcessorTest {

    private static final Logger LOGGER = Logger.getLogger(BlastPostProcessorTest.class.getName());

    private BlastPostProcessor blastPostProcessor;

    @Before
    public void init() {
        blastPostProcessor = new BlastPostProcessor();
    }

    @Test
    public void testCheckBlastCriterion() {

        String sf1 = "SF000001";
        String sf2 = "SF000002";
        String sf3 = "SF000003";
        String proteinIdModelId1 = "1-" + sf1;
        String proteinIdModelId2 = "2-" + sf2;
        String proteinIdModelId3 = "3-" + sf3;

        Map<String, Integer> blastResultMap = new HashMap<String, Integer>();
        blastResultMap.put(proteinIdModelId1, 1);
        blastResultMap.put(proteinIdModelId2, 8);
        blastResultMap.put(proteinIdModelId3, 10);

        Map<String, Integer> sfTbMap = new HashMap<String, Integer>();
        sfTbMap.put(sf1, 400);
        sfTbMap.put(sf2, 20);
        // sf3 does not appear in sf.tb file

        //failed - because numberOfBlastHits < 9 and numberOfBlastHits / sfTbValue < 0.3334f
        assertFalse("BLAST criterion shouldn't be passed!", blastPostProcessor.checkBlastCriterion(sfTbMap, blastResultMap, proteinIdModelId1));
        //should be passed - because numberOfBlastHits / sfTbValue > 0.3334f
        assertTrue("BLAST criterion should be passed!", blastPostProcessor.checkBlastCriterion(sfTbMap, blastResultMap, proteinIdModelId2));
        //should be passed - because numberOfBlastHits > 9
        assertTrue("BLAST criterion should be passed!", blastPostProcessor.checkBlastCriterion(sfTbMap, blastResultMap, proteinIdModelId3));
    }
}
