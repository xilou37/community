package com.lf.community.event;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.lf.community.entity.DiscussPost;
import com.lf.community.entity.Event;
import com.lf.community.entity.Message;
import com.lf.community.service.DiscussPostService;
import com.lf.community.service.ElasticSearchService;
import com.lf.community.service.MessageService;
import com.lf.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/10 0010 14:37
 * @Version 1.0
 */
@Component
public class EventConsumer implements CommunityConstant {
    
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;
    
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    
    
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return;
        }
        //发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());
        
        if (!event.getData().isEmpty()){
            for (Map.Entry<String,Object> entry : event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        
        messageService.addMessage(message);
    }
    
    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.saveDiscussPost(post);
        
    }    
    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return;
        }
        elasticSearchService.deleteDiscussPost(event.getEntityId());
        
    }    
    
    //消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息内容为空！");
            return;
        }
        Gson gson = new Gson();
        Event event = gson.fromJson((String) record.value(), Event.class);
//        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return;
        }
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功：" +cmd);
        } catch (IOException e) {

            logger.error("生成长图失败：" +e.getMessage());
        }


    }
    
}
