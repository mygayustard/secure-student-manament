package com.example.studentmanagement.config;

import com.example.studentmanagement.user.Role;
import com.example.studentmanagement.user.User;
import com.example.studentmanagement.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SeedUser admin;
	private final SeedUser teacher;

	public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
						   @Value("${app.seed.admin.username}") String adminUsername,
						   @Value("${app.seed.admin.email}") String adminEmail,
						   @Value("${app.seed.admin.password}") String adminPassword,
						   @Value("${app.seed.teacher.username}") String teacherUsername,
						   @Value("${app.seed.teacher.email}") String teacherEmail,
						   @Value("${app.seed.teacher.password}") String teacherPassword) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.admin = new SeedUser(adminUsername, adminEmail, adminPassword, Role.ADMIN);
		this.teacher = new SeedUser(teacherUsername, teacherEmail, teacherPassword, Role.TEACHER);
	}

	@Override
	public void run(ApplicationArguments args) {
		createIfMissing(admin);
		createIfMissing(teacher);
	}

	private void createIfMissing(SeedUser seed) {
		if (userRepository.existsByRolesContaining(seed.role())) {
			return;
		}
		if (userRepository.existsByUsernameIgnoreCase(seed.username())
				|| userRepository.existsByEmailIgnoreCase(seed.email())) {
			throw new IllegalStateException("Configured seed account conflicts with an existing user");
		}
		User user = new User();
		user.setUsername(seed.username());
		user.setEmail(seed.email());
		user.setPassword(passwordEncoder.encode(seed.password()));
		user.getRoles().add(seed.role());
		userRepository.save(user);
	}

	private record SeedUser(String username, String email, String password, Role role) {
	}
}
