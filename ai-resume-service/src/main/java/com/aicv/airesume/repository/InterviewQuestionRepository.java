package com.aicv.airesume.repository;

import com.aicv.airesume.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 面试问题数据访问接口
 * 提供对interview_question表的操作，支持AI职业问题库功能
 */
@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    /**
     * 根据jobType和skillTag查询问题列表，按使用次数降序
     * @param jobTypeId 职位类型ID
     * @param skillTag 技能标签
     * @return 问题列表
     */
    List<InterviewQuestion> findByJobTypeIdAndSkillTagOrderByUsageCountDesc(Long jobTypeId, String skillTag);

    /**
     * 根据jobType、skillTag和depthLevel查询问题列表，按使用次数降序
     * @param jobTypeId 职位类型ID
     * @param skillTag 技能标签
     * @param depthLevel 问题深度
     * @return 问题列表
     */
    List<InterviewQuestion> findByJobTypeIdAndSkillTagAndDepthLevelOrderByUsageCountDesc(
            Long jobTypeId, String skillTag, String depthLevel);

    /**
     * 根据相似性哈希查找问题
     * @param similarityHash 相似性哈希值
     * @return 问题对象
     */
    Optional<InterviewQuestion> findBySimilarityHash(String similarityHash);
    
    /**
     * 根据相似性哈希查找问题列表
     * @param similarityHash 相似性哈希值
     * @return 问题列表
     */
    List<InterviewQuestion> findAllBySimilarityHash(String similarityHash);
    
    /**
     * 根据技能标签和深度级别查询问题列表
     * @param skillTag 技能标签
     * @param depthLevel 问题深度
     * @return 问题列表
     */
    List<InterviewQuestion> findBySkillTagAndDepthLevel(String skillTag, String depthLevel);
    
    /**
     * 增加问题的使用次数
     * @param id 问题ID
     */
    @Query("UPDATE InterviewQuestion q SET q.usageCount = q.usageCount + 1 WHERE q.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    /**
     * 查找相似问题的自定义查询方法
     * 这里使用参数方式定义，实际实现可能需要通过AI服务计算相似度
     * @param jobTypeId 职位类型ID
     * @param skillTag 技能标签
     * @param depthLevel 问题深度
     * @param similarityThreshold 相似度阈值
     * @return 相似问题列表
     */
    @Query("SELECT q FROM InterviewQuestion q WHERE q.jobTypeId = :jobTypeId AND q.skillTag = :skillTag AND q.depthLevel = :depthLevel")
    List<InterviewQuestion> findSimilarQuestion(
            @Param("jobTypeId") Long jobTypeId,
            @Param("skillTag") String skillTag,
            @Param("depthLevel") String depthLevel);

    /**
     * 更新问题的使用次数和平均得分
     * @param id 问题ID
     * @param newUsageCount 新的使用次数
     * @param newAvgScore 新的平均得分
     */
    @Query("UPDATE InterviewQuestion q SET q.usageCount = :newUsageCount, q.avgScore = :newAvgScore WHERE q.id = :id")
    void updateUsageStats(@Param("id") Long id, @Param("newUsageCount") Integer newUsageCount, @Param("newAvgScore") Float newAvgScore);

    List<InterviewQuestion> findAllByDepthLevel(String depthLevel);

}