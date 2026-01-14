<script setup lang="ts">
const route = useRoute()
const config = useRuntimeConfig()
const apiBase = config.public.apiBase

const storyId = route.params.id as string

const loading = ref(true)
const story = ref<any>(null)
const error = ref('')
const progressValue = ref(0)
const progressMessage = ref('')
const imageLoaded = ref(false)

const stateMap: Record<string, { percent: number; text: string }> = {
  'IDLE': { percent: 0, text: 'Awakening the magic...' },
  'INITIALIZING': { percent: 10, text: 'Summoning the story sprites...' },
  'GENERATING_STORY': { percent: 40, text: 'Weaving a tale of wonder...' },
  'PREPARING_COVER': { percent: 60, text: 'Gathering stardust for the picture...' },
  'GENERATING_COVER': { percent: 80, text: 'Painting your dreams...' },
  'SAVING_RESULTS': { percent: 95, text: 'Sprinkling final fairy dust...' },
  'COMPLETED': { percent: 100, text: 'Your adventure awaits!' },
  'FAILED': { percent: 100, text: 'Oops, the magic wand slipped!' }
}

const updateProgress = (state: string) => {
    const info = stateMap[state]
    if (info) {
        progressValue.value = info.percent
        progressMessage.value = info.text
    }
}

const pollStatus = async () => {
    loading.value = true
    progressValue.value = 0
    progressMessage.value = 'Abracadabra...'
    
    try {
        let completed = false
        while (!completed) {
            const data = await $fetch<any>(`${apiBase}/api/story/${storyId}`)
            
            updateProgress(data.state)

            if (data.state === 'COMPLETED') {
                story.value = data.story
                completed = true
            } else if (data.state === 'FAILED') {
                throw new Error('Story generation failed')
            } else {
                // Wait 1 second before next poll
                await new Promise(resolve => setTimeout(resolve, 1000))
            }
        }
    } catch (err) {
        console.error(err)
        error.value = "The magic spell fizzled out. Let's try casting it again!"
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    pollStatus()
})
</script>

<template>
  <div class="w-full flex-grow py-24 flex items-center justify-center relative overflow-hidden">
    <main class="w-full max-w-7xl">
        <div class="animate-fade-in relative min-h-[50vh] flex flex-col justify-center">
            
            <!-- Loading View -->
            <div v-if="loading" class="text-center py-12 max-w-2xl mx-auto w-full px-4 sm:px-6 lg:px-8">
                <div class="mb-8 relative max-w-sm mx-auto w-full h-4 bg-white/10 rounded-full overflow-hidden backdrop-blur-sm border border-white/5">
                    <div class="absolute top-0 left-0 h-full bg-primary-500 transition-all duration-700 ease-out shadow-[0_0_10px_rgba(68,76,231,0.5)]" 
                            :style="{ width: `${progressValue}%` }">
                    </div>
                </div>
                <div class="space-y-3">
                    <p class="text-xl md:text-2xl font-medium text-white tracking-wide animate-pulse">
                        {{ progressMessage }}
                    </p>

                </div>
            </div>

            <!-- Error View -->
            <div v-else-if="error" class="text-center space-y-6 max-w-2xl mx-auto w-full px-4 sm:px-6 lg:px-8">
                <div class="p-6 bg-red-900/20 border border-red-500/20 rounded-2xl inline-block">
                    <p class="text-red-300 text-lg">{{ error }}</p>
                </div>
                <div>
                     <NuxtLink 
                        to="/"
                        class="px-8 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-sm font-medium transition-colors text-white"
                    >
                        Try Again
                    </NuxtLink>
                </div>
            </div>

            <!-- Story View -->
            <div v-else-if="story" class="animate-fade-in flex flex-col space-y-8">
                
                <!-- Title & Restart Button -->
                <div class="flex items-center justify-between gap-4 max-w-4xl mx-auto w-full px-4 sm:px-6 lg:px-8">
                    <div class="relative group flex-1 min-w-0">
                        <h1 class="font-display text-2xl md:text-3xl font-bold text-white tracking-tight leading-tight text-left truncate cursor-help">
                            {{ story.title }}
                        </h1>
                        <div class="absolute left-0 top-full mt-2 hidden group-hover:block w-max max-w-[calc(100vw-4rem)] md:max-w-md p-3 rounded-xl bg-gray-900/95 backdrop-blur-md border border-white/10 text-sm text-white shadow-xl z-20 whitespace-normal animate-fade-in">
                            {{ story.title }}
                        </div>
                    </div>
                    
                    <NuxtLink 
                        to="/" 
                        class="group p-3 bg-white/10 hover:bg-white/20 border border-white/10 rounded-full text-white transition-all hover:scale-105 shadow-lg backdrop-blur-md flex-shrink-0"
                        title="Start Over"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 transition-transform duration-700 group-hover:rotate-[360deg]">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                        </svg>
                    </NuxtLink>
                </div>

                <!-- Illustration -->
                <div class="w-full lg:max-w-4xl lg:mx-auto lg:px-8">
                     <div v-if="story.cover" class="relative w-full group lg:rounded-2xl lg:overflow-hidden">
                          <div v-if="!imageLoaded" class="w-full h-[500px] bg-white/5 animate-pulse rounded-lg lg:rounded-none"></div>
                          <img 
                              :src="story.cover.url" 
                              :alt="story.title" 
                              class="w-full max-h-[500px] object-cover shadow-2xl transition-opacity duration-500" 
                              :class="[imageLoaded ? 'opacity-100' : 'opacity-0 absolute top-0 left-0 pointer-events-none']"
                              @load="imageLoaded = true"
                          />
                     </div>
                     <div v-else class="w-full aspect-video bg-black/20 flex items-center justify-center border-y lg:border border-white/10 text-white/30 lg:rounded-2xl">
                         Waiting for illustration...
                     </div>
                </div>

                <!-- Content -->
                <div class="prose prose-invert max-w-4xl mx-auto leading-relaxed text-gray-200 px-4 sm:px-6 lg:px-8">
                     <div class="whitespace-pre-line font-serif text-lg md:text-xl text-justify">
                         {{ story.content }}
                     </div>
                </div>

                <div class="text-center pb-8 pt-4">
                    <div class="text-xs text-white/40 font-medium tracking-wide flex items-center justify-center gap-3">
                        <span>powered by</span>
                        <img src="/images/spring-ai.png" alt="Spring AI" width="181" height="35" class="h-5 w-auto" />
                        <span>and</span>
                        <img src="/images/temporal.png" alt="Temporal" width="169" height="45" class="h-6 w-auto" />
                    </div>
                </div>
            </div>
        </div>
    </main>
  </div>
</template>
