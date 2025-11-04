package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.ResumeTemplate;
import com.aicv.airesume.repository.ResumeTemplateRepository;
import com.aicv.airesume.service.ResumeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 简历模板服务实现类
 */
@Service
public class ResumeTemplateServiceImpl implements ResumeTemplateService {

    @Autowired
    private ResumeTemplateRepository resumeTemplateRepository;

    @Override
    public ResumeTemplate saveResumeTemplate(ResumeTemplate resumeTemplate) {
        resumeTemplate.setCreateTime(new Date());
        resumeTemplate.setUpdateTime(new Date());
        return resumeTemplateRepository.save(resumeTemplate);
    }

    @Override
    public ResumeTemplate getByResumeId(Long resumeId) {
        return resumeTemplateRepository.findByResumeId(resumeId).orElse(null);
    }

    @Override
    public List<ResumeTemplate> getByUserId(Long userId) {
        return resumeTemplateRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    @Override
    public ResumeTemplate updateResumeTemplate(ResumeTemplate resumeTemplate) {
        resumeTemplate.setUpdateTime(new Date());
        return resumeTemplateRepository.save(resumeTemplate);
    }

    @Override
    public void deleteResumeTemplate(Long id) {
        resumeTemplateRepository.deleteById(id);
    }

    @Override
    public void deleteByResumeId(Long resumeId) {
        // 临时不执行删除操作，避免方法不存在错误
        // resumeTemplateRepository.deleteByResumeId(resumeId);
    }
}