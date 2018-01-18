package sample;

//import util.properties packages
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

//import simple producer packages
import org.apache.kafka.clients.producer.*;

//import ProducerRecord packages
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

//Create java class named SimpleProducer wrapping the kafka Producer
public class SimpleProducer{


    Producer<String, byte[]> producer;
    private String clientName;

    //holde the gui controller class
    private Controller controller;

    //date format - as required by the assignment protocol
    private SimpleDateFormat formatDate = new SimpleDateFormat("HH:MM:SS");

    /**
     * Constructor for a wrapper class for the kafka producer.
     * @param clientName - client name the user type in login dialog.
     * @param boot_server - kafka server ip and port, "ip:port"
     * @param controller - give access to GUI.
     */
    SimpleProducer(String clientName, String boot_server, Controller controller) {

        this.clientName = clientName;
        this.controller = controller;

        //producer properties - configuring the producer properties
        Properties props = new Properties();

        //Properties - Server ip and port
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, boot_server);

        //Properties - producer id
        props.put(ProducerConfig.CLIENT_ID_CONFIG,"KafkaProducer_" + System.currentTimeMillis());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        //Properties - setting time out ack to 10000ms (10s)
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);


        try {
            producer = new KafkaProducer<>(props);
            System.out.println("Kafka Producer created.");
        }
        catch (Exception e)
        {
            System.out.println("error creating producer message cannot be sent to server: " + e.getMessage());
        }
    }

    /**
     *
     * @param Topic - the topic we send the message too
     * @param message - the user message
     * wrapper method to send a message to kafka broker
     */
    synchronized void SendMessage(String Topic, String message) {

        //ProducerRecord(String topic, K key, V value)
        //creating a record class to wrap the user message with protocol format, "client_name hh:mm:ss - message"
        String messageToSend = clientName + " " + formatDate.format(new Date()) +" - " + message;

        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(Topic, "message", messageToSend.getBytes());


        //sending message in a background thread - for app responsiveness
        Thread y = new Thread(()->{

            try {

                RecordMetadata metadata;
                metadata = producer.send(record).get();

                //print the message data on the kafka server
                System.out.printf("sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d)\n",
                        record.key(), record.value().toString(), metadata.partition(),
                        metadata.offset());

                producer.flush();


            }
            catch (InterruptedException | ExecutionException e) {
                System.out.println("couldn't send message: " + e.getMessage());
                controller.addMessage("couldn't send message: " + e.getMessage());
                return;
            } catch(Exception e)
            {
                System.out.println("couldn't send message: " + e.getMessage());
                controller.addMessage("couldn't send message" + e.getMessage());
                return;
            }

            System.out.println("producer finished send message: " + Topic );
        });

        y.start();


    }

    /**
     * This Method create a record for every file chunk and send it.
     * the key value is name of the file, the message value is the byte[] of the file chunk
     * @param fileChunks array list contain the file to send divided to 10kb chunks
     * @param filename - name of the file
     * @param Topic - topic where u send the file to
     */
    public void sendFile(ArrayList<byte[]> fileChunks , String filename ,String Topic)
    {
        Thread y = new Thread(()->{

            int chunkNumber = 0;

            for (byte[] chunk : fileChunks) {

                ProducerRecord<String, byte[]> record = new ProducerRecord<>(Topic, filename + " " + (fileChunks.size() - 1) + " " + chunkNumber, chunk);

                RecordMetadata metadata;
                try {
                    metadata = producer.send(record).get();

                    //print the message data on the kafka server
                    System.out.printf("sent file record(key = %s chunk number = %d) " +
                                    "meta(partition = %d, offset = %d)\n",
                            record.key(), chunkNumber++, metadata.partition(),
                            metadata.offset());

                }
                catch (InterruptedException | ExecutionException e) {
                    System.out.println("Failed to send file record: " + e.getMessage());
                }
            }
            producer.flush();

            System.out.println("producer finished send file: " + filename + "  in topic: " + Topic );
        });
        y.start();
    }

    /**
     * Helper function to close the producer on error or exist.
     */
    void closeProducer()
    {
        producer.close();
    }



}
