package com.ynet.mgmt.channel.repository;

import com.ynet.mgmt.channel.entity.Channel;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChannelRepository单元测试
 * 测试渠道Repository的各种查询方法和功能
 *
 * @author system
 * @since 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChannelRepository测试")
class ChannelRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChannelRepository repository;

    private Channel activeOnlineChannel;
    private Channel inactiveOfflineChannel;
    private Channel suspendedHybridChannel;
    private Channel deletedChannel;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        // 创建活跃的线上渠道
        activeOnlineChannel = new Channel("活跃线上渠道", "ACTIVE_ONLINE", ChannelType.ONLINE);
        activeOnlineChannel.setStatus(ChannelStatus.ACTIVE);
        activeOnlineChannel.setDescription("主要的线上销售渠道");
        activeOnlineChannel.setContactName("张三");
        activeOnlineChannel.setContactPhone("13800138000");
        activeOnlineChannel.setContactEmail("zhangsan@example.com");
        activeOnlineChannel.setAddress("北京市朝阳区");
        activeOnlineChannel.setCommissionRate(new BigDecimal("0.0500"));
        activeOnlineChannel.setMonthlyTarget(new BigDecimal("100000.00"));
        activeOnlineChannel.setTotalSales(new BigDecimal("250000.00"));
        activeOnlineChannel.setCurrentMonthSales(new BigDecimal("80000.00"));
        activeOnlineChannel.setLastActivityAt(now.minusDays(1));
        activeOnlineChannel.setSortOrder(1);
        // 手动设置审计字段
        activeOnlineChannel.setCreatedAt(now.minusDays(7));
        activeOnlineChannel.setUpdatedAt(now.minusDays(1));
        activeOnlineChannel.setCreatedBy("test");
        activeOnlineChannel.setUpdatedBy("test");
        activeOnlineChannel = entityManager.persistAndFlush(activeOnlineChannel);

        // 创建未激活的线下渠道
        inactiveOfflineChannel = new Channel("未激活线下渠道", "INACTIVE_OFFLINE", ChannelType.OFFLINE);
        inactiveOfflineChannel.setStatus(ChannelStatus.INACTIVE);
        inactiveOfflineChannel.setDescription("新开设的线下门店");
        inactiveOfflineChannel.setContactName("李四");
        inactiveOfflineChannel.setContactPhone("13900139000");
        inactiveOfflineChannel.setContactEmail("lisi@example.com");
        inactiveOfflineChannel.setAddress("上海市浦东新区");
        inactiveOfflineChannel.setCommissionRate(new BigDecimal("0.0300"));
        inactiveOfflineChannel.setMonthlyTarget(new BigDecimal("80000.00"));
        inactiveOfflineChannel.setTotalSales(new BigDecimal("0.00"));
        inactiveOfflineChannel.setCurrentMonthSales(new BigDecimal("0.00"));
        inactiveOfflineChannel.setSortOrder(2);
        // 手动设置审计字段
        inactiveOfflineChannel.setCreatedAt(now.minusDays(5));
        inactiveOfflineChannel.setUpdatedAt(now.minusDays(5));
        inactiveOfflineChannel.setCreatedBy("test");
        inactiveOfflineChannel.setUpdatedBy("test");
        inactiveOfflineChannel = entityManager.persistAndFlush(inactiveOfflineChannel);

        // 创建暂停的混合渠道
        suspendedHybridChannel = new Channel("暂停混合渠道", "SUSPENDED_HYBRID", ChannelType.HYBRID);
        suspendedHybridChannel.setStatus(ChannelStatus.SUSPENDED);
        suspendedHybridChannel.setDescription("暂停运营的混合渠道");
        suspendedHybridChannel.setContactName("王五");
        suspendedHybridChannel.setContactPhone("13700137000");
        suspendedHybridChannel.setContactEmail("wangwu@example.com");
        suspendedHybridChannel.setAddress("广州市天河区");
        suspendedHybridChannel.setCommissionRate(new BigDecimal("0.0400"));
        suspendedHybridChannel.setMonthlyTarget(new BigDecimal("120000.00"));
        suspendedHybridChannel.setTotalSales(new BigDecimal("150000.00"));
        suspendedHybridChannel.setCurrentMonthSales(new BigDecimal("20000.00"));
        suspendedHybridChannel.setLastActivityAt(now.minusDays(5));
        suspendedHybridChannel.setSortOrder(3);
        // 手动设置审计字段
        suspendedHybridChannel.setCreatedAt(now.minusDays(3));
        suspendedHybridChannel.setUpdatedAt(now.minusDays(5));
        suspendedHybridChannel.setCreatedBy("test");
        suspendedHybridChannel.setUpdatedBy("test");
        suspendedHybridChannel = entityManager.persistAndFlush(suspendedHybridChannel);

        // 创建已删除的渠道
        deletedChannel = new Channel("已删除渠道", "DELETED_CHANNEL", ChannelType.ONLINE);
        deletedChannel.setStatus(ChannelStatus.DELETED);
        deletedChannel.setDescription("已删除的渠道");
        deletedChannel.setCommissionRate(new BigDecimal("0.0200"));
        deletedChannel.setSortOrder(4);
        // 手动设置审计字段
        deletedChannel.setCreatedAt(now.minusDays(2));
        deletedChannel.setUpdatedAt(now.minusDays(2));
        deletedChannel.setCreatedBy("test");
        deletedChannel.setUpdatedBy("test");
        deletedChannel = entityManager.persistAndFlush(deletedChannel);

        entityManager.clear();
    }

    @Nested
    @DisplayName("基础查询方法测试")
    class BasicQueryTests {

        @Test
        @DisplayName("根据代码查找渠道")
        void testFindByCode() {
            Optional<Channel> found = repository.findByCode("ACTIVE_ONLINE");
            assertTrue(found.isPresent());
            assertEquals("活跃线上渠道", found.get().getName());

            Optional<Channel> notFound = repository.findByCode("NON_EXISTENT");
            assertFalse(notFound.isPresent());
        }

        @Test
        @DisplayName("根据名称查找渠道")
        void testFindByName() {
            Optional<Channel> found = repository.findByName("活跃线上渠道");
            assertTrue(found.isPresent());
            assertEquals("ACTIVE_ONLINE", found.get().getCode());

            Optional<Channel> notFound = repository.findByName("不存在的渠道");
            assertFalse(notFound.isPresent());
        }

        @Test
        @DisplayName("根据状态查找渠道")
        void testFindByStatus() {
            List<Channel> activeChannels = repository.findByStatus(ChannelStatus.ACTIVE);
            assertEquals(1, activeChannels.size());
            assertEquals("ACTIVE_ONLINE", activeChannels.get(0).getCode());

            List<Channel> inactiveChannels = repository.findByStatus(ChannelStatus.INACTIVE);
            assertEquals(1, inactiveChannels.size());
            assertEquals("INACTIVE_OFFLINE", inactiveChannels.get(0).getCode());

            List<Channel> suspendedChannels = repository.findByStatus(ChannelStatus.SUSPENDED);
            assertEquals(1, suspendedChannels.size());
            assertEquals("SUSPENDED_HYBRID", suspendedChannels.get(0).getCode());

            List<Channel> deletedChannels = repository.findByStatus(ChannelStatus.DELETED);
            assertEquals(1, deletedChannels.size());
            assertEquals("DELETED_CHANNEL", deletedChannels.get(0).getCode());
        }

        @Test
        @DisplayName("根据类型查找渠道")
        void testFindByType() {
            List<Channel> onlineChannels = repository.findByType(ChannelType.ONLINE);
            assertEquals(2, onlineChannels.size());

            List<Channel> offlineChannels = repository.findByType(ChannelType.OFFLINE);
            assertEquals(1, offlineChannels.size());
            assertEquals("INACTIVE_OFFLINE", offlineChannels.get(0).getCode());

            List<Channel> hybridChannels = repository.findByType(ChannelType.HYBRID);
            assertEquals(1, hybridChannels.size());
            assertEquals("SUSPENDED_HYBRID", hybridChannels.get(0).getCode());
        }
    }

    @Nested
    @DisplayName("组合查询方法测试")
    class CombinationQueryTests {

        @Test
        @DisplayName("根据状态和类型查找渠道")
        void testFindByStatusAndType() {
            List<Channel> activeOnline = repository.findByStatusAndType(ChannelStatus.ACTIVE, ChannelType.ONLINE);
            assertEquals(1, activeOnline.size());
            assertEquals("ACTIVE_ONLINE", activeOnline.get(0).getCode());

            List<Channel> inactiveOffline = repository.findByStatusAndType(ChannelStatus.INACTIVE, ChannelType.OFFLINE);
            assertEquals(1, inactiveOffline.size());
            assertEquals("INACTIVE_OFFLINE", inactiveOffline.get(0).getCode());

            List<Channel> activeOffline = repository.findByStatusAndType(ChannelStatus.ACTIVE, ChannelType.OFFLINE);
            assertEquals(0, activeOffline.size());
        }

        @Test
        @DisplayName("根据状态列表查找渠道")
        void testFindByStatusIn() {
            List<ChannelStatus> statuses = Arrays.asList(ChannelStatus.ACTIVE, ChannelStatus.SUSPENDED);
            List<Channel> channels = repository.findByStatusIn(statuses);

            assertEquals(2, channels.size());
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("ACTIVE_ONLINE")));
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("SUSPENDED_HYBRID")));
        }

        @Test
        @DisplayName("根据状态和类型分页查找渠道")
        void testFindByStatusAndTypeWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Channel> page = repository.findByStatusAndType(ChannelStatus.ACTIVE, ChannelType.ONLINE, pageable);

            assertEquals(1, page.getTotalElements());
            assertEquals(1, page.getContent().size());
            assertEquals("ACTIVE_ONLINE", page.getContent().get(0).getCode());
        }
    }

    @Nested
    @DisplayName("排序查询方法测试")
    class SortingQueryTests {

        @Test
        @DisplayName("根据状态查找并按排序顺序排序")
        void testFindByStatusOrderBySortOrder() {
            // 创建多个相同状态的渠道以测试排序
            LocalDateTime now = LocalDateTime.now();

            Channel channel1 = new Channel("测试渠道1", "TEST1", ChannelType.ONLINE);
            channel1.setStatus(ChannelStatus.ACTIVE);
            channel1.setSortOrder(5);
            channel1.setCreatedAt(now);
            channel1.setUpdatedAt(now);
            channel1.setCreatedBy("test");
            channel1.setUpdatedBy("test");
            entityManager.persistAndFlush(channel1);

            Channel channel2 = new Channel("测试渠道2", "TEST2", ChannelType.ONLINE);
            channel2.setStatus(ChannelStatus.ACTIVE);
            channel2.setSortOrder(3);
            channel2.setCreatedAt(now);
            channel2.setUpdatedAt(now);
            channel2.setCreatedBy("test");
            channel2.setUpdatedBy("test");
            entityManager.persistAndFlush(channel2);

            entityManager.clear();

            List<Channel> activeChannels = repository.findByStatusOrderBySortOrder(ChannelStatus.ACTIVE);
            assertTrue(activeChannels.size() >= 3);

            // 验证排序是否正确（按sortOrder升序）
            for (int i = 1; i < activeChannels.size(); i++) {
                assertTrue(activeChannels.get(i-1).getSortOrder() <= activeChannels.get(i).getSortOrder());
            }
        }

        @Test
        @DisplayName("查找所有渠道并按排序顺序排序")
        void testFindAllByOrderBySortOrder() {
            List<Channel> allChannels = repository.findAllByOrderBySortOrder();
            assertEquals(4, allChannels.size());

            // 验证排序是否正确
            for (int i = 1; i < allChannels.size(); i++) {
                assertTrue(allChannels.get(i-1).getSortOrder() <= allChannels.get(i).getSortOrder());
            }
        }

        @Test
        @DisplayName("按排序顺序和创建时间排序查找渠道（分页）")
        void testFindAllByOrderBySortOrderAscCreatedAtDesc() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Channel> page = repository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable);

            assertEquals(4, page.getTotalElements());
            assertEquals(4, page.getContent().size());
        }
    }

    @Nested
    @DisplayName("销售相关查询测试")
    class SalesQueryTests {

        @Test
        @DisplayName("查找总销售额大于指定金额的渠道")
        void testFindByTotalSalesGreaterThan() {
            List<Channel> channels = repository.findByTotalSalesGreaterThan(new BigDecimal("100000.00"));
            assertEquals(2, channels.size());
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("ACTIVE_ONLINE")));
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("SUSPENDED_HYBRID")));
        }

        @Test
        @DisplayName("查找当月销售额大于等于指定金额的渠道")
        void testFindByCurrentMonthSalesGreaterThanEqual() {
            List<Channel> channels = repository.findByCurrentMonthSalesGreaterThanEqual(new BigDecimal("50000.00"));
            assertEquals(1, channels.size());
            assertEquals("ACTIVE_ONLINE", channels.get(0).getCode());
        }

        @Test
        @DisplayName("查找已达到月度目标的渠道")
        void testFindChannelsWithTargetAchieved() {
            // 修改activeOnlineChannel的当月销售额使其达到目标
            activeOnlineChannel.setCurrentMonthSales(new BigDecimal("120000.00"));
            entityManager.merge(activeOnlineChannel);
            entityManager.flush();
            entityManager.clear();

            List<Channel> channels = repository.findChannelsWithTargetAchieved();
            assertEquals(1, channels.size());
            assertEquals("ACTIVE_ONLINE", channels.get(0).getCode());
        }

        @Test
        @DisplayName("查找未达到月度目标的活跃渠道")
        void testFindActiveChannelsWithTargetNotAchieved() {
            List<Channel> channels = repository.findActiveChannelsWithTargetNotAchieved();
            assertEquals(1, channels.size());
            assertEquals("ACTIVE_ONLINE", channels.get(0).getCode());
        }
    }

    @Nested
    @DisplayName("时间范围查询测试")
    class TimeRangeQueryTests {

        @Test
        @DisplayName("查找最后活动时间在指定时间之后的渠道")
        void testFindByLastActivityAtAfter() {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            List<Channel> channels = repository.findByLastActivityAtAfter(threeDaysAgo);

            assertEquals(1, channels.size());
            assertEquals("ACTIVE_ONLINE", channels.get(0).getCode());
        }

        @Test
        @DisplayName("查找在指定时间范围内创建的渠道")
        void testFindByCreatedAtBetween() {
            LocalDateTime start = LocalDateTime.now().minusDays(8);
            LocalDateTime end = LocalDateTime.now();

            List<Channel> channels = repository.findByCreatedAtBetween(start, end);
            assertEquals(4, channels.size()); // 所有渠道都在这个时间范围内
        }

        @Test
        @DisplayName("查找指定天数内无活动的渠道")
        void testFindInactiveChannelsBeforeDate() {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            List<Channel> channels = repository.findInactiveChannelsBeforeDate(threeDaysAgo);

            // 应该包含suspendedHybridChannel和没有lastActivityAt的渠道
            assertTrue(channels.size() >= 1);
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("SUSPENDED_HYBRID")));
        }
    }

    @Nested
    @DisplayName("关键字搜索查询测试")
    class KeywordSearchTests {

        @Test
        @DisplayName("根据关键字搜索渠道")
        void testFindByKeyword() {
            Pageable pageable = PageRequest.of(0, 10);

            // 搜索渠道名称
            Page<Channel> page1 = repository.findByKeyword("线上", pageable);
            assertEquals(1, page1.getTotalElements());
            assertEquals("ACTIVE_ONLINE", page1.getContent().get(0).getCode());

            // 搜索联系人姓名
            Page<Channel> page2 = repository.findByKeyword("张三", pageable);
            assertEquals(1, page2.getTotalElements());
            assertEquals("ACTIVE_ONLINE", page2.getContent().get(0).getCode());

            // 搜索不存在的关键字
            Page<Channel> page3 = repository.findByKeyword("不存在", pageable);
            assertEquals(0, page3.getTotalElements());
        }

        @Test
        @DisplayName("复合条件查询渠道")
        void testFindByStatusAndTypeAndKeyword() {
            Pageable pageable = PageRequest.of(0, 10);

            // 测试状态+类型+关键字组合查询
            Page<Channel> page1 = repository.findByStatusAndTypeAndKeyword(
                ChannelStatus.ACTIVE, ChannelType.ONLINE, "线上", pageable);
            assertEquals(1, page1.getTotalElements());
            assertEquals("ACTIVE_ONLINE", page1.getContent().get(0).getCode());

            // 测试只有状态的查询
            Page<Channel> page2 = repository.findByStatusAndTypeAndKeyword(
                ChannelStatus.INACTIVE, null, null, pageable);
            assertEquals(1, page2.getTotalElements());
            assertEquals("INACTIVE_OFFLINE", page2.getContent().get(0).getCode());

            // 测试只有关键字的查询
            Page<Channel> page3 = repository.findByStatusAndTypeAndKeyword(
                null, null, "混合", pageable);
            assertEquals(1, page3.getTotalElements());
            assertEquals("SUSPENDED_HYBRID", page3.getContent().get(0).getCode());
        }
    }

    @Nested
    @DisplayName("统计查询方法测试")
    class StatisticsQueryTests {

        @Test
        @DisplayName("统计指定状态的渠道数量")
        void testCountByStatus() {
            assertEquals(1, repository.countByStatus(ChannelStatus.ACTIVE));
            assertEquals(1, repository.countByStatus(ChannelStatus.INACTIVE));
            assertEquals(1, repository.countByStatus(ChannelStatus.SUSPENDED));
            assertEquals(1, repository.countByStatus(ChannelStatus.DELETED));
        }

        @Test
        @DisplayName("统计指定类型的渠道数量")
        void testCountByType() {
            assertEquals(2, repository.countByType(ChannelType.ONLINE));
            assertEquals(1, repository.countByType(ChannelType.OFFLINE));
            assertEquals(1, repository.countByType(ChannelType.HYBRID));
        }

        @Test
        @DisplayName("统计指定状态和类型的渠道数量")
        void testCountByStatusAndType() {
            assertEquals(1, repository.countByStatusAndType(ChannelStatus.ACTIVE, ChannelType.ONLINE));
            assertEquals(1, repository.countByStatusAndType(ChannelStatus.INACTIVE, ChannelType.OFFLINE));
            assertEquals(0, repository.countByStatusAndType(ChannelStatus.ACTIVE, ChannelType.OFFLINE));
        }

        @Test
        @DisplayName("获取所有活跃渠道的总销售额")
        void testGetTotalActiveSales() {
            BigDecimal total = repository.getTotalActiveSales();
            assertEquals(0, new BigDecimal("250000.00").compareTo(total));
        }

        @Test
        @DisplayName("获取所有活跃渠道的当月销售额")
        void testGetTotalActiveCurrentMonthSales() {
            BigDecimal total = repository.getTotalActiveCurrentMonthSales();
            assertEquals(0, new BigDecimal("80000.00").compareTo(total));
        }

        @Test
        @DisplayName("获取指定类型渠道的总销售额")
        void testGetTotalSalesByType() {
            BigDecimal onlineTotal = repository.getTotalSalesByType(ChannelType.ONLINE);
            assertEquals(0, new BigDecimal("250000.00").compareTo(onlineTotal));

            BigDecimal offlineTotal = repository.getTotalSalesByType(ChannelType.OFFLINE);
            assertEquals(0, BigDecimal.ZERO.compareTo(offlineTotal));
        }
    }

    @Nested
    @DisplayName("自定义复杂查询测试")
    class CustomQueryTests {

        @Test
        @DisplayName("查找指定日期后有活动的活跃渠道")
        void testFindActiveChannelsAfterDate() {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            List<Channel> channels = repository.findActiveChannelsAfterDate(threeDaysAgo);

            assertEquals(1, channels.size());
            assertEquals("ACTIVE_ONLINE", channels.get(0).getCode());
        }

        @Test
        @DisplayName("查找佣金率在指定范围内的渠道")
        void testFindByCommissionRateBetween() {
            List<Channel> channels = repository.findByCommissionRateBetween(
                new BigDecimal("0.0300"), new BigDecimal("0.0500"));

            assertEquals(3, channels.size());
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("ACTIVE_ONLINE")));
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("INACTIVE_OFFLINE")));
            assertTrue(channels.stream().anyMatch(c -> c.getCode().equals("SUSPENDED_HYBRID")));
        }

        @Test
        @DisplayName("查找销售业绩排名前N的渠道")
        void testFindTopPerformingChannels() {
            Pageable pageable = PageRequest.of(0, 2);
            List<Channel> topChannels = repository.findTopPerformingChannels(pageable);

            assertEquals(1, topChannels.size()); // 只有一个活跃渠道
            assertEquals("ACTIVE_ONLINE", topChannels.get(0).getCode());
        }
    }

    @Nested
    @DisplayName("存在性检查测试")
    class ExistenceCheckTests {

        @Test
        @DisplayName("检查渠道代码是否已存在")
        void testExistsByCode() {
            assertTrue(repository.existsByCode("ACTIVE_ONLINE"));
            assertFalse(repository.existsByCode("NON_EXISTENT"));
        }

        @Test
        @DisplayName("检查渠道名称是否已存在")
        void testExistsByName() {
            assertTrue(repository.existsByName("活跃线上渠道"));
            assertFalse(repository.existsByName("不存在的渠道"));
        }

        @Test
        @DisplayName("检查渠道代码是否已被其他渠道使用")
        void testExistsByCodeAndIdNot() {
            assertTrue(repository.existsByCodeAndIdNot("ACTIVE_ONLINE", 999L));
            assertFalse(repository.existsByCodeAndIdNot("ACTIVE_ONLINE", activeOnlineChannel.getId()));
            assertFalse(repository.existsByCodeAndIdNot("NON_EXISTENT", 999L));
        }

        @Test
        @DisplayName("检查渠道名称是否已被其他渠道使用")
        void testExistsByNameAndIdNot() {
            assertTrue(repository.existsByNameAndIdNot("活跃线上渠道", 999L));
            assertFalse(repository.existsByNameAndIdNot("活跃线上渠道", activeOnlineChannel.getId()));
            assertFalse(repository.existsByNameAndIdNot("不存在的渠道", 999L));
        }
    }
}