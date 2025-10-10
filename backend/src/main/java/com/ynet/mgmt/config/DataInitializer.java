package com.ynet.mgmt.config;

import com.ynet.mgmt.user.entity.User;
import com.ynet.mgmt.user.entity.UserStatus;
import com.ynet.mgmt.user.repository.UserRepository;
import com.ynet.mgmt.role.entity.Role;
import com.ynet.mgmt.role.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时创建默认管理员用户
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
    }

    /**
     * 初始化默认用户
     */
    private void initializeDefaultUsers() {
        try {
            // 检查是否已存在管理员用户
            if (userRepository.findByUsername("admin").isEmpty()) {
                createDefaultAdmin();
            }

            // 检查是否已存在测试用户
            if (userRepository.findByUsername("user").isEmpty()) {
                createDefaultUser();
            }

            logger.info("数据初始化完成");
        } catch (Exception e) {
            logger.error("数据初始化失败", e);
        }
    }

    /**
     * 创建默认管理员用户
     */
    private void createDefaultAdmin() {
        // 查找或创建管理员角色
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("ADMIN");
                    role.setName("管理员");
                    role.setDescription("系统管理员角色");
                    return roleRepository.save(role);
                });

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setFullName("系统管理员");
        admin.setCustomRole(adminRole);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);

        userRepository.save(admin);
        logger.info("创建默认管理员用户: username=admin, password=admin");
    }

    /**
     * 创建默认普通用户
     */
    private void createDefaultUser() {
        // 查找或创建普通用户角色
        Role userRole = roleRepository.findByCode("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("USER");
                    role.setName("普通用户");
                    role.setDescription("普通用户角色");
                    return roleRepository.save(role);
                });

        User user = new User();
        user.setUsername("user");
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("user"));
        user.setFullName("测试用户");
        user.setCustomRole(userRole);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        userRepository.save(user);
        logger.info("创建默认普通用户: username=user, password=user");
    }
}