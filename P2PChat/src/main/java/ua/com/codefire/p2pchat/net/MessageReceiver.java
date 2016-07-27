/*
 * Copyright (C) 2016 CodeFireUA <edu@codefire.com.ua>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ua.com.codefire.p2pchat.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CodeFireUA <edu@codefire.com.ua>
 */
public class MessageReceiver implements Runnable {

    private final ServerSocket serverSocket;
    private State state = State.STOPPED;
    private List<MessageListener> listeners;

    public MessageReceiver(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.serverSocket.setSoTimeout(1000);

        this.listeners = Collections.synchronizedList(new ArrayList<MessageListener>());
    }

    protected State getState() {
        return state;
    }

    protected void setState(State state) {
        this.state = state;
    }

    public boolean add(MessageListener listener) {
        return listeners.add(listener);
    }

    public boolean remove(MessageListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void run() {
        setState(State.STARTED);

        while (getState().equals(State.STARTED)) {
            try (Socket client = serverSocket.accept()) {
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                
                String address = client.getInetAddress().getHostAddress();
                String message = dis.readUTF();
                
                dos.writeUTF("OK");
                dos.flush();
                
                for (MessageListener listener : listeners) {
                    listener.messageIncome(address, message);
                }
            } catch (SocketTimeoutException ex) {
                ; // NOOP
            } catch (IOException ex) {
                Logger.getLogger(MessageReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        setState(State.STOPPED);
    }

    public synchronized boolean start() {
        switch (state) {
            case STOPPED:
                new Thread(this).start();
                break;
        }

        return false;
    }

    public synchronized boolean stop() {
        switch (state) {
            case STARTED:
                setState(State.STOPPING);
                break;
        }

        return false;
    }

    public enum State {
        STOPPED, STARTED, STOPPING;
    }
}
