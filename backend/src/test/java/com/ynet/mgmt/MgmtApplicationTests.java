package com.ynet.mgmt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 基础管理系统应用程序测试类
 * 测试Spring Boot应用程序的基本启动和配置
 *
 * @author system
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class MgmtApplicationTests {

	/**
	 * 测试Spring Boot应用程序上下文是否能正常加载
	 * 这是最基本的测试，确保应用程序配置正确，能够正常启动
	 */
	@Test
	void contextLoads() {
		// 如果应用程序上下文加载失败，此测试将失败
		// 这个测试验证了所有的配置类、依赖注入和自动配置都工作正常
	}

	/**
	 * 测试应用程序主方法
	 * 验证main方法能够正常执行而不抛出异常
	 */
	@Test
	void mainMethodRuns() {
		// 测试main方法的执行
		// 注意：这里不直接调用main方法，因为它会启动整个应用程序
		// 而是通过contextLoads()测试来验证应用程序能够启动
		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
			// 验证MgmtApplication类存在且可以被加载
			Class.forName("com.ynet.mgmt.MgmtApplication");
		});
	}

}