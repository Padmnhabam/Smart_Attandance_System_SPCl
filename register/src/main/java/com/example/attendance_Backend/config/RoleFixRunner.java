package com.example.attendance_Backend.config;

import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleFixRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    public RoleFixRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Checking for users with missing roles...");
        List<User> users = userRepository.findAll();
        long fixCount = 0;

        for (User user : users) {
            // If user has a roll number and role is null or "user" (default from
            // AuthService login logic sometimes),
            // set it to "STUDENT" so they show up in the teacher reports.
            if (user.getRollNo() != null && (user.getRole() == null || user.getRole().equalsIgnoreCase("user"))) {
                user.setRole("STUDENT");
                userRepository.save(user);
                fixCount++;
            }
        }

        if (fixCount > 0) {
            System.out.println("Successfully updated " + fixCount + " students with the correct 'STUDENT' role.");
        } else {
            System.out.println("No student roles needed fixing.");
        }
    }
}
