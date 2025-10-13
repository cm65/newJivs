package com.jivs.platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Data Transfer Object for Document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String filename;
    private String title;
    private String description;
    private String fileType;
    private Long size;
    private List<String> tags;
    private String status;
    private boolean archived;
    private Date createdDate;
    private Date modifiedDate;
    private String checksum;
    private String author;
    private String subject;
    private String keywords;
    private Integer pageCount;
    private Integer wordCount;
    private String language;
    private String storageTier;
    private boolean encrypted;
    private boolean compressed;
    private Double compressionRatio;

    // For search results
    private double score;
    private List<String> highlight;
}