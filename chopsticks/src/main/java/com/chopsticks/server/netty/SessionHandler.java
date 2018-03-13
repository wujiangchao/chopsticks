package com.chopsticks.server.netty;

import java.time.Instant;

import com.chopsticks.Chopsticks;
import com.chopsticks.event.EventManager;
import com.chopsticks.event.EventType;
import com.chopsticks.kit.ReflectKit;
import com.chopsticks.kit.UUID;
import com.chopsticks.mvc.SessionManager;
import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.http.Cookie;
import com.chopsticks.mvc.http.Request;
import com.chopsticks.mvc.http.Response;
import com.chopsticks.mvc.http.Session;
import static com.chopsticks.mvc.Const.*;
/**
 * session handler
 *
 * 2017/6/3
 */
public class SessionHandler {

    private final Chopsticks     chopsticks;
    private final SessionManager sessionManager;
    private final EventManager   eventManager;
    private final String         sessionKey;
    private final int            timeout;

    public SessionHandler(Chopsticks chopsticks) {
        this.chopsticks = chopsticks;
        this.sessionManager = chopsticks.sessionManager();
        this.eventManager = chopsticks.eventManager();
        this.sessionKey = chopsticks.environment().get(ENV_KEY_SESSION_KEY, HttpConst.DEFAULT_SESSION_KEY);
        this.timeout = chopsticks.environment().getInt(ENV_KEY_SESSION_TIMEOUT, 1800);
    }

    public Session createSession(Request request) {
        Session  session  = getSession(request);
        Response response = WebContext.response();
        if (null == session) {
            return createSession(request, response);
        } else {
            if (session.expired() < Instant.now().getEpochSecond()) {
                removeSession(session);
            }
        }
        return session;
    }

    private Session createSession(Request request, Response response) {

        long now     = Instant.now().getEpochSecond();
        long expired = now + timeout;

        String sessionId = UUID.UU32();
        Cookie cookie    = new Cookie();
        cookie.setName(sessionKey);
        cookie.setValue(sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());

        Session session = ReflectKit.newInstance(chopsticks.sessionType());
        session.id(sessionId);
        session.created(now);
        session.expired(expired);
        sessionManager.addSession(session);

        request.cookie(cookie);
        response.cookie(cookie);

        eventManager.fireEvent(EventType.SESSION_CREATED, chopsticks);

        return session;
    }

    private void removeSession(Session session) {
        session.attributes().clear();
        sessionManager.remove(session);
        eventManager.fireEvent(EventType.SESSION_DESTROY, chopsticks);
    }

    private Session getSession(Request request) {
        String cookieHeader = request.cookie(sessionKey);
        if (null == cookieHeader) {
            return null;
        }
        return sessionManager.getSession(cookieHeader);
    }

}
