package com.example.taskai.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("member_profile")
public class MemberProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String resumeText;
    private String experienceSummary;
    private Integer currentWorkload;
    private Integer completedTaskCount;
    private Integer overdueTaskCount;
    private BigDecimal taskCompletionRate;
    private BigDecimal overdueRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getExperienceSummary() { return experienceSummary; }
    public void setExperienceSummary(String experienceSummary) { this.experienceSummary = experienceSummary; }
    public Integer getCurrentWorkload() { return currentWorkload; }
    public void setCurrentWorkload(Integer currentWorkload) { this.currentWorkload = currentWorkload; }
    public Integer getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(Integer completedTaskCount) { this.completedTaskCount = completedTaskCount; }
    public Integer getOverdueTaskCount() { return overdueTaskCount; }
    public void setOverdueTaskCount(Integer overdueTaskCount) { this.overdueTaskCount = overdueTaskCount; }
    public BigDecimal getTaskCompletionRate() { return taskCompletionRate; }
    public void setTaskCompletionRate(BigDecimal taskCompletionRate) { this.taskCompletionRate = taskCompletionRate; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
