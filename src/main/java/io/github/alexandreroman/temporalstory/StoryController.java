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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@CrossOrigin
class StoryController {
    private final Logger logger = LoggerFactory.getLogger(StoryController.class);
    private final StoryService storyService;

    StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    /**
     * Starts a new story generation workflow.
     *
     * @param characterName the name of the main character (defaults to "John")
     * @param fear          the fear the character faces (defaults to "Night")
     * @param language      the language of the story (defaults to "English")
     * @return a response containing the workflow ID of the started process
     */
    @PostMapping(path = "/api/story", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<NewStoryResponse> generateStory(
            @RequestParam(value = "characterName", required = false, defaultValue = "John") String characterName,
            @RequestParam(value = "fear", required = false, defaultValue = "Night") String fear,
            @RequestParam(name = "language", required = false, defaultValue = "English") String language) {
        logger.info("Creating new story: characterName={} fear={} language={}", characterName, fear, language);
        final var workflowId = storyService.generateStory(characterName, fear, language);
        return ResponseEntity.created(URI.create("/api/story/" + workflowId)).body(new NewStoryResponse(workflowId));
    }

    /**
     * Retrieves the current status or final result of a story generation workflow.
     *
     * @param workflowId the ID of the workflow to check
     * @return a response containing the current state and, if completed, the story
     *         details
     */
    @GetMapping(path = "/api/story/{workflowId}")
    ResponseEntity<?> getStory(@PathVariable("workflowId") String workflowId) {
        final var storyOpt = storyService.getStory(workflowId);
        if (storyOpt.isPresent()) {
            return ResponseEntity.ok(new StoryProgress(StoryWorkflowState.COMPLETED, storyOpt.get()));
        }

        final var state = storyService.getState(workflowId);
        return switch (state) {
            case IDLE, INITIALIZING, GENERATING_STORY, PREPARING_COVER, GENERATING_COVER, SAVING_RESULTS ->
                ResponseEntity.status(HttpStatus.ACCEPTED).body(new StoryProgress(state, null));
            case COMPLETED -> {
                if (storyOpt.isEmpty()) {
                    throw new IllegalStateException(
                            "Expecting story to have been completed for workflow " + workflowId);
                }
                yield ResponseEntity.ok(new StoryProgress(state, storyOpt.get()));
            }
            case FAILED -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get story");
        };
    }

    record NewStoryResponse(String workflowId) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record StoryProgress(StoryWorkflowState state, Story story) {
    }
}
