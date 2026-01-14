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

import io.github.alexandreroman.temporalstory.StoryActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TemporalTestConfig {

    @Bean
    public TestWorkflowEnvironment testWorkflowEnvironment(StoryActivities storyActivities) {
        // Crée un environnement complet (Serveur + Client) en mémoire
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();

        // On crée un Worker sur la file d'attente "story-tasks"
        Worker worker = env.newWorker("story-tasks");

        // On enregistre l'implémentation du Workflow
        worker.registerWorkflowImplementationTypes(StoryWorkflowImpl.class);

        // On enregistre l'activité (qui sera mockée dans le test)
        worker.registerActivitiesImplementations(storyActivities);

        env.start();
        return env;
    }

    @Bean
    public WorkflowClient workflowClient(TestWorkflowEnvironment env) {
        return env.getWorkflowClient();
    }
}
