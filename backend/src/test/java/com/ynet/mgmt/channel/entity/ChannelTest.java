package com.ynet.mgmt.channel.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Channel实体类单元测试
 * 测试Channel实体的创建、业务方法、验证注解等功能
 *
 * @author system
 * @since 1.0.0
 */
@DisplayName("Channel实体测试")
class ChannelTest {

    private Validator validator;
    private Channel channel;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // 创建测试用的渠道对象
        channel = new Channel("测试渠道", "TEST_CHANNEL", ChannelType.ONLINE);
        channel.setDescription("测试渠道描述");
        channel.setContactName("张三");
        channel.setContactPhone("13800138000");
        channel.setContactEmail("test@example.com");
        channel.setAddress("北京市朝阳区");
        channel.setCommissionRate(new BigDecimal("0.0500"));
        channel.setMonthlyTarget(new BigDecimal("100000.00"));
        channel.setSortOrder(1);
    }

    @Nested
    @DisplayName("实体创建测试")
    class CreationTests {

        @Test
        @DisplayName("使用默认构造函数创建渠道")
        void testDefaultConstructor() {
            Channel emptyChannel = new Channel();

            assertNull(emptyChannel.getId());
            assertNull(emptyChannel.getName());
            assertNull(emptyChannel.getCode());
            assertNull(emptyChannel.getType());
            assertEquals(ChannelStatus.INACTIVE, emptyChannel.getStatus());
            assertEquals(BigDecimal.ZERO, emptyChannel.getCommissionRate());
            assertEquals(BigDecimal.ZERO, emptyChannel.getMonthlyTarget());
            assertEquals(BigDecimal.ZERO, emptyChannel.getTotalSales());
            assertEquals(BigDecimal.ZERO, emptyChannel.getCurrentMonthSales());
            assertEquals(Integer.valueOf(0), emptyChannel.getSortOrder());
        }

        @Test
        @DisplayName("使用参数构造函数创建渠道")
        void testParameterizedConstructor() {
            Channel testChannel = new Channel("新渠道", "NEW_CHANNEL", ChannelType.OFFLINE);

            assertEquals("新渠道", testChannel.getName());
            assertEquals("NEW_CHANNEL", testChannel.getCode());
            assertEquals(ChannelType.OFFLINE, testChannel.getType());
            assertEquals(ChannelStatus.INACTIVE, testChannel.getStatus());
        }

        @Test
        @DisplayName("设置所有属性后验证")
        void testFullChannelCreation() {
            assertNotNull(channel);
            assertEquals("测试渠道", channel.getName());
            assertEquals("TEST_CHANNEL", channel.getCode());
            assertEquals(ChannelType.ONLINE, channel.getType());
            assertEquals("测试渠道描述", channel.getDescription());
            assertEquals("张三", channel.getContactName());
            assertEquals("13800138000", channel.getContactPhone());
            assertEquals("test@example.com", channel.getContactEmail());
            assertEquals("北京市朝阳区", channel.getAddress());
            assertEquals(new BigDecimal("0.0500"), channel.getCommissionRate());
            assertEquals(new BigDecimal("100000.00"), channel.getMonthlyTarget());
            assertEquals(Integer.valueOf(1), channel.getSortOrder());
        }
    }

    @Nested
    @DisplayName("业务逻辑方法测试")
    class BusinessLogicTests {

        @Test
        @DisplayName("状态检查方法测试")
        void testStatusCheckMethods() {
            // 测试默认状态INACTIVE
            assertFalse(channel.isActive());
            assertFalse(channel.isSuspended());
            assertFalse(channel.isDeleted());

            // 测试ACTIVE状态
            channel.setStatus(ChannelStatus.ACTIVE);
            assertTrue(channel.isActive());
            assertFalse(channel.isSuspended());
            assertFalse(channel.isDeleted());

            // 测试SUSPENDED状态
            channel.setStatus(ChannelStatus.SUSPENDED);
            assertFalse(channel.isActive());
            assertTrue(channel.isSuspended());
            assertFalse(channel.isDeleted());

            // 测试DELETED状态
            channel.setStatus(ChannelStatus.DELETED);
            assertFalse(channel.isActive());
            assertFalse(channel.isSuspended());
            assertTrue(channel.isDeleted());
        }

        @ParameterizedTest
        @EnumSource(ChannelType.class)
        @DisplayName("类型检查方法测试")
        void testTypeCheckMethods(ChannelType type) {
            channel.setType(type);

            switch (type) {
                case ONLINE:
                    assertTrue(channel.isOnline());
                    assertFalse(channel.isOffline());
                    assertFalse(channel.isHybrid());
                    break;
                case OFFLINE:
                    assertFalse(channel.isOnline());
                    assertTrue(channel.isOffline());
                    assertFalse(channel.isHybrid());
                    break;
                case HYBRID:
                    assertFalse(channel.isOnline());
                    assertFalse(channel.isOffline());
                    assertTrue(channel.isHybrid());
                    break;
            }
        }

        @Test
        @DisplayName("激活渠道方法测试")
        void testActivateChannel() {
            LocalDateTime beforeActivation = LocalDateTime.now();

            channel.activate();

            assertEquals(ChannelStatus.ACTIVE, channel.getStatus());
            assertNotNull(channel.getLastActivityAt());
            assertTrue(channel.getLastActivityAt().isAfter(beforeActivation) ||
                      channel.getLastActivityAt().equals(beforeActivation));
        }

        @Test
        @DisplayName("暂停渠道方法测试")
        void testSuspendChannel() {
            LocalDateTime beforeSuspension = LocalDateTime.now();

            channel.suspend();

            assertEquals(ChannelStatus.SUSPENDED, channel.getStatus());
            assertNotNull(channel.getLastActivityAt());
            assertTrue(channel.getLastActivityAt().isAfter(beforeSuspension) ||
                      channel.getLastActivityAt().equals(beforeSuspension));
        }

        @Test
        @DisplayName("删除渠道方法测试")
        void testMarkAsDeleted() {
            LocalDateTime beforeDeletion = LocalDateTime.now();

            channel.markAsDeleted();

            assertEquals(ChannelStatus.DELETED, channel.getStatus());
            assertNotNull(channel.getLastActivityAt());
            assertTrue(channel.getLastActivityAt().isAfter(beforeDeletion) ||
                      channel.getLastActivityAt().equals(beforeDeletion));
        }

        @Test
        @DisplayName("更新活动时间方法测试")
        void testUpdateActivity() {
            assertNull(channel.getLastActivityAt());
            LocalDateTime beforeUpdate = LocalDateTime.now();

            channel.updateActivity();

            assertNotNull(channel.getLastActivityAt());
            assertTrue(channel.getLastActivityAt().isAfter(beforeUpdate) ||
                      channel.getLastActivityAt().equals(beforeUpdate));
        }

        @Test
        @DisplayName("更新销售数据方法测试")
        void testUpdateSales() {
            BigDecimal initialTotalSales = channel.getTotalSales();
            BigDecimal initialCurrentMonthSales = channel.getCurrentMonthSales();
            BigDecimal salesAmount = new BigDecimal("10000.00");

            channel.updateSales(salesAmount);

            assertEquals(initialTotalSales.add(salesAmount), channel.getTotalSales());
            assertEquals(initialCurrentMonthSales.add(salesAmount), channel.getCurrentMonthSales());
            assertNotNull(channel.getLastActivityAt());
        }

        @Test
        @DisplayName("更新销售数据方法测试 - 无效金额")
        void testUpdateSalesWithInvalidAmount() {
            BigDecimal initialTotalSales = channel.getTotalSales();
            BigDecimal initialCurrentMonthSales = channel.getCurrentMonthSales();

            // 测试null金额
            channel.updateSales(null);
            assertEquals(initialTotalSales, channel.getTotalSales());
            assertEquals(initialCurrentMonthSales, channel.getCurrentMonthSales());

            // 测试负数金额
            channel.updateSales(new BigDecimal("-1000.00"));
            assertEquals(initialTotalSales, channel.getTotalSales());
            assertEquals(initialCurrentMonthSales, channel.getCurrentMonthSales());

            // 测试零金额
            channel.updateSales(BigDecimal.ZERO);
            assertEquals(initialTotalSales, channel.getTotalSales());
            assertEquals(initialCurrentMonthSales, channel.getCurrentMonthSales());
        }

        @Test
        @DisplayName("计算佣金金额方法测试")
        void testGetCommissionAmount() {
            BigDecimal salesAmount = new BigDecimal("10000.00");
            BigDecimal expectedCommission = new BigDecimal("500.0000"); // 10000 * 0.0500

            BigDecimal actualCommission = channel.getCommissionAmount(salesAmount);

            assertEquals(0, expectedCommission.compareTo(actualCommission));
        }

        @Test
        @DisplayName("计算佣金金额方法测试 - 边界条件")
        void testGetCommissionAmountBoundaryConditions() {
            // 测试null销售金额
            assertEquals(BigDecimal.ZERO, channel.getCommissionAmount(null));

            // 测试负数销售金额
            assertEquals(BigDecimal.ZERO, channel.getCommissionAmount(new BigDecimal("-1000.00")));

            // 测试零销售金额
            assertEquals(BigDecimal.ZERO, channel.getCommissionAmount(BigDecimal.ZERO));

            // 测试null佣金率
            channel.setCommissionRate(null);
            assertEquals(BigDecimal.ZERO, channel.getCommissionAmount(new BigDecimal("10000.00")));
        }

        @Test
        @DisplayName("计算月度目标完成度方法测试")
        void testGetMonthlyTargetCompletion() {
            // 设置当月销售额为50000，月度目标为100000
            channel.setCurrentMonthSales(new BigDecimal("50000.00"));

            BigDecimal completion = channel.getMonthlyTargetCompletion();
            BigDecimal expected = new BigDecimal("0.5000"); // 50000 / 100000

            assertEquals(0, expected.compareTo(completion));
        }

        @Test
        @DisplayName("计算月度目标完成度方法测试 - 边界条件")
        void testGetMonthlyTargetCompletionBoundaryConditions() {
            // 测试零月度目标
            channel.setMonthlyTarget(BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, channel.getMonthlyTargetCompletion());

            // 测试null月度目标
            channel.setMonthlyTarget(null);
            assertEquals(BigDecimal.ZERO, channel.getMonthlyTargetCompletion());

            // 测试null当月销售额
            channel.setMonthlyTarget(new BigDecimal("100000.00"));
            channel.setCurrentMonthSales(null);
            assertEquals(BigDecimal.ZERO, channel.getMonthlyTargetCompletion());
        }

        @Test
        @DisplayName("检查是否完成月度目标方法测试")
        void testIsTargetAchieved() {
            // 设置当月销售额为100000，月度目标为100000
            channel.setCurrentMonthSales(new BigDecimal("100000.00"));
            assertTrue(channel.isTargetAchieved());

            // 设置当月销售额为150000，月度目标为100000
            channel.setCurrentMonthSales(new BigDecimal("150000.00"));
            assertTrue(channel.isTargetAchieved());

            // 设置当月销售额为50000，月度目标为100000
            channel.setCurrentMonthSales(new BigDecimal("50000.00"));
            assertFalse(channel.isTargetAchieved());
        }

        @Test
        @DisplayName("检查是否完成月度目标方法测试 - 边界条件")
        void testIsTargetAchievedBoundaryConditions() {
            // 测试零月度目标
            channel.setMonthlyTarget(BigDecimal.ZERO);
            channel.setCurrentMonthSales(new BigDecimal("10000.00"));
            assertFalse(channel.isTargetAchieved());

            // 测试null月度目标
            channel.setMonthlyTarget(null);
            assertFalse(channel.isTargetAchieved());

            // 测试null当月销售额
            channel.setMonthlyTarget(new BigDecimal("100000.00"));
            channel.setCurrentMonthSales(null);
            assertFalse(channel.isTargetAchieved());
        }

        @Test
        @DisplayName("重置当月销售额方法测试")
        void testResetCurrentMonthSales() {
            channel.setCurrentMonthSales(new BigDecimal("50000.00"));
            assertNotEquals(BigDecimal.ZERO, channel.getCurrentMonthSales());

            channel.resetCurrentMonthSales();

            assertEquals(BigDecimal.ZERO, channel.getCurrentMonthSales());
            assertNotNull(channel.getLastActivityAt());
        }
    }

    @Nested
    @DisplayName("验证注解测试")
    class ValidationTests {

        @Test
        @DisplayName("有效渠道对象验证")
        void testValidChannel() {
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertTrue(violations.isEmpty(), "有效的渠道对象不应有验证错误");
        }

        @Test
        @DisplayName("渠道名称验证测试")
        void testNameValidation() {
            // 测试空名称
            channel.setName("");
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));

            // 测试null名称
            channel.setName(null);
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));

            // 测试过长名称
            channel.setName("a".repeat(101));
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        }

        @Test
        @DisplayName("渠道代码验证测试")
        void testCodeValidation() {
            // 测试空代码
            channel.setCode("");
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("code")));

            // 测试null代码
            channel.setCode(null);
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("code")));

            // 测试无效格式代码
            channel.setCode("invalid-code!");
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("code")));

            // 测试过短代码
            channel.setCode("a");
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("code")));
        }

        @Test
        @DisplayName("联系邮箱验证测试")
        void testContactEmailValidation() {
            // 测试无效邮箱格式
            channel.setContactEmail("invalid-email");
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("contactEmail")));

            // 测试过长邮箱
            channel.setContactEmail("a".repeat(90) + "@example.com");
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("contactEmail")));
        }

        @Test
        @DisplayName("佣金率验证测试")
        void testCommissionRateValidation() {
            // 测试负数佣金率
            channel.setCommissionRate(new BigDecimal("-0.1000"));
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("commissionRate")));

            // 测试超过100%的佣金率
            channel.setCommissionRate(new BigDecimal("1.1000"));
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("commissionRate")));
        }

        @Test
        @DisplayName("类型和状态验证测试")
        void testTypeAndStatusValidation() {
            // 测试null类型
            channel.setType(null);
            Set<ConstraintViolation<Channel>> violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));

            // 测试null状态
            channel.setType(ChannelType.ONLINE);
            channel.setStatus(null);
            violations = validator.validate(channel);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        }
    }

    @Nested
    @DisplayName("equals和hashCode测试")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("equals方法测试")
        void testEquals() {
            // 测试相同对象
            assertEquals(channel, channel);

            // 测试null对象
            assertNotEquals(channel, null);

            // 测试不同类型对象
            assertNotEquals(channel, "string");

            // 测试相同ID和代码的对象
            Channel other = new Channel();
            other.setId(1L);
            other.setCode("TEST_CHANNEL");
            channel.setId(1L);
            assertEquals(channel, other);

            // 测试不同ID的对象
            other.setId(2L);
            assertNotEquals(channel, other);

            // 测试不同代码的对象
            other.setId(1L);
            other.setCode("DIFFERENT_CODE");
            assertNotEquals(channel, other);
        }

        @Test
        @DisplayName("hashCode方法测试")
        void testHashCode() {
            Channel other = new Channel();
            other.setId(1L);
            other.setCode("TEST_CHANNEL");
            channel.setId(1L);

            assertEquals(channel.hashCode(), other.hashCode());

            other.setId(2L);
            assertNotEquals(channel.hashCode(), other.hashCode());
        }
    }

    @Nested
    @DisplayName("toString方法测试")
    class ToStringTests {

        @Test
        @DisplayName("toString方法测试")
        void testToString() {
            channel.setId(1L);
            String toString = channel.toString();

            assertNotNull(toString);
            assertTrue(toString.contains("Channel{"));
            assertTrue(toString.contains("id=1"));
            assertTrue(toString.contains("name='测试渠道'"));
            assertTrue(toString.contains("code='TEST_CHANNEL'"));
            assertTrue(toString.contains("type=ONLINE"));
            assertTrue(toString.contains("status=INACTIVE"));
        }
    }
}