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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> linkPool = new ArrayList<>();
        linkPool.add("https://sina.cn");
        Set<String> handledLink = new HashSet<>();
        while (!linkPool.isEmpty()) {
            String currentLink = linkPool.remove(linkPool.size() - 1);

            if (handledLink.contains(currentLink)) {
                continue;
            }
            if (currentLink.startsWith("//")) {
                currentLink = "https:" + currentLink;
            }
            //我们想要的新浪的链接，则处理(拿到链接)
            if (currentLink.contains("news.sina.cn")
                    && !(currentLink.contains("passport.sina.cn") && !(currentLink.contains("passport.weibo.com")))
                    && !currentLink.contains("roll.d.html")
                    || "https://sina.cn".equals(currentLink)) {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(currentLink);
                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0");

                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    System.out.println(response.getStatusLine());
                    System.out.println(currentLink);

                    HttpEntity entity = response.getEntity();
                    String html = EntityUtils.toString(entity, "utf-8");

                    Document doc = Jsoup.parse(html);
                    Elements aTags = doc.select("a");
                    for (Element aTag : aTags) {
                        linkPool.add(aTag.attr("href"));
                    }

                    Elements articleTags = doc.select("article");
                    if (!articleTags.isEmpty()) {
                        for (Element e : articleTags) {
                            System.out.println(e.child(0).text());
                        }
                    }
                    handledLink.add(currentLink);
                }
            }
        }
    }
}

