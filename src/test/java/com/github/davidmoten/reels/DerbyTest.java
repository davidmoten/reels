package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

public class DerbyTest {

    @Test
    public void test() throws SQLException {
        // we do this so that derby gets tested for JDK 8 as well as higher
        try (Connection con = DriverManager.getConnection("jdbc:derby:memory:db;create=true");
                PreparedStatement ps = con.prepareStatement("select count(*) from SYSIBM.SYSDUMMY1");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

}
