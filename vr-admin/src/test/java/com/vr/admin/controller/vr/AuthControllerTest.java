package com.vr.admin.controller.vr;

import com.vr.admin.VRApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = VRApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("admin 登录成功获取 token")
    void loginSuccess() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("错误密码返回 401")
    void loginWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("不存在的用户返回 401")
    void loginNoUser() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("修改密码-原密码错误拒绝")
    void changePasswordWrongOld() throws Exception {
        String token = loginAsAdmin();
        mvc.perform(put("/api/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"new123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("无 token 访问保护接口返回 403")
    void noTokenAccessDenied() throws Exception {
        mvc.perform(get("/api/admin/tenants"))
            .andExpect(status().isForbidden());
    }

    private String loginAsAdmin() throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.data.token");
    }
}
