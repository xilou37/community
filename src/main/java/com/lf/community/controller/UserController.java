package com.lf.community.controller;

import com.lf.community.annotation.LoginRequired;
import com.lf.community.entity.User;
import com.lf.community.service.FollowService;
import com.lf.community.service.LikeService;
import com.lf.community.service.UserService;
import com.lf.community.util.CommunityConstant;
import com.lf.community.util.CommunityUtil;
import com.lf.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Multipart;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/4 0004 19:10
 * @Version 1.0
 */
@Controller
@RequestMapping("user")
public class UserController implements CommunityConstant {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Value("${community.path.upload}")
    private String uploadPath;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private FollowService followService;
    
    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if (headerImage == null){
            model.addAttribute("error","???????????????????????????");
            return "/site/setting";
        }
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","????????????????????????");
            return "/site/setting";
        }
        //????????????????????????
        filename = CommunityUtil.generateUUID() + suffix;
        //???????????????????????????
        File dest = new File(uploadPath + filename);
        try {
            //????????????
            headerImage.transferTo(dest);
        } catch (IOException e) {
            
            logger.error("?????????????????????"+ e.getMessage());
            throw new RuntimeException("?????????????????????????????????????????????"+ e);
        }
        //???????????????????????????????????????web?????????
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" +filename;
        userService.updateHeader(user.getId(),headerUrl);
        
        return "redirect:/index";
    }
    
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //????????????????????????
        filename = uploadPath + filename;
        //???????????????
        String suffix = filename.substring(filename.lastIndexOf("."));
        //????????????
        response.setContentType("/image/"+suffix);
        try (
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(filename);
            ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("?????????????????????"+e.getMessage());
        }
    }
    
    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword,Model model){
        Map<String, Object> map = userService.updatePassword(oldPassword, newPassword, confirmPassword);
        if (map == null || map.isEmpty()){
            return "redirect:/logout";
        }else {
            model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
            model.addAttribute("confirmPasswordMsg",map.get("confirmPasswordMsg"));
            return "/site/setting";
        }
    }
    
    //????????????
    @RequestMapping(path = "/profile/{userId}" ,method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.selectById(userId);
        if (user == null){
            throw new RuntimeException("?????????????????????");
        }
        //??????
        model.addAttribute("user",user);
        //????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //????????????
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);
        //???????????????????????????
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        
        return "/site/profile";


    }
    
    
}
