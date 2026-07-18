package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController("userUserController")
@RequestMapping("/user/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户微信登录：{}", userLoginDTO.getCode());

        String openid = "mock_openid_" + userLoginDTO.getCode();

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
        );
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("openid", user.getOpenid());
        result.put("token", token);
        result.put("deliveryFee", 4.0);
        result.put("shopName", "苍穹外卖");
        result.put("shopAddress", "北京市朝阳区");
        result.put("shopId", "f3deb");

        return Result.success(result);
    }
}
