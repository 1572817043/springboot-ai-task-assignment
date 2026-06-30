package com.example.taskai.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ai_recommendation_candidate")
public class AiRecommendationCandidate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long candidateUserId;
    private Integer rankNo;
    private BigDecimal totalScore;
    private BigDecimal skillScore;
    private BigDecimal historyScore;
    private BigDecimal workloadScore;
    private BigDecimal completionScore;
    private BigDecimal deadlineRiskScore;
    private String reason;
    private Integer accepted;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getCandidateUserId() { return candidateUserId; }
    public void setCandidateUserId(Long candidateUserId) { this.candidateUserId = candidateUserId; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public BigDecimal getSkillScore() { return skillScore; }
    public void setSkillScore(BigDecimal skillScore) { this.skillScore = skillScore; }
    public BigDecimal getHistoryScore() { return historyScore; }
    public void setHistoryScore(BigDecimal historyScore) { this.historyScore = historyScore; }
    public BigDecimal getWorkloadScore() { return workloadScore; }
    public void setWorkloadScore(BigDecimal workloadScore) { this.workloadScore = workloadScore; }
    public BigDecimal getCompletionScore() { return completionScore; }
    public void setCompletionScore(BigDecimal completionScore) { this.completionScore = completionScore; }
    public BigDecimal getDeadlineRiskScore() { return deadlineRiskScore; }
    public void setDeadlineRiskScore(BigDecimal deadlineRiskScore) { this.deadlineRiskScore = deadlineRiskScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getAccepted() { return accepted; }
    public void setAccepted(Integer accepted) { this.accepted = accepted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
