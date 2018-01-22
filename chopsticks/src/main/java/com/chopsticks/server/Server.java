package com.chopsticks.server;

import com.chopsticks.Chopsticks;

public interface Server {

    /**
     * Start blade application
     *
     * @param blade blade instance
     * @param args  command arguments
     * @throws Exception
     */
    void start(Chopsticks chopsticks, String[] args) throws Exception;

    /**
     * Join current server
     *
     * @throws Exception
     */
    void join() throws Exception;

    /**
     * Stop current server
     */
    void stop();

    /**
     * Stop current, Will have been waiting for the service to stop
     */
    void stopAndWait();

}
