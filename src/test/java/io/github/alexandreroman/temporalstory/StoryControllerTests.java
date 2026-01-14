/*
 * Copyright (c) 2026 Alexandre Roman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.alexandreroman.temporalstory;

import com.redis.testcontainers.RedisContainer;
import io.github.alexandreroman.temporalstory.impl.TemporalTestConfig;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.junit.jupiter.Container;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Application.class, TemporalTestConfig.class})
@ActiveProfiles("tests")
class StoryControllerTests {
    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag("8.4"));

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private TestWorkflowEnvironment testEnv;

    @Autowired
    private TestRestTemplate client;

    @MockitoBean
    private StoryActivities storyActivities;

    @Autowired
    private StoryRepository storyRepository;

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    void createStory() {
        when(storyActivities.generateStory(anyString(), anyString(), anyString()))
                .thenReturn(new Story("Title", "Story", null));
        when(storyActivities.generateCoverPrompt(any(Story.class), anyString()))
                .thenReturn("A prompt");
        when(storyActivities.generateCover(anyString()))
                .thenReturn(new StoryCover("http://foo.bar", 32, 32));
        //doNothing().when(storyActivities).saveStory(any(Story.class));

        final var params = new LinkedMultiValueMap<String, String>();
        params.add("characterName", "Alex");
        params.add("fear", "PHP");
        params.add("language", "French");

        final var respCreate = client.postForEntity("/api/story", params, Map.class);
        assertThat(respCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respCreate.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        final var respValues = respCreate.getBody();
        assertThat(respValues).containsKey("workflowId");
        final var workflowId = (String) respValues.get("workflowId");
        final var storyPath = respCreate.getHeaders().getLocation().getPath();
        assertThat(storyPath).isEqualTo(String.format("/api/story/%s", workflowId));

        testEnv.awaitTermination(2, TimeUnit.SECONDS);

        // Simulate the end of the workflow by storing a story.
        storyRepository.saveStory(workflowId, new Story("Title", "Story", new StoryCover("http://foo.bar", 32, 32)));

        final var respStory = client.getForEntity(storyPath, StoryController.StoryProgress.class);
        assertThat(respStory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respStory.getBody().state()).isEqualTo(StoryWorkflowState.COMPLETED);
        assertThat(respStory.getBody().story()).isNotNull();
    }
}
