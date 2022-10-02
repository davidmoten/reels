package com.github.davidmoten.reels;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryExampleMain {

    private static final Logger log = LoggerFactory.getLogger(RetryExampleMain.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context //
                .builder() //
                .supervisor((message, actor, error) -> {
                    log.warn(error.getMessage());
                    long intervalSeconds = 1;
                    if (error.getCause() != null && error.getCause() instanceof SQLTransientException) {
                        actor.retry();
                        actor.pause(intervalSeconds, TimeUnit.SECONDS);
                    } else if (error.getCause() != null
                            && error.getCause() instanceof SQLNonTransientConnectionException) {
                        // connection failed so retry the message and recreate the connection
                        actor.retry();
                        actor.pauseAndRestart(intervalSeconds, TimeUnit.SECONDS);
                    } else {
                        actor.pauseAndRestart(intervalSeconds, TimeUnit.SECONDS);
                    }
                }).build();

        ActorRef<String> actor = context.createActor(Query.class);
        actor.tell("run");
        actor.tell("error");
        actor.tell("again");
        Thread.sleep(10000);
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        log.info("shutdown");
        Scheduler.forkJoin().shutdown();
    }

    public static final class Query extends AbstractActor<String> {

        private static final Logger log = LoggerFactory.getLogger(Query.class);

        private Connection con;
        private PreparedStatement ps;

        @Override
        public void preStart(Context context) {
            log.info("preStart");
            try {
                con = DriverManager.getConnection("jdbc:derby:memory:db;create=true");
                ps = con.prepareStatement("select count(*) from SYSIBM.SYSDUMMY1");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            log.info("preStart finished");
        }

        @Override
        public void onMessage(Message<String> message) {
            if (message.content().equals("error")) {
                throw new RuntimeException("boo");
            }
            try {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    log.info("returned {}", rs.getObject(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onStop(Context context) {
            log.info("onStop");
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    log.warn(e.getMessage());
                }
            }
            log.info("onStop finished");
        }

    }

}
