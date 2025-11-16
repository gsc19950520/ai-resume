package com.aicv.airesume.model.vo;

import java.util.List;

/**
 * 面试历史列表VO
 */
public class InterviewHistoryListVO {
    
    private List<InterviewHistoryVO> histories;
    
    public InterviewHistoryListVO() {}
    
    public InterviewHistoryListVO(List<InterviewHistoryVO> histories) {
        this.histories = histories;
    }
    
    public List<InterviewHistoryVO> getHistories() {
        return histories;
    }
    
    public void setHistories(List<InterviewHistoryVO> histories) {
        this.histories = histories;
    }
}