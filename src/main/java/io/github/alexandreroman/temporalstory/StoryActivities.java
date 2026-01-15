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

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface StoryActivities {
    /**
     * Generates the text content of the story.
     * 
     * @param characterName Name of the main character.
     * @param fear          The fear to be overcome in the story.
     * @param language      The language in which to write the story.
     * @return A Story object containing title and content.
     */
    Story generateStory(String characterName, String fear, String language);

    /**
     * Creates a detailed prompt for image generation based on the story content.
     * 
     * @param story    The generated story.
     * @param language The language of the prompt (usually English for best
     *                 results).
     * @return A string description for the image generator.
     */
    String generateCoverPrompt(Story story, String language);

    /**
     * Generates an image based on the provided prompt.
     * 
     * @param prompt The image description.
     * @return A StoryCover object containing the image URL.
     */
    StoryCover generateCover(String prompt);

    /**
     * Persists the final story to the database or storage.
     * 
     * @param story The complete story object.
     */
    void saveStory(Story story);
}
