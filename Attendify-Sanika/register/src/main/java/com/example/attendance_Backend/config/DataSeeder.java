package com.example.attendance_Backend.config;

import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initData(AdminRepository adminRepository,
            com.example.attendance_Backend.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Admin User
            if (adminRepository.findByEmail("admin@attendify.com").isEmpty()) {
                Admin admin = new Admin();
                admin.setName("Admin Director");
                admin.setEmail("admin@attendify.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                adminRepository.save(admin);
                System.out.println("✅ Seeded default admin account: admin@attendify.com / admin123");
            }
        };
    }
}
