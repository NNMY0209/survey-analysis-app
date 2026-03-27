package com.example.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.app.dao.AdminDao;

@Service
public class AuthService {

    private final AdminDao adminDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdminDao adminDao, PasswordEncoder passwordEncoder) {
        this.adminDao = adminDao;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String loginId, String userName, String password) {
        String passwordHash = passwordEncoder.encode(password);
        adminDao.insertAdmin(loginId, userName, passwordHash);
    }
}