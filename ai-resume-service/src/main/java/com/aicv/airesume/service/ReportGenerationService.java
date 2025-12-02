package com.aicv.airesume.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 报告生成服务，用于管理异步报告生成状态和内容块
 */
@Service
public class ReportGenerationService {

    /**
     * 报告状态枚举
     */
    public enum ReportStatus {
        GENERATING,  // 生成中
        COMPLETED,   // 已完成
        FAILED       // 生成失败
    }

    /**
     * 报告内容块
     */
    public static class ReportChunk {
        private final int index;
        private final String content;

        public ReportChunk(int index, String content) {
            this.index = index;
            this.content = content;
        }

        public int getIndex() {
            return index;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * 报告生成记录
     */
    public static class ReportGenerationRecord {
        private final String reportId;
        private ReportStatus status;
        private final List<ReportChunk> chunks;
        private final long createdAt;
        private long lastAccessedAt;
        private String errorMessage;

        public ReportGenerationRecord(String reportId) {
            this.reportId = reportId;
            this.status = ReportStatus.GENERATING;
            this.chunks = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = System.currentTimeMillis();
        }

        public String getReportId() {
            return reportId;
        }

        public ReportStatus getStatus() {
            return status;
        }

        public void setStatus(ReportStatus status) {
            this.status = status;
        }

        public List<ReportChunk> getChunks() {
            return chunks;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastAccessedAt() {
            return lastAccessedAt;
        }

        public void setLastAccessedAt(long lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        /**
         * 添加报告内容块
         */
        public synchronized void addChunk(String content) {
            int index = chunks.size();
            chunks.add(new ReportChunk(index, content));
        }
    }

    // 线程安全的报告存储Map
    private final Map<String, ReportGenerationRecord> reportStore = new ConcurrentHashMap<>();
    
    // 定时清理过期报告的线程池
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 报告过期时间（毫秒）- 1小时
    private static final long REPORT_EXPIRY_TIME = 60 * 60 * 1000;
    
    // 清理间隔（毫秒）- 10分钟
    private static final long CLEANUP_INTERVAL = 10 * 60 * 1000;

    public ReportGenerationService() {
        // 启动定时清理任务
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredReports, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 创建新的报告生成记录
     */
    public ReportGenerationRecord createReportRecord(String reportId) {
        ReportGenerationRecord record = new ReportGenerationRecord(reportId);
        reportStore.put(reportId, record);
        return record;
    }

    /**
     * 获取报告生成记录
     */
    public ReportGenerationRecord getReportRecord(String reportId) {
        ReportGenerationRecord record = reportStore.get(reportId);
        if (record != null) {
            // 更新最后访问时间
            record.setLastAccessedAt(System.currentTimeMillis());
        }
        return record;
    }

    /**
     * 获取报告块，从指定索引的下一个位置开始
     */
    public List<ReportChunk> getReportChunks(String reportId, int lastIndex) {
        ReportGenerationRecord record = getReportRecord(reportId);
        if (record == null) {
            return null;
        }

        synchronized (record) {
            List<ReportChunk> chunks = record.getChunks();
            // 计算下一个要获取的起始索引，确保只返回新增的报告块
            int startIndex = lastIndex + 1;
            if (startIndex >= chunks.size()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(chunks.subList(startIndex, chunks.size()));
        }
    }

    /**
     * 清理过期报告
     */
    private void cleanupExpiredReports() {
        long currentTime = System.currentTimeMillis();
        reportStore.entrySet().removeIf(entry -> {
            ReportGenerationRecord record = entry.getValue();
            long timeSinceLastAccess = currentTime - record.getLastAccessedAt();
            return timeSinceLastAccess > REPORT_EXPIRY_TIME;
        });
    }

    /**
     * 关闭清理线程池
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
