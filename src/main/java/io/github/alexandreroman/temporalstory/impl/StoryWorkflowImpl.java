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

package io.github.alexandreroman.temporalstory.impl;

import io.github.alexandreroman.temporalstory.Story;
import io.github.alexandreroman.temporalstory.StoryActivities;
import io.github.alexandreroman.temporalstory.StoryWorkflow;
import io.github.alexandreroman.temporalstory.StoryWorkflowState;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Implementation of the Story generation workflow.
 * This workflow orchestrates the steps to create a story: generating text,
 * creating a cover prompt, generating the cover image, and saving the result.
 */
@WorkflowImpl(taskQueues = "story-tasks")
public class StoryWorkflowImpl implements StoryWorkflow {
    private final Logger logger = LoggerFactory.getLogger(StoryWorkflowImpl.class);
    private final StoryActivities storyActivities = Workflow.newActivityStub(
            StoryActivities.class,
            ActivityOptions.newBuilder()
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());
    private StoryWorkflowState state = StoryWorkflowState.INITIALIZING;

    @Override
    public Story createStory(StoryParams params) {
        // Extract workflow ID and sanitize it (removing prefix if necessary)
        final var workflowId = Workflow.getInfo().getWorkflowId().replace("story-", "");
        logger.debug("Story workflow {} started: params={}", workflowId, params);
        try {
            return doCreateStory(workflowId, params);
        } catch (RuntimeException e) {
            setState(workflowId, StoryWorkflowState.FAILED);
            logger.warn("Story workflow {} failed", workflowId, e);
            throw e;
        }
    }

    private Story doCreateStory(String workflowId, StoryParams params) {
        // Step 1: Generate the story text based on inputs
        setState(workflowId, StoryWorkflowState.GENERATING_STORY);
        final var storyTextOnly = storyActivities.generateStory(params.characterName(), params.fear(),
                params.language());

        // Step 2: Generate a prompt for the cover image based on the story content
        setState(workflowId, StoryWorkflowState.PREPARING_COVER);
        final var coverPrompt = storyActivities.generateCoverPrompt(storyTextOnly, params.language());

        // Step 3: Generate the cover image using DALL-E (or similar)
        setState(workflowId, StoryWorkflowState.GENERATING_COVER);
        final var cover = storyActivities.generateCover(coverPrompt);

        // Step 4: Save the complete story (text + image URL)
        setState(workflowId, StoryWorkflowState.SAVING_RESULTS);
        final var story = new Story(storyTextOnly.title(), storyTextOnly.content(), cover);
        storyActivities.saveStory(story);

        setState(workflowId, StoryWorkflowState.COMPLETED);
        return story;
    }

    @Override
    public StoryWorkflowState getState() {
        return state;
    }

    private void setState(String workflowId, StoryWorkflowState state) {
        this.state = state;
        logger.debug("Story workflow {} state changed to {}", workflowId, state);
    }
}
