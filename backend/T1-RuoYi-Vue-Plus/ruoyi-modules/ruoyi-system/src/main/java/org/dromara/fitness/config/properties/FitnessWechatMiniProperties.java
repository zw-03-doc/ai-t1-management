package org.dromara.fitness.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 *
 * @author CodeBuddy
 */
@Data
@Component
@ConfigurationProperties(prefix = "fitness.wechat.mini")
public class FitnessWechatMiniProperties {

    /**
     * 小程序 AppId
     */
    private String appid;

    /**
     * 小程序 AppSecret
     */
    private String secret;
}
