package com.nutalig.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

@Slf4j
@Setter
@Getter
@ConfigurationProperties(prefix = "line")
public class LineConfiguration {

    private final Message message = new Message();
    private final Channel channel = new Channel();
    private final Login login = new Login();

    public String getLineMessageApiUrl() { return message.getApi(); }

    public String getLineMessageChannelId() { return message.getChannel().getId(); }

    public String getLineMessageChannelSecret() { return message.getChannel().getSecret(); }

    public String getLineMessageAccessToken() { return message.getChannel().getAccessToken(); }

    public String getLineChannelAccessToken() {
        return channel.getAccessToken();
    }

    public String getLineChannelSecret() {
        return channel.getSecret();
    }

    public String getLineChannelId() { return channel.getId(); }

    public String getVerifyAccessTokenUrl() {
        return login.getVerifyAccessTokenUrl();
    }

    public String getAuthorizeUrl() { return login.getAuthorizeUrl(); }

    public String getTokenUrl() {
        return login.getTokenUrl();
    }

    public String getVerifyIdTokenUrl() {
        return login.getVerifyIdTokenUrl();
    }

    public String getProfileUrl() {
        return login.getProfileUrl();
    }

    public String getRedirectUri() {
        return login.getRedirectUri();
    }

    public String getScope() {
        return login.getScope();
    }

    public String getLoginSuccessUrl() {
        return login.getLoginSuccessUrl();
    }

    public String getLoginFailureUrl() {
        return login.getLoginFailureUrl();
    }

    public String getLinkSuccessUrl() {
        return login.getLinkSuccessUrl();
    }

    public String getLinkFailureUrl() {
        return login.getLinkFailureUrl();
    }

    @PostConstruct
    public void logResolvedConfiguration() {
        log.info(
                "Loaded line config: messageApi={}, messageChannelId={}, channelId={}, loginAuthorizeUrl={}, tokenUrl={}, redirectUri={}, scope={}, successUrl={}, failureUrl={}",
                safe(message.getApi()),
                safe(message.getChannel() != null ? message.getChannel().getId() : null),
                safe(channel.getId()),
                safe(login.getAuthorizeUrl()),
                safe(login.getTokenUrl()),
                safe(login.getRedirectUri()),
                safe(login.getScope()),
                safe(login.getLoginSuccessUrl()),
                safe(login.getLoginFailureUrl())
        );
        log.debug(
                "Loaded line config secrets: messageAccessTokenLen={}, channelAccessTokenLen={}, channelSecretLen={}, messageChannelSecretLen={}",
                length(message.getChannel() != null ? message.getChannel().getAccessToken() : null),
                length(channel.getAccessToken()),
                length(channel.getSecret()),
                length(message.getChannel() != null ? message.getChannel().getSecret() : null)
        );
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        if (value.length() <= 8) {
            return value;
        }

        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    @Getter
    @Setter
    public static class Message {
        private String api;
        private Channel channel;

    }

    @Getter
    @Setter
    public static class Channel {
        private String id;
        private String accessToken;
        private String secret;
    }

    @Getter
    @Setter
    public static class Login {
        private String authorizeUrl;
        private String tokenUrl;
        private String verifyAccessTokenUrl;
        private String verifyIdTokenUrl;
        private String profileUrl;
        private String redirectUri;
        private String scope;
        private String loginSuccessUrl;
        private String loginFailureUrl;
        private String linkSuccessUrl;
        private String linkFailureUrl;
    }

    
}
