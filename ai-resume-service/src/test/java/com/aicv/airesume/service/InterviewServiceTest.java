package com.aicv.airesume.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 测试InterviewService Bean是否能正确注入
 */
@SpringBootTest
@SpringJUnitConfig
public class InterviewServiceTest {

    @Autowired
    private InterviewService interviewService;

    @Test
    public void testInterviewServiceInjection() {
        // 验证InterviewService Bean是否成功注入
        assertNotNull(interviewService, "InterviewService Bean未成功注入");
        System.out.println("InterviewService Bean成功注入！");
    }
}