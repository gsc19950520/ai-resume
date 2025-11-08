package com.aicv.airesume.common.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应封装类
 * 统一分页查询的返回格式
 *
 * @param <T> 分页数据项类型
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Data
@NoArgsConstructor
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 分页数据列表
     */
    private List<T> list;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 构造器
     *
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param total 总记录数
     * @param list 数据列表
     */
    public PageResponse(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        // 计算总页数
        this.pages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        // 计算是否有下一页
        this.hasNext = pageNum < this.pages;
        // 计算是否有上一页
        this.hasPrevious = pageNum > 1;
    }

    /**
     * 创建空的分页响应
     *
     * @param <T> 分页数据项类型
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResponse<T>(pageNum, pageSize, 0L, null);
    }

    /**
     * 计算偏移量
     *
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return 偏移量
     */
    public static Integer calculateOffset(Integer pageNum, Integer pageSize) {
        return Math.max(0, (pageNum - 1) * pageSize);
    }
}