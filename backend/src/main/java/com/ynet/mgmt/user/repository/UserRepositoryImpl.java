package com.ynet.mgmt.user.repository;

import com.ynet.mgmt.user.dto.UserSearchCriteria;
import com.ynet.mgmt.user.dto.UserStatistics;
import com.ynet.mgmt.user.entity.User;
import com.ynet.mgmt.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户Repository自定义实现
 * 使用JPA Criteria API实现复杂查询
 */
@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> findByComplexCriteria(UserSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        List<Predicate> predicates = buildPredicates(cb, user, criteria);
        query.where(predicates.toArray(new Predicate[0]));

        // 应用排序
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(user.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(user.get(sortOrder.getProperty())));
                }
            }
            query.orderBy(orders);
        }

        TypedQuery<User> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<User> users = typedQuery.getResultList();

        // 计算总数
        Long total = countByComplexCriteria(criteria);

        return new PageImpl<>(users, pageable, total);
    }

    @Override
    public List<UserStatistics> getUserStatisticsByRole() {
        String jpql = "SELECT new com.ynet.mgmt.user.dto.UserStatistics(u.role, u.status, COUNT(u)) " +
                     "FROM User u GROUP BY u.role, u.status ORDER BY u.role, u.status";
        return entityManager.createQuery(jpql, UserStatistics.class).getResultList();
    }

    @Override
    public List<User> findActiveUsersWithRecentLogin(Duration duration) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(duration);
        return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.status = :status AND u.lastLoginAt >= :cutoffTime ORDER BY u.lastLoginAt DESC",
            User.class)
            .setParameter("status", UserStatus.ACTIVE)
            .setParameter("cutoffTime", cutoffTime)
            .getResultList();
    }

    @Override
    public List<User> findRecentlyRegisteredUsers(int limit) {
        return entityManager.createQuery(
            "SELECT u FROM User u ORDER BY u.createdAt DESC",
            User.class)
            .setMaxResults(limit)
            .getResultList();
    }

    @Override
    public List<User> findUsersRequiringPasswordReset(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.passwordChangedAt < :cutoffDate OR u.passwordChangedAt IS NULL ORDER BY u.passwordChangedAt ASC",
            User.class)
            .setParameter("cutoffDate", cutoffDate)
            .getResultList();
    }

    /**
     * 构建查询条件谓词
     * @param cb CriteriaBuilder
     * @param user Root实体
     * @param criteria 查询条件
     * @return 谓词列表
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<User> user, UserSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        // 关键字搜索（用户名、邮箱、姓名）
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
            Predicate usernamePredicate = cb.like(cb.lower(user.get("username")), keyword);
            Predicate emailPredicate = cb.like(cb.lower(user.get("email")), keyword);
            Predicate fullNamePredicate = cb.like(cb.lower(user.get("fullName")), keyword);
            predicates.add(cb.or(usernamePredicate, emailPredicate, fullNamePredicate));
        }

        // 状态筛选
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(user.get("status"), criteria.getStatus()));
        }

        // 角色筛选
        if (criteria.getRole() != null) {
            predicates.add(cb.equal(user.get("role"), criteria.getRole()));
        }

        // 创建时间范围
        if (criteria.getCreatedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(user.get("createdAt"), criteria.getCreatedAfter()));
        }

        if (criteria.getCreatedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(user.get("createdAt"), criteria.getCreatedBefore()));
        }

        // 邮箱验证状态
        if (criteria.getEmailVerified() != null) {
            predicates.add(cb.equal(user.get("emailVerified"), criteria.getEmailVerified()));
        }

        return predicates;
    }

    /**
     * 统计查询条件匹配的用户总数
     * @param criteria 查询条件
     * @return 用户总数
     */
    private Long countByComplexCriteria(UserSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);

        List<Predicate> predicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(countQuery).getSingleResult();
    }
}