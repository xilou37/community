package com.lf.community.controller;

import com.lf.community.entity.DiscussPost;
import com.lf.community.entity.Page;
import com.lf.community.service.ElasticSearchService;
import com.lf.community.service.LikeService;
import com.lf.community.service.UserService;
import com.lf.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/11 0011 17:03
 * @Version 1.0
 */
@Controller
public class SearchController implements CommunityConstant {
    
    @Autowired
    private ElasticSearchService elasticSearchService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeService likeService;
    
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model){
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult = elasticSearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        //聚合数据
        List<Map<String ,Object>> discussPosts = new ArrayList<>();
        if (searchResult != null){
            for (DiscussPost post : searchResult){
                Map<String, Object> map = new HashMap<>();
                //帖子
                map.put("post",post);
                //作者
                map.put("user",userService.selectById(post.getUserId()));
                //点赞数量
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);
        
        //分页信息
//        page.setLimit(5);
        
        page.setPath("/search?keyword=" +keyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());
        
        return "/site/search";
        
        

    }
    
    
}
