package uk.ac.ebi.interpro.scan.jms.activemq;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This controller monitors the activity of the Worker.  If the worker is inactive and has
 * been idle for longer than maximumIdleTimeMillis, or if the worker is inactive and
 * has been running for longer than maximumLifeMillis then it is shut down and the
 * JVM closes.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class DistributedWorkerController implements Runnable{

    private static final Logger LOGGER = Logger.getLogger(DistributedWorkerController.class.getName());

    private final long startUpTime          = new Date().getTime();

    private long lastMessageFinishedTime    = new Date().getTime();

    private long maximumIdleTimeMillis      = Long.MAX_VALUE;

    private long maximumLifeMillis          = Long.MAX_VALUE;

    private final List<String> runningJobs  = new ArrayList<String>();

    private final Object jobListLock        = new Object();

    private DefaultMessageListenerContainer messageListenerContainer;

    public void setMaximumIdleTimeSeconds(Long maximumIdleTime) {
        this.maximumIdleTimeMillis = maximumIdleTime * 1000;
    }

    public void setMaximumLifeSeconds(Long maximumLife) {
        this.maximumLifeMillis = maximumLife * 1000;
    }

    @Required
    public void setMessageListenerContainer(DefaultMessageListenerContainer messageListenerContainer) {
        this.messageListenerContainer = messageListenerContainer;
    }

    public void jobStarted(String jmsMessageId){
        synchronized (jobListLock){
            if (LOGGER.isDebugEnabled()){
                LOGGER.debug("Job " + jmsMessageId + " added to DistributedWorkerController.runningJobs");
            }
            runningJobs.add(jmsMessageId);
        }
    }

    public void jobFinished(String jmsMessageId){
        synchronized (jobListLock){
            if (LOGGER.isDebugEnabled()){
                LOGGER.debug("Job " + jmsMessageId + " removed from DistributedWorkerController.runningJobs");
            }
            if (! runningJobs.remove(jmsMessageId)){
                LOGGER.error ("DistributedWorkerController.jobFinished(jmsMessageId) has been called with a message ID that it does not recognise: " + jmsMessageId);
            }
            lastMessageFinishedTime = new Date().getTime();
        }
    }

    public boolean stopIfAppropriate(){
        synchronized (jobListLock){
            final long now = new Date().getTime();
            final boolean exceededLifespan = now - startUpTime > maximumLifeMillis;
            final boolean exceededIdleTime = now - lastMessageFinishedTime > maximumIdleTimeMillis;
            if (runningJobs.size() == 0 && ( exceededLifespan || exceededIdleTime )){

                if (LOGGER.isInfoEnabled()){
                    if (exceededLifespan){
                        LOGGER.info("Stopping worker as exceeded maximum life span");
                    }
                    else {
                        LOGGER.info("Stopping worker as idle for longer than max idle time");
                    }
                }

                messageListenerContainer.stop();
                return true;
            }
        }
        return false;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try{
            while(! stopIfAppropriate()){
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            LOGGER.error ("InterruptedException thrown by DistributedWorkerController.  Stopping now.", e);
        }
    }
}



