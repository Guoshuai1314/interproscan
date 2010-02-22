package uk.ac.ebi.interpro.scan.io.cli;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instances of this class can be instantiated to build a generic 'commmand line conversation'.
 * This allows any command to be called and captures both the console output and error output.
 *
 * This class is <b>NOT THREAD SAFE</b>, however a single instance of this class
 * can be reused synchronously in a single thread.
 *
 * @author  Phil Jones, EMBL-EBI
 * @author  Antony Quinn
 * @version $Id: CommandLineConversationImpl.java 112 2009-08-12 13:49:39Z aquinn.ebi $
 * @since   1.0
 */
public class CommandLineConversationImpl implements CommandLineConversation {

    private static volatile Logger LOGGER = Logger.getLogger(CommandLineConversationImpl.class);

    private String output;
    private String error;
    private volatile File outputFileHandle;
    private volatile File errorFileHandle;
    private Integer exitStatus;
    private Map<String, String> environment;
    private boolean overrideAllEnvironment;
    private File workingDirectory;
    private volatile IOException exceptionThrownByGobbler;

    /**
     * Runs a command on the command line synchronously.
     *
     * @param mergeErrorIntoOutput if true, error messages will be included in the
     *                            output and can be accessed through the <code>getOutput()</code> method.  In this case,
     *                            <code>getErrorMessage()</code> will return <code>null</code>.
     * @param commands             being a set of recognised commands.  Note that compound commands (i.e. commands
     * separated by spaces) need to be submitted as separate strings, e.g. `ls -l` is passed in as "ls", "-l"
     * @return the return code from the command after it has completed.
     * @throws java.io.IOException propagated from the RunTime.execute method.
     */
    @Override public int runCommand(boolean mergeErrorIntoOutput, String... commands)
            throws IOException, InterruptedException {
        return runCommand(mergeErrorIntoOutput, new ArrayList<String>(Arrays.asList(commands)));
    }


    /**
     * Runs a command on the command line synchronously.
     *
     * @param mergeErrorIntoOutput if true, error messages will be included in the
     *                            output and can be accessed through the <code>getOutput()</code> method.  In this case,
     *                            <code>getErrorMessage()</code> will return <code>null</code>.
     * @param commands             being a set of recognised commands.  Note that compound commands (i.e. commands
     * separated by spaces) need to be submitted as separate strings, e.g. `ls -l` is passed in as "ls", "-l"
     * @return the return code from the command after it has completed.
     * @throws java.io.IOException propagated from the RunTime.execute method.
     */
    @Override public int runCommand(boolean mergeErrorIntoOutput, List<String> commands)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(commands);

        // Set error redirect as requested.
        pb.redirectErrorStream(mergeErrorIntoOutput);

        // Sort out the environment stuff.
        Map<String, String> retrievedEnvironment = pb.environment();
        if (overrideAllEnvironment){
            retrievedEnvironment.clear();
        }
        if (environment != null){
            retrievedEnvironment.putAll(environment);
        }

        // Set the working directory.  If workingDirectory as null,
        // uses the directory of the current process.  Not advised!
        pb.directory(workingDirectory);

        // Run the command
        if (LOGGER.isDebugEnabled()){
            LOGGER.debug("Command Line: \n "+ pb);
        }


        Process process = pb.start();
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), outputFileHandle);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), errorFileHandle);
        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // Retrieve status and output from the command.
        exitStatus = process.waitFor();

        /**
         * Attempt to ensure that any IOExceptions thrown in the Gobbler threads are re-thrown
         * to be handled by the Worker.
         */
        if (exceptionThrownByGobbler != null){
            throw exceptionThrownByGobbler;
        }

        if (outputFileHandle == null){
            output = outputGobbler.getStreamContent();
        }

        if (mergeErrorIntoOutput || errorFileHandle != null){
            error = null;
        }
        else {
            error = errorGobbler.getStreamContent();
        }

        return exitStatus;
    }

    /**
     * Runs command and returns result as an {@link InputStream}.
     * 
     * @param   command             Command to run, for example "head -n 100 /tmp/example.txt"
     * @return  Command output
     * @throws  IOException if could not run command
     * @throws  IllegalStateException if could not run command, or if command returns a failure flag
     */
    @Override public InputStream runCommand(String command) throws IOException {
        int exitStatus;
        try {
            exitStatus = runCommand(false, Arrays.asList(command.split(" ")));
        }
        catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (exitStatus != 0) {
            throw new IllegalStateException (getErrorMessage());
        }
        // Not an ideal way to get an input stream...
        return new ByteArrayInputStream(getOutput().getBytes("UTF-8"));
    }

    /**
     * Allows the environment to be set, overriding any environment variables that
     * are included, or clearing <b>all</b> environment variables and setting only those
     * specified
     *
     * @param environmentVariables being a Map of environment variable name to value.
     * @param overrideAll          if all preexisting environment variables should be cleared first.
     */
    @Override public void setEnvironment(Map<String, String> environmentVariables, boolean overrideAll) {
        this.environment = environmentVariables;
        this.overrideAllEnvironment = overrideAll;
    }

    /**
     * Redirects the output from the command directly to the specified file.
     * This method tries its best to ensure that the path is writable.
     * If a filePath is set, then the method 'getOutput' will return null.
     *
     * @param filePath the path of the file to which output should be redirected.
     */
    @Override public void setOutputPathToFile(String filePath, boolean overwriteIfExists, boolean append)
            throws IOException, InterruptedException {
        outputFileHandle = createfileHandle(filePath, overwriteIfExists, append);
    }

    /**
     * Redirects the error stream from the command directly to the specified file.
     * If a filePath is set, then the method 'getError' will return null.
     *
     * @param filePath the path of the file to which error output should be redirected.
     */
    @Override public void setErrorPathToFile(String filePath, boolean overwriteIfExists, boolean append)
            throws IOException, InterruptedException {
        errorFileHandle = createfileHandle (filePath, overwriteIfExists, append);
    }

    /**
     * Sets the working directory for subsequent commands.
     *
     * @param directoryPath being a valid path to a working directory
     * @throws FileNotFoundException if the directory path given does not exist.
     * @throws FileIsNotADirectoryException if the path exists, but does not resolve to a directory.
     */
    @Override public void setWorkingDirectory(String directoryPath)
            throws FileNotFoundException, FileIsNotADirectoryException {
        File file = new File (directoryPath);
        if (! file.exists()){
            throw new FileNotFoundException("Directory " + directoryPath + " does not exist.");
        }
        if (! file.isDirectory()){
            throw new FileIsNotADirectoryException("Directory " + directoryPath + " exists, " +
                    "but inputStream not a directory.");
        }
        workingDirectory = file;
    }

    /**
     * @return The output from the command, or null if no output was produced or no command has been run.
     */
    @Override public String getOutput() {
        return output;
    }

    /**
     * @return The error message from the command, or null if no error message was generated or no command has been run.
     */
    @Override public String getErrorMessage() {
        return error;
    }

    /**
     * @return The exit status from the last command run, or null if no command has been run yet.
     */
    @Override public Integer getExitStatus() {
        return exitStatus;
    }
    
    private File createfileHandle(String filePath, boolean overwriteIfExists, boolean append) throws IOException, InterruptedException {
        File sinkFile = new File(filePath);
        if (sinkFile.exists()){
            if (! sinkFile.isFile()){
                throw new IOException("Attempting to redirect to a path which points to a directory or hidden file.");
            }
            if (overwriteIfExists){
                if (! sinkFile.delete()){
                    throw new IOException("The filePath " + filePath +
                            " already contains a file that cannot be deleted.");
                }
//                Thread.sleep (5000);
//                if (! sinkFile.createNewFile()){
//                    throw new IOException("Unable to create a new file " + filePath + " to route to.");
//                }
            }
            else if (append){
                if (! sinkFile.canWrite() ){
                    throw new IOException("Attempting to append to a read-only file: " + filePath);
                }
            }
            else {
                throw new IOException ("There is already a file located at " + filePath + ". " +
                        "The calling code has set overwriteIfExists to false, so this file will not be deleted.");
            }
        }
//        else if (! sinkFile.createNewFile()){
//            throw new IOException("Unable to create a new file " + filePath);
//        }
//        Thread.sleep (5000); // Give NFS a chance to catch up before proceeding.
        return sinkFile;
    }

    /**
     * Inner class that reads in the output / error streams from the process.
     * This is required because otherwise the stream buffers in the underlying OS
     * may / will fill causing the process to hang.
     *
     * These readers operate in a separate thread, ensuring that the buffers
     * for error / output are emptied in a timely manner.
     */
    class StreamGobbler extends Thread {
        InputStream inputStream;
        StringBuffer stringBuffer = new StringBuffer();
        private File gobblerFileHandle;

        StreamGobbler(InputStream inputStream) {
            this(inputStream, null);
            // These stream gobblers really need to run to keep up with the external process.
            this.setPriority(Thread.MAX_PRIORITY);
        }

        StreamGobbler(InputStream inputStream, File outputFileHandle) {
            this.inputStream = inputStream;
            if (outputFileHandle != null){
                this.gobblerFileHandle = outputFileHandle;
            }
        }

        /**
         * Run method than consumes any ouput from the external process as it is produced.
         */
        public void run(){
            if (gobblerFileHandle == null){
                outputToString();
            }
            else {
                outputToFile();
            }
        }



        private void outputToString(){
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ( (line = bufferedReader.readLine()) != null){
                    stringBuffer
                            .append(line)
                            .append('\n');
                }
            }
            catch (IOException ioe) {
                LOGGER.error("IOException thrown when attempting to read InputStream from external process.", ioe);
                exceptionThrownByGobbler = ioe;
            }
            finally{
                if (bufferedReader != null){
                    try {
                        bufferedReader.close();
                    } catch (IOException ioe) {
                        LOGGER.error("IOException thrown when attempting to close " +
                                "BufferedReader from external process.", ioe);
                        exceptionThrownByGobbler = ioe;
                    }
                }
            }
        }

        /**
         * Uses java.nio for maximum speed / efficiency.
         */
        private void outputToFile() {
            final int BUFFER_SIZE = 4096;

            FileChannel destinationChannel = null;
            try {
                destinationChannel = new FileOutputStream(gobblerFileHandle).getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				byte[] bufferArray = buffer.array(); //get the backing byte array
                while (true) {
					buffer.clear(); // Prepare buffer for use, sets position to 0 and limit at capacity
					int lim = inputStream.read(bufferArray);
					if (lim == -1)
						break; // No more bytes to transfer
					buffer.flip(); //prepare for write, set position to 0
					buffer.limit(lim); //set the limit to what was read in from the stream
                    while (buffer.hasRemaining()){
					    destinationChannel.write(buffer);
                    }
				}
            }
            catch (IOException ioe) {
                LOGGER.error("IOException thrown when attempting to read InputStream from external process.", ioe);
                exceptionThrownByGobbler = ioe;
            }
            finally{
                try{
                    if (destinationChannel != null){
                        destinationChannel.close();
                    }
                    inputStream.close();
                }
                catch (IOException ioe) {
                    LOGGER.error("IOException thrown when attempting to close " +
                            "BufferedReader from external process.", ioe);
                    exceptionThrownByGobbler = ioe;
                }
            }
        }

        /**
         * Accessor to retrieve the ouput from the process as a String.
         * @return the ouput from the process as a String.
         */
        public String getStreamContent(){
            return stringBuffer.toString();
        }
    }

}
