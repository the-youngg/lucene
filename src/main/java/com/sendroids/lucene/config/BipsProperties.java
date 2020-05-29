package com.sendroids.lucene.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "bips")
@Setter
@Getter
public class BipsProperties {

    @Value("${bips.upload-location}")
    private String uploadLocation;
    @Value("${bips.index-location}")
    private String indexLocation;
    @Value("${bips.buy-location}")
    private String buyLocation;
    @Value("${bips.sell-location}")
    private String sellLocation;
    @Value("${bips.buy-location-project}")
    private String projectBuyLocation;// index-dir/Buy/project/201710
    @Value("${bips.sell-location-project}")
    private String projectSellLocation;
    @Value("${bips.buy-location-file}")
    private String fileBuyLocation; // index-dir/Buy/file/201710
    @Value("${bips.sell-location-file}")
    private String fileSellLocation;

    private final long indexMatchReadMaxSize = 1024 * 1024 * 1024;// 1024^3 Byte = 1GB
    private final int indexMatchReadMaxMonth = 12;
    private final int matchMaxNumber = 10;
    private final int matchMaxSendProject = 10;
    private final boolean autoSendMatchResult = false;
    private final int sendEmailUnReadMsgIntervalHour = 24;

    private int maxProfanityLevel;
    private Set<String> disallowedWords;
    private Destinations destinations;

    @Getter
    @Setter
    // 注意要保持类是public。
    public static class Destinations {
        private String login;
        private String logout;
    }

    @Setter
    @Getter
    public static class Security {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>(Collections.singleton("USER"));
    }

}
