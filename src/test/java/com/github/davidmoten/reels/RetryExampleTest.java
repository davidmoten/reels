package com.github.davidmoten.reels;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryExampleTest {

    private static final Logger log = LoggerFactory.getLogger(RetryExampleTest.class);
    
    private static final long RESTART_INTERVAL_MS= 300;

    @Test
    public void demonstrateJdbcRetriesAndRestartsWithSpecialSupervisor() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context //
                .builder() //
                .supervisor(createJdbcSupervisor()) //
                .scheduler(Scheduler.io()) //
                .build();
        CountDownLatch latch = new CountDownLatch(2);
        ActorRef<String> actor = context.actorClass(Query.class, latch).build();
        actor.tell("run");
        actor.tell("error");
        actor.tell("again");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        log.info("shutdown");
    }

    private static Supervisor createJdbcSupervisor() {
        return (message, actor, error) -> {
            log.warn(error.getMessage());
            if (error.getCause() != null && error.getCause() instanceof SQLTransientException) {
                actor.retry();
                actor.pause(RESTART_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } else if (error.getCause() != null
                    && error.getCause() instanceof SQLNonTransientConnectionException) {
                // connection failed so retry the message and recreate the connection
                actor.retry();
                actor.pauseAndRestart(RESTART_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } else {
                // don't retry the message but pause a bit anyway
                actor.pauseAndRestart(RESTART_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
        };
    }

    public static final class Query extends AbstractActor<String> {

        private static final Logger log = LoggerFactory.getLogger(Query.class);

        private final CountDownLatch latch;
        private Connection con;
        private PreparedStatement ps;
        
        public Query(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void preStart(ActorRef<String> self) {
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
                    latch.countDown();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onStop(ActorRef<String> self) {
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
