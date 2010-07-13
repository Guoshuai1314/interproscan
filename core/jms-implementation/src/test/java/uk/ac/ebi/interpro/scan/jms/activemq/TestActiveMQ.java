package uk.ac.ebi.interpro.scan.jms.activemq;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Currently a stub for testing the ActiveMQ setup.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TestActiveMQ {

    private static final Logger LOGGER = Logger.getLogger(TestActiveMQ.class.getName());

    @Resource
    private AmqInterProScanMaster amqstandalone;

    @Test
    public void testEmbeddedSystem() {
        amqstandalone.setFastaFilePath("5.fasta");
        amqstandalone.run();

        // TODO - Test the contents of the database  - look for proteins / sequences / completed steps etc.
    }
}
