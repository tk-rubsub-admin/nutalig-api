package com.nutalig.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    @PostConstruct
    public void test() {
        System.out.println("=== LINE CONFIG ===");
        System.out.println("line.message.api = " + getLineMessageApiUrl());
        System.out.println("line.message.channel.id = " + getLineMessageChannelId());
        System.out.println("line.message.channel.secret = " + getLineMessageChannelSecret());
        System.out.println("line.message.access-token = " + getLineMessageAccessToken());
        System.out.println("line.channel.id = " + getLineChannelId());
        System.out.println("line.channel.secret = " + getLineChannelSecret());
        System.out.println("line.channel.access-token = " + getLineChannelAccessToken());
        System.out.println("line.login.authorize-url = " + getAuthorizeUrl());
        System.out.println("line.login.token-url = " + getTokenUrl());
        System.out.println("line.login.verify-access-token-url = " + getVerifyAccessTokenUrl());
        System.out.println("line.login.verify-id-token-url = " + getVerifyIdTokenUrl());
        System.out.println("line.login.profile-url = " + getProfileUrl());
        System.out.println("line.login.redirect-uri = " + getRedirectUri());
        System.out.println("line.login.scope = " + getScope());
        System.out.println("line.login.login-success-url = " + getLoginSuccessUrl());
        System.out.println("line.login.login-failure-url = " + getLoginFailureUrl());
        System.out.println("line.login.link-success-url = " + getLinkSuccessUrl());
        System.out.println("line.login.link-failure-url = " + getLinkFailureUrl());
        System.out.println("===================");
    }

}
