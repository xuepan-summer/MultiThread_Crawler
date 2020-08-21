package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.stream.Collectors;

public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:E:/Crawler_Project/MultiThread_Crawler/news", "root", "root");
        String currentLink;
        while ((currentLink = getNextLinkAndDelete(connection)) != null) {

            if (!linksHasBeenProcessed(connection, currentLink)) {

                if (isInterestedLink(currentLink)) {
                    Document doc = HttpGetAndParseHtml(currentLink);

                    obtainRelatedLinksAndUpdateIntoDatabase(connection, doc);

                    obtainNewsInfoAndUpdateIntoDatabase(connection, doc, currentLink);

                    updateIntoDatabase(connection, "insert into LINKS_ALREADY_PROCESSED (link) values (?)", currentLink);
                }
            }
        }
    }

    private static String getNextLinkAndDelete(Connection connection) throws SQLException {
        String currentLink = getNextLink(connection, "select * from LINKS_TO_BE_PROCESSED LIMIT 1");
        if (currentLink != null) {
            updateIntoDatabase(connection, "delete from LINKS_TO_BE_PROCESSED where link = ?", currentLink);
        }
        return currentLink;
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

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        String result = null;
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result = resultSet.getString(1);
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
            System.out.println(currentLink);
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity, "utf-8");
            doc = Jsoup.parse(html);
        }
        return doc;
    }

    private static void obtainRelatedLinksAndUpdateIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (!href.isEmpty() && !href.toLowerCase().startsWith("javascript")) {
                updateIntoDatabase(connection, "insert into LINKS_TO_BE_PROCESSED (link) values (?)", href);
            }
        }
    }

    private static void obtainNewsInfoAndUpdateIntoDatabase(Connection connection, Document doc, String currentLink) throws SQLException {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element e : articleTags) {
                String title = e.child(0).text();
                String content = e.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                String url = currentLink;

                try (PreparedStatement statement = connection.prepareStatement("insert into news (url,title,content,created_at,updated_at) values (?,?,?,now(),now())")) {
                    statement.setString(1, url);
                    statement.setString(2, title);
                    statement.setString(3, content);
                    statement.executeUpdate();
                }
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
