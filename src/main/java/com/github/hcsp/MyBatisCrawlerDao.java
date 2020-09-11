package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        String resource = "db/myBatis/mybatis-config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public synchronized String getNextLinkAndDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String currentLink = session.selectOne("com.github.hcsp.MyMapper.selectCurrentLink");
            if (currentLink != null) {
                session.delete("com.github.hcsp.MyMapper.deleteLink", currentLink);
            }
            return currentLink;
        }
    }

    @Override
    public void updateNewsInfoIntoDatabase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertNews", new News(url, title, content));
        }
    }

    @Override
    public synchronized boolean linksHasBeenProcessed(String currentLink) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String linkInProcessedTable = session.selectOne("com.github.hcsp.MyMapper.judgeLinkHasBeenProcessed", currentLink);
            return linkInProcessedTable != null;
        }
    }

    @Override
    public void insertAlreadyProcessedLink(String currentLink) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "LINKS_ALREADY_PROCESSED");
        param.put("link", currentLink);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLink", param);
        }
    }

    @Override
    public void insertToBeProcessedLink(String href) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "LINKS_TO_BE_PROCESSED");
        param.put("link", href);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLink", param);
        }
    }
}
