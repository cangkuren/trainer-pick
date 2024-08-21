package com.tencent.wxcloudrun.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LoginRequest {
    private String tel;
    private String captcha;
}
