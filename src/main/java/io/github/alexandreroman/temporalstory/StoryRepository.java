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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StoryRepository {
    private final Logger logger = LoggerFactory.getLogger(StoryRepository.class);
    private final StringRedisTemplate redis;

    StoryRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String getKey(String workflowId) {
        return String.format("temporal-story:stories:%s", workflowId);
    }

    public void saveStory(String workflowId, Story story) {
        if (workflowId == null) {
            throw new IllegalArgumentException("workflowId cannot be null");
        }
        if (story == null) {
            throw new IllegalArgumentException("story cannot be null");
        }
        logger.debug("Saving story for workflow {}", workflowId);
        final var key = getKey(workflowId);
        final var values = Map.of(
                "title", story.title(),
                "content", story.content(),
                "coverUrl", story.cover().url(),
                "coverWidth", String.valueOf(story.cover().width()),
                "coverHeight", String.valueOf(story.cover().height())
        );
        redis.opsForHash().putAll(key, values);
    }

    public Optional<Story> getStory(String workflowId) {
        if (workflowId == null) {
            throw new IllegalArgumentException("workflowId cannot be null");
        }
        logger.debug("Loading story for workflow {}", workflowId);
        final var key = getKey(workflowId);
        final var values = redis.opsForHash().multiGet(key,
                List.of("title", "content", "coverUrl", "coverWidth", "coverHeight"));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        final var title = (String) values.get(0);
        final var content = (String) values.get(1);
        final var coverUrl = (String) values.get(2);
        final var coverWidthStr = (String) values.get(3);
        final var coverHeightStr = (String) values.get(4);
        if (title == null || content == null || coverUrl == null || coverWidthStr == null || coverHeightStr == null) {
            return Optional.empty();
        }
        return Optional.of(new Story(title, content, new StoryCover(coverUrl, Integer.parseInt(coverWidthStr), Integer.parseInt(coverHeightStr))));
    }
}
