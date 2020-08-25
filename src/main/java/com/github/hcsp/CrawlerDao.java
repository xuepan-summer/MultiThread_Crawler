package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {

    String getNextLinkAndDelete() throws SQLException;

    void updateNewsInfoIntoDatabase(String url, String title, String content) throws SQLException;

    boolean linksHasBeenProcessed(String currentLink) throws SQLException;

    void insertAlreadyProcessedLink(String currentLink) throws SQLException;

    void insertToBeProcessedLink(String href) throws SQLException;
}
