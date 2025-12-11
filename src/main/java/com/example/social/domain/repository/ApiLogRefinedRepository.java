package com.example.social.domain.repository;

import com.example.social.domain.entity.ApiLogRefined;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ApiLogRefinedRepository extends ElasticsearchRepository<ApiLogRefined, String> {
}