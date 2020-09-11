package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockData {
    private static final int EXPECTED_NEWS_NUMBER = 100_0000;

    public static void main(String[] args) {
        String resource = "db/myBatis/mybatis-config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        MockData(sqlSessionFactory);
    }

    private static void MockData(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<News> newsList = session.selectList("com.github.hcsp.MockDataMapper.selectAllNews");
            int count = EXPECTED_NEWS_NUMBER - newsList.size();
            try {
                while (count-- > 0) {
                    int randomNum = new Random().nextInt(newsList.size());
                    News randomNews = new News(newsList.get(randomNum));

                    Instant currentTime = randomNews.getCreatedAt().minusSeconds(new Random().nextInt(3600 * 24 * 365));
                    randomNews.setCreatedAt(currentTime);
                    randomNews.setUpdatedAt(currentTime);

                    session.insert("com.github.hcsp.MockDataMapper.insertIntoNews", randomNews);
                    System.out.println("left:" + count);
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }
}
