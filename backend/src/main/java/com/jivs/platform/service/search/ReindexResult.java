package com.jivs.platform.service.search;

import java.util.Date;

/**
 * Reindex result
 */
public class ReindexResult {
    private int totalDocuments;
    private int indexedDocuments;
    private int failedDocuments;
    private boolean success;
    private String errorMessage;
    private Date startTime;
    private Date endTime;

    // Getters and setters
    public int getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
    public int getIndexedDocuments() { return indexedDocuments; }
    public void setIndexedDocuments(int indexedDocuments) { this.indexedDocuments = indexedDocuments; }
    public int getFailedDocuments() { return failedDocuments; }
    public void setFailedDocuments(int failedDocuments) { this.failedDocuments = failedDocuments; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
