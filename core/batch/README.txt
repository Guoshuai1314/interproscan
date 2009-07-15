                          InterProScan Batch
                          -------------------

  What is it?
  -----------

  InterProScan Batch contains batch-processing logic for the InterProScan project.

  Requirements
  ------------

  To install:
   o  A Java 1.5 or later compatible virtual machine for your operating system.
   o  Maven 2.0 or later

  To run:
   o  A Java 1.5 or later compatible virtual machine for your operating system.

  The Latest Version
  ------------------

  See EBI Maven repository...

  Documentation
  -------------

  To run on the command line using Maven:
  mvn clean compile exec:java
   -Djava.security.policy=src/test/resources/uk/ac/ebi/interpro/scan/batch/partition/proactive/security.policy
   -Dexec.mainClass=org.springframework.batch.core.launch.support.CommandLineJobRunner 
   -Dexec.args="META-INF/spring/launch-context.xml hmmerJob sequences=file:fasta/*.fasta model=hmm/tigrfam_10.hmm"

  http://www.ebi.ac.uk/interpro/

  Installation
  ------------

  Type mvn in the top-level directory. See pom.xml for more information.

  Licensing
  ---------

  Please see the file called LICENSE.txt.

  Contacts
  --------

  Antony Quinn <aquinn@ebi.ac.uk>
  Phil Jones   <pjones@ebi.ac.uk>

  Acknowledgments
  ----------------

  InterPro is currently funded by grant number 213037 from the European Union under the program 
  "FP7 capacities: Scientific Data Repositories". The working title for the project is
  IMproving Protein Annotation and Co-ordination using Technology (IMPACT).

  $Id: README.txt,v 1.2 2009/06/18 10:53:08 aquinn Exp $
  