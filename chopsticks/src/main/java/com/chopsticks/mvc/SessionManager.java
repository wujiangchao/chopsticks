package com.chopsticks.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.chopsticks.mvc.http.Session;
/**
 * manager web sessions
 * @author jack
 *
 */
public class SessionManager {
	/**
     * Store all Session instances
     */
    private Map<String, Session> sessionMap;

    /**
     * Create SessionManager
     */
    public SessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
    }

    /**
     * Get a Session instance based on the Session id
     *
     * @param id session id
     * @return Session instance
     */
    public Session getSession(String id) {
        return sessionMap.get(id);
    }

    /**
     * Add a session instance to sessionMap
     *
     * @param session session instance
     */
    public void addSession(Session session) {
        sessionMap.put(session.id(), session);
    }

    /**
     * Clean all session instances
     */
    public void clear() {
        sessionMap.clear();
    }

    /**
     * Remove a session
     *
     * @param session session instance
     */
    public void remove(Session session) {
        sessionMap.remove(session.id());
}
}
