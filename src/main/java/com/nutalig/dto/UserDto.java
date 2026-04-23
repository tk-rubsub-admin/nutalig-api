package com.nutalig.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@Data
public class UserDto implements Serializable {

    private String id;
    private String username;
    private UserRoleDto role;
    private String status;
    private ZonedDateTime createdDate;
    private List<String> permissions;
    private String lineUserId;
    private String displayName;
    private String pictureUrl;
    private String employeeId;
    @JsonIgnore
    private List<GrantedAuthority> authorities;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}
