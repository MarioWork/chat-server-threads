package org.academiadecodigo.altcatras65;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private final int PORT_NUMBER;
    private ServerSocket serverSocket;
    private LinkedList<ClientDispatcher> clients;

    public ChatServer(int PORT_NUMBER) {
        this.PORT_NUMBER = PORT_NUMBER;
        this.clients = new LinkedList<>();
    }

    public void listen() {

        try {

            //Create server socket
            this.serverSocket = new ServerSocket(this.PORT_NUMBER);

            ExecutorService executorService = Executors.newCachedThreadPool();

            while (true) {
                //Create a client socket by accepting new connection
                Socket socket = this.serverSocket.accept();
                ClientDispatcher clientDispatcher = new ClientDispatcher(socket);
                executorService.submit(clientDispatcher);
                clients.add(clientDispatcher);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void dispatch(ClientDispatcher client) {

        try {
            //Receive the message from the client socket
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getClientSocket().getInputStream()));


            //Send to everyone that there is a new client
            String messageConnection = Thread.currentThread().getName() + " has connected.";
            System.err.println(messageConnection);
            messageAll(messageConnection);

            while (client.getClientSocket().isBound()) {
                // Save the incoming message
                String message = in.readLine();

                //Change the name of the thread if receives a message with /name command
                if (!isCommand(message, client)) {

                    //Save the message with the client name
                    message = Thread.currentThread().getName() + ": " + message;

                    //Log the message
                    System.out.println(message);

                    //send the Message to every client
                    messageAll(message);
                }
            }

            //Create the message
            String messageDisconnect = Thread.currentThread().getName() + " Disconnected.";

            //Log the message
            System.err.println(messageDisconnect);

            //Send the message to every client
            messageAll(messageDisconnect);

            //Remove the socket from the list
            this.clients.remove(client);

            //Close the connection
            client.getClientSocket().close();


        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    private void messageAll(String message) {
        for (ClientDispatcher client : this.clients) {
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
            executeNameCommand(message, client);
            return true;

        } else if (message.startsWith("/quit")) {
            executeQuitCommand(client);
            return true;

        } else if (message.startsWith("/list")) {
            for (ClientDispatcher clientDispatcher : clients) {
                sendMessage(clientDispatcher.getName(), client);
                return true;
            }
        }

        return false;
    }

    private void executeQuitCommand(ClientDispatcher client) {
        try {
            //Create the message
            String messageName = Thread.currentThread().getName() + " Disconnected.";

            //Log the message
            System.err.println(messageName);

            //Send the message to every client
            messageAll(messageName);

            //Remove the socket from the list
            this.clients.remove(client);

            //Close the connection
            client.getClientSocket().close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeNameCommand(String message, ClientDispatcher client) {
        //Create the message
        String messageName = Thread.currentThread().getName() + " changed his name.";

        //Log the message
        System.err.println(messageName);

        //Send the message to every client
        messageAll(messageName);

        //Change the Client name
        String newName = message.substring(6);
        client.setName(newName);
    }

    private class ClientDispatcher implements Runnable {
        private Socket clientSocket;
        private String name;

        public ClientDispatcher(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            //Get the index of the thread number
            int clientNumberIndex = Thread.currentThread().getName().lastIndexOf("-") + 1;

            //Get the thread number
            String clientNumber = Thread.currentThread().getName().substring(clientNumberIndex);

            //Set the new name
            setName("Client " + clientNumber);

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
        }
    }

}
