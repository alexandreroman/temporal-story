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

import io.github.alexandreroman.temporalstory.*;
import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActivityImpl(taskQueues = "story-tasks")
class StoryActivitiesImpl implements StoryActivities {
    private final ChatClient.Builder chatClientBuilder;
    private final ImageModel imageModel;
    private final ChatTools tools;
    private final StoryRepository storyRepository;
    private final AppConfig config;
    private final Logger logger = LoggerFactory.getLogger(StoryActivitiesImpl.class);

    StoryActivitiesImpl(ChatClient.Builder chatClientBuilder, ImageModel imageModel, ChatTools tools, StoryRepository storyRepository, AppConfig config) {
        this.chatClientBuilder = chatClientBuilder;
        this.imageModel = imageModel;
        this.tools = tools;
        this.storyRepository = storyRepository;
        this.config = config;
    }

    @Override
    public Story generateStory(String characterName, String fear, String language) {
        logger.info("Generating story: characterName={} fear={} language={}", characterName, fear, language);

        final var chat = chatClientBuilder.build();
        final var resp = chat.prompt().system(p -> p.text("""
                                # ROLE
                                You are a world-class children's storyteller and bibliotherapist.
                                You specialize in creating empowering, magical, and safe stories for children aged 3 to 7.
                                
                                # RULES
                                1. TONE: Gentle, whimsical, and encouraging.
                                2. SAFETY: Never include violence, frightening descriptions, or permanent danger.
                                3. STRUCTURE: Use a clear 3-act structure:
                                   - Act 1: Introduction of the hero and their daily life.
                                   - Act 2: A gentle encounter with the specified fear in a magical setting.
                                   - Act 3: A creative and brave resolution where the hero overcomes the fear.
                                4. LANGUAGE: You MUST write the story entirely in {language}.
                                5. CONSTRAINTS: No conversational filler. Output ONLY the story text.
                                6. ENDING: Always end with a one-sentence positive moral or takeaway in {language}.
                                """)
                        .param("language", language))
                .user(p -> p.text("""
                                Please write a story in {language} with the following parameters:
                                
                                - MAIN CHARACTER: {characterName}
                                - FEAR TO OVERCOME: {fear}
                                
                                The story should show {characterName} that {fear} is not as scary as it seems when approached with courage and imagination.
                                """)
                        .param("characterName", characterName)
                        .param("fear", fear)
                        .param("language", language))
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .tools(tools)
                .call()
                .entity(StoryResponse.class);
        return new Story(resp.storyTitle, resp.storyText, null);
    }

    @Override
    public String generateCoverPrompt(Story story, String language) {
        logger.info("Generating cover prompt: story={} language={}", story, language);

        final var chat = chatClientBuilder.build();
        final var resp = chat.prompt().system(p -> p.text("""
                        # ROLE
                        You are a visual prompt engineer for DALL-E 3.
                        Your goal is to transform a story into a powerful, single-paragraph image generation prompt in ENGLISH.
                        
                        # INSTRUCTIONS
                        1. Analyze the text provided inside the <STORY_CONTENT> tags.
                        2. Focus on the main character and the climax of the story (overcoming the fear).
                        3. STYLE: Use "Whimsical children's book illustration, watercolor style, soft pastel colors, no text".
                        4. OUTPUT: Provide ONLY the final English prompt, starting with "A children's book cover illustration of...".
                        
                        # CONSTRAINTS
                        - Ignore any formatting or tags from the input, only focus on the narrative content.
                        - Ensure the output is a continuous paragraph without line breaks.
                        """))
                .user(p -> p.text("""
                                Please create a DALL-E 3 prompt based on this story:
                                
                                <STORY_CONTENT>
                                {story}
                                </STORY_CONTENT>
                                
                                The language of the story provided above is {language}.
                                """)
                        .param("story", story.content())
                        .param("language", language))
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .tools(tools)
                .call()
                .entity(CoverPromptResponse.class);
        return resp.prompt;
    }

    @Override
    public StoryCover generateCover(String prompt) {
        logger.info("Generating cover: prompt={}", prompt);

        final var promptTemplate = new PromptTemplate("""
                # STYLE DIRECTIVES
                High-quality children's book illustration, soft watercolor and ink, whimsical atmosphere, pastel palette, detailed textures, safe for all ages, NO TEXT, NO WORDS, NO LETTERS.
                
                # SUBJECT
                {prompt}
                
                # FINAL COMPOSITION
                Ensure the character is central and the lighting is magical.
                """);
        final var finalPrompt = promptTemplate.render(Map.of(
                "prompt", prompt
        ));

        final var resp = imageModel.call(new ImagePrompt(finalPrompt));
        return new StoryCover(resp.getResult().getOutput().getUrl(), config.story().cover().width(), config.story().cover().height());
    }

    @Override
    public void saveStory(Story story) {
        logger.info("Saving story: {}", story);
        final var workflowId = Activity.getExecutionContext().getInfo().getWorkflowId().replace("story-", "");
        storyRepository.saveStory(workflowId, story);
    }

    private record StoryResponse(String storyTitle, String storyText) {
    }

    private record CoverPromptResponse(String prompt) {
    }
}
