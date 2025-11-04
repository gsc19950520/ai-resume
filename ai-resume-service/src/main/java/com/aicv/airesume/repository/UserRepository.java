package com.aicv.airesume.repository;

import com.aicv.airesume.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据openId查询用户
     */
    Optional<User> findByOpenId(String openId);

    /**
     * 判断用户是否存在
     */
    boolean existsByOpenId(String openId);
}
