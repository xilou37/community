package com.lf.community.controller;

import com.google.code.kaptcha.Producer;
import com.lf.community.dao.LoginTicketMapper;
import com.lf.community.entity.User;
import com.lf.community.service.UserService;
import com.lf.community.util.CommunityConstant;
import com.lf.community.util.CommunityUtil;
import com.lf.community.util.RedisKeyUtil;
import com.sun.deploy.net.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/5 0005 16:35
 * @Version 1.0
 */
@Controller
public class LoginController implements CommunityConstant {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }
    
    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()){
            model.addAttribute("msg","?????????????????????????????????????????????????????????????????????????????????");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "site/register";
        }
        
    }
    //httP://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code){
        int result = userService.activation(userId, code);
        if (result == ACTIVITION_SUCCESS){
            model.addAttribute("msg","??????????????????????????????????????????????????????!");
            model.addAttribute("target","/login");
        }else if (result == ACTIVITION_REPEAT){
            model.addAttribute("msg","?????????????????????????????????????????????");
            model.addAttribute("target","/index");
        }else {
            model.addAttribute("msg","?????????????????????????????????????????????");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }
    
    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/){
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //??????????????????session
//        session.setAttribute("kaptcha",text);
        
        //??????????????????
        String kaptcha = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptcha);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);//????????????
        response.addCookie(cookie);
        //??????????????????redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptcha);
        redisTemplate.opsForValue().set(redisKey,text, 60, TimeUnit.SECONDS);

        //???????????????????????????
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            logger.error("????????????????????????"+ e.getMessage());
        }
    }
    
    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username, String password, String code,
                        boolean rememberme, Model model/*,HttpSession session*/,HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner ){
        //???????????????
//        String kaptcha = (String)session.getAttribute("kaptcha");
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
             kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","??????????????????");
            return "/site/login";
        }
        //?????????????????????
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }

    }
    
    @RequestMapping(path = "/logout",method =RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }
    
}
