# 雾雨云图床 🌁☁️

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://www.oracle.com/java/)

一款基于Spring Boot的可视化图床应用，提供高效稳定的图片托管服务。前后端分离架构，开箱即用。

本项目参考自[鱼皮](https://github.com/liyupi)的原创开源项目[智能协同云图库](https://github.com/liyupi/yu-picture)，
由[MarisaDA-ZE](https://github.com/MarisaDA-ZE)修改后上线。

📸 线上演示：https://pic.marisa.cloud

## ✨ 功能特性

- 可视化图片上传与管理
- AI图片自动审核
- 多格式图片支持（JPG/PNG等）
- 多存储策略（本地/OSS）
- 用户鉴权与权限管理
- 图片统计与访问日志
- 响应式前端界面

## 🛠️ 技术栈

**后端**
- Spring Boot 2.7
- MyBatis-Plus 3.5
- MySQL 8.0
- Redis 5.0
- Lombok
- Hutool

**前端**（[独立仓库](https://github.com/liyupi/yupi-picture-backend)）
- Vue 3
- Ant Design Vue
- Axios

## 🚀 快速开始

### 环境要求
- JDK 11+
- MySQL 8.0+
- Redis 5.0+
- Maven 3.6+

### 配置指南

1. 克隆仓库
    ```bash
    git clone https://github.com/MarisaDA-ZE/picture-backend.git
    ```
2. 配置文件准备
    将`application-template.yml`中的内容解除注释，并根据自己的需求进行修改，保存为`application.yml`，文件中大部分都有注释，跟着注释改就行。
    ```yml
     server:
      port: 8100
      servlet:
        context-path: /api
        session:
          cookie:
          max-age: 604800
     # spring 相关配置
     spring:
      main:
        allow-circular-references: true
      servlet:
        multipart:
          # 单文件最大上传体积
          max-file-size: 15MB
          # 设置总上传的文件大小
          max-request-size: 50MB
      application:
        name: picture-backend
      # 分库分表
      shardingsphere:
        datasource:
          names: picture_backend
          picture_backend:
            type: com.zaxxer.hikari.HikariDataSource
            driver-class-name: com.mysql.cj.jdbc.Driver
            url: jdbc:mysql://127.0.0.1:3306/picture_backend?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
            username: root
            password: 123456
        rules:
          sharding:
            tables:
              picture:
                # 使用动态分表
                actual-data-nodes: picture_backend.picture
                table-strategy:
                  standard:
                    sharding-column: spaceId
                    # 使用自定义分片算法
                    sharding-algorithm-name: picture_sharding_algorithm
            sharding-algorithms:
              picture_sharding_algorithm:
                type: CLASS_BASED
                props:
                  strategy: standard
                  # 自定义分片算法的实现类
                  algorithmClassName: cloud.marisa.picturebackend.sharding.PictureShardingAlgorithm
        props:
          sql-show: true
      # 数据库配置
      datasource:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/picture_backend?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
        username: root
        password: 123456
      # redis 配置
      redis:
        host: 127.0.0.1
        password: 123456
        port: 6379
        database: 0
      session:
        store-type: redis
        # session过期时间 7天（60 * 60 * 24 * 7 = 604800s）
        timeout: 604800
        
     # 日志配置
     logging:
      config: classpath:log4j2.xml
        
     # 接口文档配置
     knife4j:
      enable: true
      openapi:
        title: 雾雨云图库接口文档
        description: "雾雨云图库接口文档"
        email: admin@marisa.cloud
        concat: MarisaDAZE
        url: https://blog.marisa.cloud
        version: v1.0
        license-url: https://stackoverflow.com/
        terms-of-service-url: https://stackoverflow.com/
        group:
          default:
            group-name: DEFAULT
            api-rule: package
            api-rule-resources:
              - cloud.marisa.picturebackend.controller
        
     # mybatis-plus 配置
     mybatis-plus:
      configuration:
        map-underscore-to-camel-case: false
        # 仅在开发环境开启日志
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
      global-config:
        db-config:
          logic-delete-field: isDelete # 全局逻辑删除的实体字段名
          logic-delete-value: 1 # 逻辑已删除值（默认为 1）
          logic-not-delete-value: 0 # 逻辑未删除值（默认为 0）
        enable-sql-runner: false
        
     # minio 配置
     minio:
      url: https://example.com
      access-key: minioadmin
      secret-key: minioadmin
      bucket-name: <your-bucket-name>
      folders:
        # 图片存储文件夹
        pictures: picture
        
     # 个性化配置
     mrs:
      # 图片批量下载时的地址，支持format格式
      picture-bath-url: https://cn.bing.com/images/async?q=%s&mmasync=1
      aliyun:
        # 阿里云百炼AI开放平台
        bailian-ai:
          access-key-id: <access-key-id>
          access-key-secret: <access-key-secret>
        # 阿里云AI图片审核
        green:
          access-key-id: <access-key-id>
          access-key-secret: <access-key-secret>
          endpoint: green-cip.cn-shanghai.aliyuncs.com
        # 阿里云对象存储OSS
        oss:
          access-key-id: <access-key-id>
          access-key-secret: <access-key-secret>
          bucket-name: <your-bucket-name>
          endpoint: https://oss-cn-hangzhou.aliyuncs.com
          region: cn-hangzhou
          pictures-folder: picture/
      color-search:
        # 按颜色搜索的相似度，0 代表以前端为准，取值[0,1]
        similarity: 0.8
          # 文件相关配置
      file:
        picture:
          # 是否使用阿里云OSS进行存储，为false时使用自建MInIO存储
          is-use-oss: true
          # 图片最大体积
          image-max-size: 15MB
          # 图片大小超过多少会进行压缩
          compress-max-size: 1MB
          # 图片压缩率[0-1], 1表示不压缩，不建议低于0.5
          compress-rate: 0.8
          # 图片压缩后的类型
          #（png压不动、jpg不支持alpha通道，webp兼容性好还小）
          compress-image-type: .webp
          # 支持上传的图片格式
          image-suffix:
            - .jpg
            - .png
            - .jpeg
            - .webp
          # 支持上传的图片MIME类型
          url-image-type:
            - image/jpg
            - image/png
            - image/jpeg
            - image/webp
   ```
3. 使用maven打包项目并启动
    ```bash
      mvn clean package
      java -jar target/application.jar
    ```

## 📌 注意事项
1. 首次启动前请确保完成所有必填配置
2. 生产环境建议启用HTTPS
3. 默认端口8100，修改请调整server.port
4. 推荐使用Nginx进行反向代理

