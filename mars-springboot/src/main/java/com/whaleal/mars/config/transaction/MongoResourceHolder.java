package com.whaleal.mars.config.transaction;

import com.mongodb.client.ClientSession;
import com.whaleal.mars.core.Mars;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * @author lyz
 * @desc
 * @create: 2022-10-31 16:32
 **/


public class MongoResourceHolder extends ResourceHolderSupport {
    @Nullable
    private ClientSession session;

    MongoResourceHolder(Mars mars) {
        this.session = mars.startSession();
    }

    @Nullable
    ClientSession getSession() {
        return this.session;
    }

    ClientSession getRequiredSession() {
        ClientSession session = this.getSession();
        if (session == null) {
            throw new IllegalStateException("No session available!");
        } else {
            return session;
        }
    }

    public void setSession(@Nullable ClientSession session) {
        this.session = session;
    }

    void setTimeoutIfNotDefaulted(int seconds) {
        if (seconds != -1) {
            this.setTimeoutInSeconds(seconds);
        }

    }

    boolean hasSession() {
        return this.session != null;
    }

    boolean hasActiveSession() {
        if (!this.hasSession()) {
            return false;
        } else {
            return this.hasServerSession() && !this.getRequiredSession().getServerSession().isClosed();
        }
    }

    boolean hasActiveTransaction() {
        return !this.hasActiveSession() ? false : this.getRequiredSession().hasActiveTransaction();
    }

    boolean hasServerSession() {
        try {
            return this.getRequiredSession().getServerSession() != null;
        } catch (IllegalStateException var2) {
            return false;
        }
    }
}

