package uk.ac.ebi.interpro.scan.business.sequence.fasta;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.interpro.scan.persistence.ProteinDAO;


/**
 * Created by IntelliJ IDEA.
 * User: phil
 * Date: 14-Nov-2009
 * Time: 15:01:29
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class LoadFastaFileTest {

    private LoadFastaFile loader;

    private ProteinDAO proteinDAO;

    private org.springframework.core.io.Resource fastaFile;

    @javax.annotation.Resource (name="loader")
    public void setLoader(LoadFastaFile loader) {
        this.loader = loader;
    }
    @javax.annotation.Resource (name="proteinDAO")
    public void setProteinDAO(ProteinDAO proteinDAO) {
        this.proteinDAO = proteinDAO;
    }
    @javax.annotation.Resource (name="fastaFile")
    public void setFastaFile(org.springframework.core.io.Resource fastaFile) {
        this.fastaFile = fastaFile;
    }

    @Test
    public void testLoader(){
        System.out.println("Loader:" + loader);
        System.out.println("FastaFile: " + fastaFile);
        loader.loadSequences(fastaFile);
        System.out.println("Proteins loaded: " + proteinDAO.count());
    }
}
