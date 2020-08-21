package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {

    void updateIntoDatabase(String sql, String currentLink) throws SQLException;

    String getNextLink(String sql) throws SQLException;

    void updateNewsInfoIntoDatabase(String url, String title, String content) throws SQLException;

    boolean linksHasBeenProcessed(String currentLink) throws SQLException;
}
