package com.streamhub.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.settings.dto.UserSettingsDTO;
import com.streamhub.settings.repository.UserSettingsRepository;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import com.streamhub.watchhistory.entity.WatchHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User(
                null,
                "testuser_settings",
                "settings@test.com",
                "Settings",
                "Test",
                java.time.LocalDate.of(2000, 1, 1),
                "password",
                "USER",
                "ACTIVE",
                null,
                null
        );
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "testuser_settings")
    public void testGetSettingsReturnsDefault() throws Exception {
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchHistoryEnabled").value(true))
                .andExpect(jsonPath("$.notifySocial").value(true))
                .andExpect(jsonPath("$.notifyModeration").value(true))
                .andExpect(jsonPath("$.notifySystem").value(true));
    }

    @Test
    @WithMockUser(username = "testuser_settings")
    public void testUpdateSettings() throws Exception {
        UserSettingsDTO updateDto = new UserSettingsDTO();
        updateDto.setWatchHistoryEnabled(false);
        updateDto.setNotifySocial(false);
        updateDto.setNotifyModeration(true);
        updateDto.setNotifySystem(false);

        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchHistoryEnabled").value(false))
                .andExpect(jsonPath("$.notifySocial").value(false))
                .andExpect(jsonPath("$.notifyModeration").value(true));
    }
}
