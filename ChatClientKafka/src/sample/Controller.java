package sample;


/**
 * Controller class - define Handlers methods to GUI elements
 *
 * Connecting between methods and GUI elements with FXML file
 *
 * this class connect the Backend to the Frontend of the app.
 *
 */

import javafx.application.Platform;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import org.apache.kafka.clients.admin.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class Controller {

    /**
     * vars representing the GUI elements
     */
    @FXML
    public TextArea textArea;
    @FXML
    public Button sendBtn, registerBtn, quitBtn, fileChooserBtn,changeDirBtn;
    @FXML
    public TextField topicField, messageField, registerField;
    @FXML
    public Text topicLabel, topicRegisterLabel, clientNameLabel;
    @FXML
    public Pane anchorPane;

    /**
     * Helper Vars
     */
    private Consumers consumer;
    private Vector<Consumers> consumerPool = new Vector<>();
    private String clientName;
    private String BOOTSTRAP_SERVERS;
    private SimpleProducer producer;
    private AdminClient admin;
    private File fileToSend;
    private String outDir;


    private final short REPLICATION_FACTOR = 3;
    private final int PARTITION = 1;

    /**
     * displaying formatted message from kafka broker on screen.
     *
     * @param message - message to display on screen
     * @param Topic   - topic to display on screen
     */
    synchronized void addMessage(String message, String Topic) {
        Platform.runLater(() -> textArea.appendText("(" + Topic + ") " + message + '\n'));
    }

    /**
     * display general message on screen - for error notifying
     *
     * @param message - the string to display on screen
     */
    synchronized void addMessage(String message) {
        Platform.runLater(() -> textArea.appendText(message + '\n'));
    }

    /**
     * Button callback - on Button "Send" press call this function.
     * check if topic if is not empty, check if broker available with admin client
     * Admin wait for 3 sec then declare broker unavailable.
     * if all ok producer send message, trying to send for 10s, if send failed show
     * error message to user.
     */
    public synchronized void sendBtnAction() {
        Platform.runLater(() -> {

            try {
                //making sure producer not null - for sanity check
                if (producer != null) {

                    //cannot send message with empty topic
                    if (topicField.getText().isEmpty()) {

                        System.out.println("Cannot send message without topic.");
                        addMessage("Cannot send message without topic.");
                        return;
                    }

                    //checking kafka server is online
                    if (!checkTopicOnServer(topicField.getText())) {

                        System.out.println("Topic Not Exist, Creating New One.");
                        createTopicWithAdmin(topicField.getText());
                    }

                    if (messageField.getText().startsWith("file:")) {
                        //sending a file
                        //read and split file to chunks and sending thous chunks with the consumer
                        ArrayList<byte[]> fileChunks = readFileChunks(fileToSend);
                        producer.sendFile(fileChunks, fileToSend.getName(), topicField.getText());


                    } else {
                        //send String message
                        System.out.println("Sending message with producer");
                        producer.SendMessage(topicField.getText(), messageField.getText());
                    }

                } else {
                    System.out.println("No producer pls restart app");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Broker unavailable: " + e.getMessage());
                addMessage("Broker unavailable could not registered for topic");
            } catch (Exception e) {
                System.out.println("could not send message: " + e.getMessage());
                addMessage("General Error: could not send message");

            }


            //clean message field
            messageField.setText("");

        });


    }

    /**
     * Register Button callback Function.
     * checking for existing topic, if topic exist creating new one with AdminClient with 1 partition and replicate factor 3
     * then creating consumer thread to poll from that Topic.
     */
    public void registerTopicBtnAction() {
        Platform.runLater(() -> {

            try {

                String TopicToRegister = registerField.getText();

                if (TopicToRegister.equals("")) {
                    System.out.println("Cannot register to empty Topic.");
                    addMessage("Cannot register to empty Topic.");
                    registerField.setText("");

                    return;
                }
                //checking if already registered for the topic
                for (Consumers consumer : consumerPool) {

                    if (consumer.getTopic().equals(TopicToRegister)) {

                        System.out.println("Already registered for the topic: " + consumer.getTopic());
                        addMessage("Already registered for the topic: " + consumer.getTopic());
                        registerField.setText("");
                        return;

                    }
                }

                if (!checkTopicOnServer(TopicToRegister)) {

                    System.out.println("No Topic Found in server, creating New Topic and consumer");

                    //no topic found creating new topic with 1 partition and replication factor 3
                    createTopicWithAdmin(TopicToRegister);
                }

                CreateConsumer(TopicToRegister);


            } catch (InterruptedException | ExecutionException e) {

                System.out.println("Broker unavailable: " + e.getMessage());
                addMessage("Broker unavailable could not registered for topic");
                //clearing the register field
                registerField.setText("");

            }

            //clearing the register field
            registerField.setText("");

        });

    }


    /**
     * Closing anyConsumer registered to Topic
     * if not registered for the topic display error message to user
     */
    public synchronized void leaveTopicBtnAction() {
        Platform.runLater(() -> {

            String TopicToLeave = registerField.getText();

            if (TopicToLeave.equals("")) {
                System.out.println("empty topic cannot leave.");
                addMessage("empty topic cannot leave.");
                registerField.setText("");
                return;
            }

            for (int i = 0; i < consumerPool.size(); i++) {
                if (consumerPool.elementAt(i).Topic.equals(TopicToLeave)) {
                    consumerPool.elementAt(i).closeConsumer();
                    System.out.println("Client Left Topic: " + TopicToLeave);
                    consumerPool.removeElementAt(i);
                    return;
                }
            }

            //not registered for the topic - display error message
            System.out.println("Not registered for the topic: " + TopicToLeave);
            addMessage("Not registered for the topic: " + TopicToLeave);


        });
    }

    /**
     * @param event - on exit/quit event - closing all consumers and producers then closing app
     */
    public synchronized void quitBtnAction(ActionEvent event) {
        Platform.runLater(() -> {

            //closing every running consumer
            for (int i = 0; i < consumerPool.size(); i++) {
                consumerPool.elementAt(i).closeConsumer();
            }

            //sleeping to make sure all consumer where closed
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Sleep Failed, continue closing the app");
            }

            //closing producer
            producer.closeProducer();

            //closing AdminClient
            admin.close();


            //close app
            Stage stage = (Stage) quitBtn.getScene().getWindow();
            stage.close();

        });


    }

    /**
     * File Chooser Button action - open file chooser dialog to
     * choose file to send.
     */
    public void fileChooserBtnAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileToSend = fileChooser.showOpenDialog(anchorPane.getScene().getWindow());

        if (fileToSend.exists()) {
            messageField.setText("file: " + fileToSend.getAbsolutePath());
        }
    }

    public void ChangeDirBtnAction()
    {
        while(true) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose folder to save files.");
            File choosenDir = directoryChooser.showDialog(anchorPane.getScene().getWindow());

            if (choosenDir.exists()) {
                outDir = choosenDir.getAbsolutePath();
                addMessage("Download Directory Changed to: " + outDir);
                return;
            }

            addMessage("Folder not exist.");
        }
    }

    /**
     * Helper method read bytes of the file in chunk of 10k
     */
    private ArrayList<byte[]> readFileChunks(File file) throws IOException {

        byte[] byteFile;
        int i, l;
        int block = 10240;
        int numblocks = 0;
        int counter = 0, totalSize = 0;
        int marker = 0;
        byte[] chunk;

        if (file != null && file.exists()) {
            byteFile = Files.readAllBytes(file.toPath());
            l = byteFile.length;
            numblocks = l / block;

            ArrayList<byte[]> data = new ArrayList<>();

            for (i = 0; i < numblocks; i++) {
                counter++;
                chunk = Arrays.copyOfRange(byteFile, marker, marker + block);
                data.add(chunk);
                totalSize += chunk.length;
                marker += block;
            }

            chunk = Arrays.copyOfRange(byteFile, marker, l);
            data.add(chunk);

            // the null value is a flag to the consumer, specifying that it has reached the end of the file
            return data;
        }

        return null;
    }

    /**
     * helper function checking if topic is already exist on the server.
     * admin client get from server the topic list and comparing it to the TopicToCheck
     * if exist on the list return true else false
     *
     * @param TopicToCheck - the topic to check if eist on the servers
     * @return true - if topic exist , false if topic not on server
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private boolean checkTopicOnServer(String TopicToCheck) throws ExecutionException, InterruptedException {

        String currentTopic;

        //searching Topic in existing topic
        Set<String> topicSet = admin.listTopics().names().get();

        Iterator<String> it = topicSet.iterator();

        System.out.println("Printing Topics on server:");

        for (int i = 0; i < topicSet.size(); i++) {

            currentTopic = it.next().toString();

            System.out.println(currentTopic);

            if (currentTopic.equals(TopicToCheck)) {
                //topic found in the server creating consumer thread and exiting the method.
                System.out.println("Found Topic in server.");
                return true;
            }

        }
        return false;
    }

    /**
     * hleper function - admin client create register a topic on the kafka server
     * topic created with replication factor 3 and 1 partition.
     *
     * @param TopicToCreate - String, the topic to create on the server
     */
    private void createTopicWithAdmin(String TopicToCreate) {
        Map<String, String> configs = new HashMap<>();

        admin.createTopics(Collections.singletonList(new NewTopic(TopicToCreate, PARTITION, REPLICATION_FACTOR).configs(configs)));

    }


    /**
     * @param Topic - for the consumer to register to.
     *              create new consumer thread to run int the background
     */
    private void CreateConsumer(String Topic) {
        consumer = new Consumers(this);

        consumer.setTopic(Topic);

        Thread x = new Thread(consumer);

        //saving thread in the thread pool
        consumerPool.add(consumer);

        //starting consumer thread
        x.start();

        System.out.println("consumer thread created");

    }


    /**
     * @param ke - keyboard event, enter pressed activating registerTopicBtnAction.
     */
    public void RegisterFieldOnkeyPressed(javafx.scene.input.KeyEvent ke) {
        if (ke.getCode().equals(KeyCode.ENTER)) {
            registerTopicBtnAction();
        }

    }

    /**
     * @param ke - keyboard event, enter press on message field focus - sending message.
     */
    public void MessageFieldOnkeyPressed(javafx.scene.input.KeyEvent ke) {
        if (ke.getCode().equals(KeyCode.ENTER)) {
            sendBtnAction();
        }
    }


    public void sendFileDialog(String fileName)
    {
        DialogPane dialog = new DialogPane();

        //dialog.getChildren().add();
        ProgressBar p2 = new ProgressBar();
        p2.setProgress(0.25F);

    }


    /**
     * On app load - reading from config file, creating adminClient and producer.
     * if config file is missing display error dialog and closing the app.
     */
    @FXML
    protected void initialize() {
        boolean stop = false;

        //read from file the broker ip and port
        BufferedReader brIn;
        try {

            //reading from config file broker ip and port
            brIn = new BufferedReader(new FileReader("config.txt"));
            BOOTSTRAP_SERVERS = brIn.readLine();

            //creating admin client
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
            config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
            admin = AdminClient.create(config);

        } catch (IOException e) {

            System.out.println("config file is missing, exiting app.");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Fatal Error :(");
            alert.setContentText("config file is missing!");

            alert.showAndWait();
            Platform.exit();
            return;


        }

        //showing text input dialog for "Login screen" setting the client name
        //if name is empty string ask again until valid name is entered
        do {

            TextInputDialog dialog = new TextInputDialog("my_name_" + System.currentTimeMillis());
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
            dialog.setTitle("Login");
            dialog.setHeaderText("Please choose a name to log in (Spaces and empty String is not allowed)");
            dialog.setContentText("Please enter your name:");


            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (!(result.get().equals("")) && !result.get().contains(" ")) {
                    stop = true;
                    this.clientName = result.get();
                    clientNameLabel.setText(clientNameLabel.getText() + clientName);

                    System.out.println("Your name: " + clientName);

                } else {
                    System.out.println("illegal name, no spaces or empty name");
                }

            }
        } while (!stop);

        //creating kafka producer with client name.
        producer = new SimpleProducer(clientName, BOOTSTRAP_SERVERS, this);

        //setting Default file save location - support windows or linux
        if(System.getProperty("file.separator").toLowerCase().contains("\\")) {
            outDir = System.getProperty("user.home") + "\\Downloads";//for os with \ separator
        }else{
            outDir = System.getProperty("user.home") + "/Downloads";//for os with / separator
        }

    }

    /**
     * clientName Getter
     *
     * @return client name
     */
    String getClientName() {
        return clientName;
    }

    /**
     * Boot Strap server getter
     *
     * @return server ip and port as a string "ip:port"
     */
    String GetBootStrapServer() {
        return BOOTSTRAP_SERVERS;
    }

    /**
     *
     */
    public String getOutDir(){ return outDir; }
}