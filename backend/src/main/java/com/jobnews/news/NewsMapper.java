package com.jobnews.news;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsMapper {

    boolean existsByUrl(String url);

    int insert(News news);
}
