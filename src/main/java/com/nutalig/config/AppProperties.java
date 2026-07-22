package com.nutalig.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Upload upload = new Upload();
    private final RfqPendingAcceptance rfqPendingAcceptance = new RfqPendingAcceptance();

    @Getter
    @Setter
    public static class Upload {
        private String dir;
        private String publicBaseUrl;
    }

    @Getter
    @Setter
    public static class RfqPendingAcceptance {
        private boolean enabled = true;
        private String cron = "0 0 10 * * *";
        private int startOffsetDays = 2;
        private int endOffsetDays = 1;
        private LocalTime startTime = LocalTime.of(17, 0);
        private LocalTime endTime = LocalTime.of(17, 0);
    }
}
