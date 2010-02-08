package uk.ac.ebi.interpro.scan.jms.broker;


import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.jms.SessionHandler;
import uk.ac.ebi.interpro.scan.jms.broker.platforms.WorkerRunner;

import javax.jms.*;
import java.io.Serializable;
import java.lang.IllegalStateException;

/**
 * Runs on the broker in a separate thread, moving any
 * jobs from the job submission queue to the request queue,
 * starting up a Destination on demand for each job.
 *
 * @author Phil Jones
 * @version $Id: QueueJumper.java,v 1.1.1.1 2009/10/07 13:37:52 pjones Exp $
 * @since 1.0
 */
public class QueueJumper implements Runnable{

    private int workersStarted = 0;

    private WorkerRunner parallelWorkerRunner;

    private SessionHandler sessionHandler;

    private String jobSubmissionQueueName;

    private String workerJobRequestQueueName;

    private volatile boolean running = true;

    private String jmsMessageSelector;

    @Required
    public void setParallelWorkerRunner(WorkerRunner workerRunner) {
        this.parallelWorkerRunner = workerRunner;
    }

    @Required
    public void setConnectionConfiguration(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    @Required
    public void setJobSubmissionQueueName(String jobSubmissionQueueName) {
        this.jobSubmissionQueueName = jobSubmissionQueueName;
    }

    @Required
    public void setWorkerJobRequestQueueName(String workerJobRequestQueueName) {
        this.workerJobRequestQueueName = workerJobRequestQueueName;
    }

    /**
     * Allows clean shutdown of the QueueJumper thread.
     */
    void shutdown(){
        running = false;
    }

     /**
     * Optional setter to allow a JMS filter to be passed in.
     * <p/>
     * See JMS Version 1.1 documentation for building selector clauses.
     *
     * @param messageSelector JMS message selector clause.
     */
    public void setJmsMessageSelector(String messageSelector) {
        this.jmsMessageSelector = messageSelector;
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
    public void run() {
        System.out.println("In QueueJumper thread start method.");

        try{
            sessionHandler.init();
            MessageConsumer messageConsumer = null;
            if (jmsMessageSelector == null){
                messageConsumer = sessionHandler.getMessageConsumer(jobSubmissionQueueName);
            }
            else {
                messageConsumer = sessionHandler.getMessageConsumer(jobSubmissionQueueName, jmsMessageSelector);
            }
            MessageProducer producer = sessionHandler.getMessageProducer(workerJobRequestQueueName);

            while (running){
                System.out.println("Waiting for message on submission queue...");
                Message message = messageConsumer.receive();
                Message outgoingMessage = null;


                if (message != null){
                    if (message instanceof ObjectMessage){
                        final Serializable serializedObject = ((ObjectMessage) message).getObject();
                        if (serializedObject != null){
                            outgoingMessage = sessionHandler.createObjectMessage(serializedObject);
                        }
                    }
                    else if (message instanceof TextMessage){
                        final String text = ((TextMessage) message).getText();
                        if (text != null){
                            outgoingMessage = sessionHandler.createTextMessage(text);
                        }
                    }
                }


                System.out.println("Message recieved on submission queue - routing to worker request queue.");
                if (((++workersStarted) % 5) == 0){
                    // Start up an extra worker every now and then...
                    System.out.println("Starting up an extra worker...");
                    parallelWorkerRunner.startupNewWorker();
                }

                parallelWorkerRunner.startupNewWorker();
                if (outgoingMessage == null){
                    throw new IllegalStateException("The QueueJumper was unable to forward the incoming message.");
                }
                producer.send(outgoingMessage);
                message.acknowledge();
            }

        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        finally{
            try {
                sessionHandler.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }
}
