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
import java.sql.SQLException;
import java.util.stream.Collectors;

public class Crawler extends Thread {
    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    @Override
    public void run() {
        try {
            String currentLink;

            while ((currentLink = dao.getNextLinkAndDelete()) != null) {

                if (!dao.linksHasBeenProcessed(currentLink)) {

                    if (isInterestedLink(currentLink)) {
                        Document doc = HttpGetAndParseHtml(currentLink);

                        obtainRelatedLinksAndUpdateIntoDatabase(doc);

                        obtainNewsInfoAndUpdateIntoDatabase(doc, currentLink);

                        dao.insertAlreadyProcessedLink(currentLink);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInterestedLink(String currentLink) {
        return isNewsLink(currentLink) && !isLoginLink(currentLink) && !isRollNewsLink(currentLink) || "https://sina.cn".equals(currentLink);
    }

    private Document HttpGetAndParseHtml(String currentLink) throws IOException {
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

    private void obtainRelatedLinksAndUpdateIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (!href.isEmpty() && !href.toLowerCase().startsWith("javascript")) {
                dao.insertToBeProcessedLink(href);
            }
        }
    }

    private void obtainNewsInfoAndUpdateIntoDatabase(Document doc, String currentLink) throws SQLException {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element e : articleTags) {
                String title = e.child(0).text();
                String content = e.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                String url = currentLink;

                dao.updateNewsInfoIntoDatabase(url, title, content);
            }
        }
    }

    private boolean isRollNewsLink(String currentLink) {
        return currentLink.contains("roll.d.html");
    }

    private boolean isLoginLink(String currentLink) {
        return currentLink.contains("passport.sina.cn") || currentLink.contains("passport.weibo.com");
    }

    private boolean isNewsLink(String currentLink) {
        return currentLink.contains("news.sina.cn");
    }
}
