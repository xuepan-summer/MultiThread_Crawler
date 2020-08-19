package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:E:/Crawler_Project/MultiThread_Crawler/news", "root", "root");
        while (true) {
            List<String> linkPool = executeSelectSql(connection, "select * from LINKS_TO_BE_PROCESSED");

            if (!linkPool.isEmpty()) {
                String currentLink = linkPool.remove(linkPool.size() - 1);

                updateIntoDatabase(connection, "delete from LINKS_TO_BE_PROCESSED where link = ?", currentLink);

                if (!linksHasBeenProcessed(connection, currentLink)) {
                    if (isInterestedLink(currentLink)) {
                        Document doc = HttpGetAndParseHtml(currentLink);

                        obtainRelatedLinksAndUpdateIntoDatabase(connection, linkPool, doc);
                        obtainNewsTitle(doc);
                        updateIntoDatabase(connection, "insert into LINKS_ALREADY_PROCESSED (link) values (?)", currentLink);
                    }
                }
            }
        }
    }

    private static boolean isInterestedLink(String currentLink) {
        return isNewsLink(currentLink) && !isLoginLink(currentLink) && !isRollNewsLink(currentLink) || "https://sina.cn".equals(currentLink);
    }

    private static boolean linksHasBeenProcessed(Connection connection, String currentLink) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?")) {
            statement.setString(1, currentLink);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    private static void updateIntoDatabase(Connection connection, String sql, String currentLink) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentLink);
            statement.executeUpdate();
        }
    }

    private static List<String> executeSelectSql(Connection connection, String sql) throws SQLException {
        List<String> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        }
    }

    private static Document HttpGetAndParseHtml(String currentLink) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(currentLink);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0");
        Document doc;

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            System.out.println(currentLink);
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity, "utf-8");
            doc = Jsoup.parse(html);
        }
        return doc;
    }

    private static void obtainRelatedLinksAndUpdateIntoDatabase(Connection connection, List<String> linkPool, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            linkPool.add(href);

            updateIntoDatabase(connection, "insert into LINKS_TO_BE_PROCESSED (link) values (?)", href);
        }
    }

    private static void obtainNewsTitle(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element e : articleTags) {
                System.out.println(e.child(0).text());
            }
        }
    }

    private static boolean isRollNewsLink(String currentLink) {
        return currentLink.contains("roll.d.html");
    }


    private static boolean isLoginLink(String currentLink) {
        return currentLink.contains("passport.sina.cn") || currentLink.contains("passport.weibo.com");
    }


    private static boolean isNewsLink(String currentLink) {
        return currentLink.contains("news.sina.cn");
    }
}
