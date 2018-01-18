package sample;


import java.io.*;
import java.util.*;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.CorruptRecordException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Consumers implements Runnable {

    //server ip and port
    private static String BOOTSTRAP_SERVERS = "";

    //Controller object - access to change GUI elements
    private Controller controller = null;


    /* Control variable for the thread
    *  stop - to stop this thread.
    *  Topic - the topic the consumer object subscribe to.
    */

    boolean stop = false;

    volatile String Topic = "";

    private Consumer<String, byte[]> consumer;
    private HashMap<Integer,byte[]> incomingFileBuffer = new HashMap<>();


    //constructor class
    Consumers(Controller controller)
    {
        this.controller=controller;
    }


    private Consumer<String, byte[]> createConsumer(String Topic) {

        BOOTSTRAP_SERVERS=this.controller.GetBootStrapServer();

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group1_" + System.currentTimeMillis());

        props.put(ConsumerConfig.CLIENT_ID_CONFIG, controller.getClientName() + " " + System.currentTimeMillis() );

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");


        // Create the consumer using props.
        final Consumer<String, byte[]> consumer =
                new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(Topic));


        return consumer;
    }




    @Override
    public void run() {

        System.out.println("creating consumer with Topic: " + this.Topic + " and start polling");
        this.consumer = createConsumer(this.Topic);

        controller.addMessage("joined " + Topic);
        while (!stop) {

            try {

                //polling up to 100 messages from server
                final ConsumerRecords<String, byte[]> consumerRecords =
                        consumer.poll(100);

                //if no message polled moving to next iteration
                if (consumerRecords.count() == 0) {
                    continue;
                }

                //for each message polled from server print to screen and console for more detail.
                //if file arriving saving chunks to a buffer
                consumerRecords.forEach(record -> {


                    if(record.key().toLowerCase().equals("message")) {
                        System.out.printf("Consumer Record:(%s, %s, %d, %d)\n", record.key(), new String(record.value()), record.partition(), record.offset());
                        this.controller.addMessage(new String(record.value()), this.Topic);
                    }
                    else{

                        String[] arr = record.key().split(" ");
                        System.out.println("receiving file: " + arr[1] + " out of " + arr[2]);
                        incomingFileBuffer.put(Integer.parseInt(arr[2]),record.value());
                        if(arr[1].equals(arr[2]))
                        {
                            // assuming received all bytes
                            System.out.println("file fully arrived");
                            WriteFile(arr[0]);
                        }
                    }
                });


            }
            catch (WakeupException e)
            {
                stop = true;//exit loop with exception
            }
            catch(CorruptRecordException e)
            {

                System.out.printf("Error getting message: " + e.getMessage() );
            }

        }

        consumer.close();
        controller.addMessage("Left topic: " + Topic);
        System.out.println("Consumer Thread with Topic: \"" + this.Topic + "\" closing");

    }

    /**
     * Helper Method save the file to Disk in a background thread
     */
    private void WriteFile(String fileName)
    {
        Thread backGroundWriting = new Thread(()->{

            System.out.println("file received saving at: " + controller.getOutDir());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            for(int i=0;i<incomingFileBuffer.size();i++)
            {
                try {
                    System.out.println("saving chunk " + i + "data: " + incomingFileBuffer.get(i));
                    bos.write(incomingFileBuffer.get(i));//putting together all file bytes
                } catch (IOException e) {
                    System.out.println("failed to save file, operation canceled");
                    incomingFileBuffer.clear();//clearing file buffer
                    controller.addMessage("failed to save file, operation canceled");
                    return;
                }
                catch (NoSuchElementException e)
                {
                    System.out.println("file failed to download or received incomplete file, operation canceled ");
                    incomingFileBuffer.clear();//clearing file buffer
                    controller.addMessage("file failed to download or received incomplete file, operation canceled ");

                    return;
                }

            }


            File dir = new File(controller.getOutDir());

            //creating dir if not exists
            if(!dir.exists())
            {
                dir.mkdirs();
            }

            String finalName = fileName;
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if(child.toString().equals(finalName))
                    {
                        finalName  += "(1)";
                    }
                }
            }

            try {
                //saving the file to disk
                FileOutputStream fos = new FileOutputStream(controller.getOutDir() + System.getProperty("file.separator") + finalName);
                fos.write(bos.toByteArray());
                fos.flush();
                fos.close();
                bos.close();

                System.out.println("File Saved Successfully");
                controller.addMessage("File Saved Successfully at: " + controller.getOutDir());
                incomingFileBuffer.clear();


            } catch (FileNotFoundException e) {
                System.out.println("file not found: " + e.getMessage());
                incomingFileBuffer.clear();//clearing file buffer
                return;
            } catch (IOException e) {
                System.out.println("Failed to save file to disk: " + e.getMessage());
                incomingFileBuffer.clear();//clearing file buffer
                return;
            }


        });
        backGroundWriting.start();
    }

    /**
     * Raising exception to close the consumer.
     */
    public void closeConsumer()
    {

        this.consumer.wakeup();

    }

    /**
     * Topic getter and setter
     */
    public String getTopic(){return Topic;}
    public void setTopic(String Topic){this.Topic = Topic;}


}
