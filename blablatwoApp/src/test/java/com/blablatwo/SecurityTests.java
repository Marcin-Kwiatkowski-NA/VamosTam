package com.blablatwo;


import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.user.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HelloController.class)
class SecurityTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("/hello is publicly accessible without authentication")
    void helloIsPublic() throws Exception {
        mvc.perform(get("/hello"))
                .andExpect(status().isOk());
    }
}
