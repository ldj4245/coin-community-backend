package com.coincommunity.backend.service;

import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.repository.PostRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.net.URLConnection;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsPersistenceService {

    private static final String NEWS_RSS_URL = "https://kr.cointelegraph.com/rss";
    private final PostRepository postRepository;

    @Scheduled(initialDelay = 10000, fixedRate = 3600000) // 앱 시작 10초 후 첫 실행, 이후 1시간마다 반복
    @Transactional
    public void fetchAndSaveNews() {
        log.info("Starting scheduled job: Fetching and saving news from RSS feed.");
        try {
            URL feedUrl = new URL(NEWS_RSS_URL);
            URLConnection connection = feedUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            SyndFeedInput input = new SyndFeedInput();
            input.setXmlHealerOn(true);
            SyndFeed feed = input.build(new XmlReader(connection));

            List<SyndEntry> entries = feed.getEntries();
            log.info("Found {} news entries in the RSS feed.", entries.size());

            for (SyndEntry entry : entries) {
                String sourceUrl = entry.getLink();
                if (!postRepository.existsBySourceUrl(sourceUrl)) {
                    Post post = convertRssEntryToPost(entry);
                    postRepository.save(post);
                    log.info("Saved new article: {}", post.getTitle());
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled news fetching job.", e);
        }
        log.info("Finished scheduled job: Fetching and saving news.");
    }

    private Post convertRssEntryToPost(SyndEntry entry) {
        String description = entry.getDescription() != null ? entry.getDescription().getValue() : "";
        description = description.replaceAll("<[^>]*>", "").trim();

        return Post.builder()
                .title(entry.getTitle())
                .content(description)
                .category(PostCategory.NEWS)
                .source("코인텔레그래프 코리아")
                .sourceUrl(entry.getLink())
                .user(null) // 시스템이 작성한 글이므로 사용자는 null
                .build();
    }
} 