package com.nutalig.config;

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

    public String getLineMessageApiUrl() {
        return message.getApi();
    }

    public String getLineChannelAccessToken() {
        return channel.getAccessToken();
    }

    public String getLineChannelSecret() {
        return channel.getSecret();
    }

    public String getLineChannelId() {
        return channel.getId();
    }

    public String getVerifyAccessTokenUrl() {
        return login.getVerifyAccessTokenUrl();
    }

    public String getAuthorizeUrl() {
        return login.getAuthorizeUrl();
    }

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
