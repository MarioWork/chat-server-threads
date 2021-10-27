package org.academiadecodigo.altcatras65;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private final int PORT_NUMBER;
    private ServerSocket serverSocket;
    private final String DEFAULT_NAME = "Client ";
    private Map<ClientDispatcher, String> clients;

    public ChatServer(int PORT_NUMBER) {
        this.PORT_NUMBER = PORT_NUMBER;
        this.clients = new ConcurrentHashMap<>();
    }

    public void listen() {

        try {
            long connections = 0;

            //Create server socket
            this.serverSocket = new ServerSocket(this.PORT_NUMBER);

            ExecutorService executorService = Executors.newCachedThreadPool();

            while (true) {
                //Create a client socket by accepting new connection
                Socket socket = this.serverSocket.accept();


                ClientDispatcher clientDispatcher = new ClientDispatcher(socket);

                //Set default name of the client
                clientDispatcher.setName(DEFAULT_NAME+connections);

                //Send to everyone that there is a new client connected
                String messageConnection = clientDispatcher.getName() + " has connected.";
                System.err.println(messageConnection);
                broadCast(messageConnection);

                connections++;

                //create a thread
                executorService.submit(clientDispatcher);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void dispatch(ClientDispatcher client) {

        try {
            //Receive the message from the client socket
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getClientSocket().getInputStream()));


            while (client.getClientSocket().isBound()) {
                // Save the incoming message
                String message = in.readLine();

                //Change the name of the thread if receives a message with /name command
                if (!isCommand(message, client)) {

                    //Save the message with the client name
                    message = client.getName() + ": " + message;

                    //Log the message
                    System.out.println(message);

                    //send the Message to every client
                    broadCast(message);
                }
            }

            //Create the message
            String messageDisconnect = Thread.currentThread().getName() + " Disconnected.";

            //Log the message
            System.err.println(messageDisconnect);

            //Send the message to every client
            broadCast(messageDisconnect);

            //Remove the socket from the list
            this.clients.remove(client);

            //Close the connection
            client.getClientSocket().close();


        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    private synchronized void broadCast(String message) {
        for (ClientDispatcher client : this.clients.keySet()) {
            try {
                //To write a new message to the clientSocket
                PrintWriter outClient = new PrintWriter(client.getClientSocket().getOutputStream(), true);

                //Send the message back
                outClient.println(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(String message, ClientDispatcher client) {
        try {
            PrintWriter out = new PrintWriter(client.getClientSocket().getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isCommand(String message, ClientDispatcher client) {

        if (message.startsWith("/name")) {
            System.err.println(client.getName() + ": Executing the command /quit");
            executeChangeNameCommand(message, client);
            return true;

        } else if (message.startsWith("/quit")) {
            System.err.println(client.getName() + ": Executing the command /quit");
            executeQuitCommand(client);
            return true;

        } else if (message.startsWith("/list")) {
            System.err.println(client.getName() + ": Executing the command /list");
            executeCommandList(client);
            return true;
        } else if (message.startsWith("/whisper")) {
            System.err.println(client.getName() + ": Executing the command /whisper");
            executeCommandWhisper(message);
            return true;

        }
        return false;
    }

    private synchronized void executeCommandList(ClientDispatcher client) {
        for (ClientDispatcher clientDispatcher : this.clients.keySet()) {
            sendMessage(clientDispatcher.getName(), client);
        }
    }

    private void executeQuitCommand(ClientDispatcher client) {
        try {
            //Create the message
            String messageName = client.getName() + " Disconnected.";

            //Log the message
            System.err.println(messageName);

            //Send the message to every client
            broadCast(messageName);

            //Remove the socket from the list
            this.clients.remove(client);

            //Close the connection
            client.getClientSocket().close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeChangeNameCommand(String message, ClientDispatcher client) {
        //Create the message
        String messageName = client.getName() + " changed his name.";

        //Log the message
        System.err.println(messageName);

        //Send the message to every client
        broadCast(messageName);

        //Change the Client name
        String newName = message.substring(6);
        client.setName(newName);
    }

    private void executeCommandWhisper(String message) {
        String[] commands = message.split("/");
        String sender = commands[1].substring(7).trim();
        String msg = commands[2].substring(4);
        sendMessage(msg, getClientByName(sender));
    }

    private synchronized ClientDispatcher getClientByName(String name) {
        for (ClientDispatcher clientDispatcher : clients.keySet()) {
            if (clientDispatcher.getName().equals(name)) {
                return clientDispatcher;
            }
        }
        return null;
    }

    private class ClientDispatcher implements Runnable {
        private Socket clientSocket;
        private String name;

        public ClientDispatcher(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            clients.put(this, this.name);
            dispatch(this);
        }

        public Socket getClientSocket() {
            return clientSocket;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            Thread.currentThread().setName(this.name);
            clients.replace(this, name);
        }
    }

}
