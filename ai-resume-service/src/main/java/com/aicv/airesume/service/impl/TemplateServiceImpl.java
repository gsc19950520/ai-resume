package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Template;
import com.aicv.airesume.repository.TemplateRepository;
import com.aicv.airesume.service.TemplateService;
import com.aicv.airesume.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.aicv.airesume.utils.RetryUtils;

/**
 * 模板服务实现类
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private RetryUtils retryUtils;

    @Override
    public List<Template> getAllTemplates() {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 使用修改后的方法，按使用次数降序排序
            return templateRepository.findAllByOrderByUseCountDesc();
        });
    }

    @Override
    public List<Template> getTemplatesByJobType(String jobType) {
        // 使用修改后的方法，根据岗位类型查询并按使用次数排序
        return templateRepository.findByJobTypeOrderByUseCountDesc(jobType);
    }

    @Override
    public List<Template> getFreeTemplates() {
        // 获取所有免费模板
        return templateRepository.findAll()
                .stream()
                .filter(Template::getFree)
                .collect(Collectors.toList());
    }

    @Override
    public List<Template> getVipTemplates() {
        // 使用修改后的方法，获取VIP专属模板
        return templateRepository.findByVipOnlyOrderByUseCountDesc(true);
    }

    @Override
    public Template getTemplateById(Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    @Override
    public void addTemplateUseCount(Long templateId) {
    }

    @Override
    public boolean checkTemplatePermission(Long userId, Long templateId) {
        return true;
    }
}