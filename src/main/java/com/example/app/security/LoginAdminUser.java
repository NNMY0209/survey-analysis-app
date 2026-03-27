package com.example.app.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.example.app.dto.AdminDto;

public class LoginAdminUser extends User {

    private final Long userId;
    private final String userName;

    public LoginAdminUser(AdminDto admin) {
        super(
                admin.getLoginId(),
                admin.getPasswordHash(),
                admin.getIsActive(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole()))
        );
        this.userId = admin.getUserId();
        this.userName = admin.getUserName();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}