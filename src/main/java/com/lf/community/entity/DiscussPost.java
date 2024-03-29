package com.lf.community.entity;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/4 0004 21:50
 * @Version 1.0
 */
@Data
@ToString
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
public class DiscussPost {
    @Id
    private int id;
    @Field(type = FieldType.Integer)
    private int userId;
    @Field(type = FieldType.Text,analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text,analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;
    @Field(type = FieldType.Integer)
    private int type;
    @Field(type = FieldType.Integer)
    private int status;
    @Field(type = FieldType.Date)
    private Date createTime;
    @Field(type = FieldType.Integer)
    private int commentCount;
    @Field(type = FieldType.Double)
    private double score;
}
