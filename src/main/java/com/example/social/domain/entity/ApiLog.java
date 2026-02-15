package com.example.social.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "api-logs")
public class ApiLog {
    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String httpMethod;

    @Field(type = FieldType.Keyword)
    private String path;

    @Field(type = FieldType.Integer)
    private int statusCode;

    @Field(type = FieldType.Long)
    private long duration;

    @Field(type = FieldType.Text)
    private String requestHeader;

    @Field(type = FieldType.Text)
    private String requestBody;

    @Field(type = FieldType.Text)
    private String responseHeader;

    @Field(type = FieldType.Text)
    private String responseBody;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text)
    private String clientIp;

    @Field(type = FieldType.Text)
    private String userAgent;
}


/*GET api-logs/_search
{
    "_source": ["id", "timestamp", "httpMethod", "path", "statusCode", "duration", "requestBody", "responseBody", "userId", "clientIp", "userAgent"]
}*/