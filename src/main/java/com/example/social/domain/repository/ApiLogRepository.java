package com.example.social.domain.repository;

import com.example.social.domain.entity.ApiLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ApiLogRepository extends ElasticsearchRepository<ApiLog, String> {
}