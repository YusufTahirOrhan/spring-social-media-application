package com.example.social.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "api-logs-refined")
public class ApiLogRefined {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String originalLogId;

    @Field(type = FieldType.Keyword)
    private String userType;

    @Field(type = FieldType.Keyword)
    private String durationColor;
}