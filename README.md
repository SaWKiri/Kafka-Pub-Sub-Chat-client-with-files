# Pub/Sub Client with Kafka


## Description:

 This app demonstrate the usage of Apache Kafka.

## Installation and Configuration:

### Kafka chat Client config.txt configuration:

The file contains only one line, the line must contain only one broker ip and port. Ip is in ipv4 format only after the ip must be &quot;:&quot; character then the port number e.g. 123.123.123.123:9090. This is the only characters in the line. Rest of the file will be ignored. The config file must be located in the same project or jar file in order to run the chat client.

### User Interface:

The user interface is implemented with javafx. When u open the client a login window will appear. In the login window the user must type username with **no space**. Empty username is **not allowed**, no password requierd.

After typing valid user name the main GUI window will appear on the screen.

On the top of the window u can see a quit Button and the client username. For safely exit the application please use this Button and not the X Button.

In the middle of the window you can see big white rectangle, message will appear in that window.

Bellow that you can see topic and message input boxes, here u can type the topic and message to send to kafka brokers. Sending message is possible by inputting a topic and message in the input box and pressing/clicking the &quot;Send&quot; Button. Empty topic is not allowed but empty message is.

The next line you have one input box and two Button register and leave, you can register to a topic by filling the desired topic and pressing register to topic and leave to leave that topic, topic to register/leave must not be empty to register/leave.

### Kafka server (zookeeper and brokers) instruction:

Required files (can be found in kafka broker\_zookeeper folder)

  a. KB0.config
  b. KB1.config
  c. KB2.config
  d. ZS.config

In order to run Kafka, the zookeeper config file need to contain the port number the Kafka zookeeper listen on. By default, the port is 2181 in general the port can stay the same.

In all three-broker file KB0 KB1 and KB2 config file need to have the correct zookeeper ip and port, if u change the port it must be updated in all of this files. When u choose a PC to run the zookeeper, you must update the ip in the broker config file. the input need to be in the following format &quot;ip:port&quot;

the broker id in the broker configuration file must be **unique** across all running brokers. You require to change them accordingly.

next configuration must be changed is the port and ip the broker listen on.

the broker ip and port must be updated in the client config file for each client connected to its borker.

If you running all brokers in the same PC you must change the directory of there logs files, you can simply add &quot;-number&quot; and change the number with the broker id value. It&#39;s must be unique file name if placing all logs in the same folder.

Running the Zookeeper and Brokers:

In order to run the zookeeper and Brokers in windows OS open cmd and navigate to the folder kafka\_2.11-1.0.0.

To run the zookeeper, type the command:
~~~
bin\windows\zookeeper-server-start.bat config\ZS.properties
~~~
To run the Brokers, type the command:
~~~
bin\windows\kafka-server-start.bat config\KB0.properties

bin\windows\kafka-server-start.bat config\KB1.properties

bin\windows\kafka-server-start.bat config\KB2.properties
~~~


To find witch broker is the leader on a specific topic, type:
~~~
bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic topic
~~~
where localhost:2181 is the ip and port of the zookeeper, this command you need to run in the PC that the zookeeper running on.


### Compiling the code in eclipse:

In order to open the project in eclipse u must open it and import the project by doing file -&gt; Import under General choose existing Project into workspace.

Press Browse and navigate to the project location click on the project folder and press confirm.

Make sure the project is tic and press Finish.

after the project is load you might need to re-define the Kafka library jars in the project. To do right click on the project name in the project explorer and open Project Properties (shortcut keys alt+enter). In the properties window press the Java Build Path and choose the tab Libraries.

on the right press the Add External JARS…, jar selection window will appear navigate to the project directory. In the project folder, there is a folder called &quot;libs&quot; containing all the necessary jars of the Kafka. select all of them and press open.



press apply and ok Button. Now you can Run the project with the run Button.
