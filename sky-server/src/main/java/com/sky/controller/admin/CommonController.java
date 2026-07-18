package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.MinIOUtil;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private MinIOUtil minIOUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 文件上传接口（用于菜品、套餐等图片上传）
     * @param file 上传的文件
     * @return 文件访问 URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        log.info("文件上传，文件名：{}", file.getOriginalFilename());

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

            // 上传到 MinIO
            String url = minIOUtil.upload(file.getInputStream(), objectName);

            log.info("文件上传成功，URL: {}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
            return Result.error("文件上传失败");
        }
    }

    @PostMapping("/test")
    public Result<String> test() {
        log.info("测试WebSocket");
        webSocketServer.sendToAll("测试消息");
        return Result.success("发送成功");
    }
}
