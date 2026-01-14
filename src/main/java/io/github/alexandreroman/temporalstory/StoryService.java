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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
class StoryService {
    private final Logger logger = LoggerFactory.getLogger(StoryService.class);
    private final WorkflowClient workflowClient;
    private final StoryRepository storyRepository;

    StoryService(WorkflowClient workflowClient, StoryRepository storyRepository) {
        this.workflowClient = workflowClient;
        this.storyRepository = storyRepository;
    }

    String generateStory(String characterName, String fear, String language) {
        final var workflowId = UUID.randomUUID().toString();
        final var workflowOptions = WorkflowOptions.newBuilder()
                .setTaskQueue("story-tasks")
                .setWorkflowId(getStoryWorkflowId(workflowId))
                .build();

        logger.debug("Looking up workflow with id {}", workflowId);
        final var workflow = workflowClient.newWorkflowStub(StoryWorkflow.class, workflowOptions);

        final var workflowParams = new StoryWorkflow.StoryParams(characterName, fear, language);
        logger.info("Starting story workflow with id {}: params={}", workflowId, workflowParams);
        WorkflowClient.start(workflow::createStory, workflowParams);
        return workflowId;
    }

    StoryWorkflowState getState(String workflowId) {
        final var untypedStub = workflowClient.newUntypedWorkflowStub(getStoryWorkflowId(workflowId));
        return untypedStub.query("getState", StoryWorkflowState.class);
    }

    private String getStoryWorkflowId(String workflowId) {
        return "story-" + workflowId;
    }

    Optional<Story> getStory(String workflowId) {
        return storyRepository.getStory(workflowId);
    }
}
