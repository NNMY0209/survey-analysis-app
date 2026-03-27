package com.example.app.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.app.dao.AdminDao;
import com.example.app.dto.AdminDto;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminDao adminDao;

    public AdminUserDetailsService(AdminDao adminDao) {
        this.adminDao = adminDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminDto admin = adminDao.findByLoginId(username);

        if (admin == null) {
            throw new UsernameNotFoundException("admin not found: " + username);
        }

        return new LoginAdminUser(admin);
    }
}