<script setup lang="ts">
import { ref, onMounted } from 'vue'

const config = useRuntimeConfig()
const apiBase = config.public.apiBase

const characterInput = ref<any>(null)

const form = ref({
  character: '',
  fear: '',
  language: 'en'
})
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (characterInput.value) {
    characterInput.value.focus()
  }
})

const languages = [
  { code: 'en', label: 'English' },
  { code: 'fr', label: 'French' },
  { code: 'es', label: 'Spanish' }
]

const generateStory = async () => {
  if (!form.value.character || !form.value.fear) return

  loading.value = true
  error.value = ''

  try {
    const data = await $fetch<any>(`${apiBase}/api/story`, {
      method: 'POST',
      body: new URLSearchParams({
        characterName: form.value.character,
        fear: form.value.fear,
        language: form.value.language
      })
    })

    const workflowId = data.workflowId

    if (workflowId) {
        await navigateTo(`/story/${workflowId}`)
    } else {
        throw new Error('Could not retrieve story ID')
    }

  } catch (err) {
    error.value = "A magical error occurred... Please try again!"
    console.error(err)
    loading.value = false
  }
}
</script>

<template>
  <div class="w-full flex-grow pt-12 md:pt-24 pb-4 px-4 sm:px-6 lg:px-8 flex flex-col relative">
    <main class="w-full max-w-3xl mx-auto my-auto">
        <div class="text-center mb-12 animate-slide-up">

            <p class="text-gray-500 text-lg md:text-xl font-light">
                Create unforgettable memories for your children.
            </p>
        </div>

        <div class="glass-card rounded-3xl p-8 md:p-10 animate-fade-in relative overflow-hidden border-t border-white/10">
            
            <transition name="fade" mode="out-in">
                <div key="form">
                    <form @submit.prevent="generateStory" class="space-y-6">
                        <div class="space-y-4">
                            <div>
                                <label for="character" class="block text-sm font-medium text-gray-300 mb-2">Hero or Heroine</label>
                                <input 
                                    ref="characterInput"
                                    v-model="form.character"
                                    type="text" 
                                    id="character" 
                                    required
                                    placeholder="Ex: Leo the Lion"
                                    class="w-full px-5 py-4 bg-background/50 border border-white/10 rounded-xl focus:ring-2 focus:ring-primary-400 focus:border-transparent outline-none transition-all placeholder-gray-600 text-white shadow-inner"
                                />
                            </div>

                            <div>
                                <label for="fear" class="block text-sm font-medium text-gray-300 mb-2">Fear to face</label>
                                <input 
                                    v-model="form.fear"
                                    type="text" 
                                    id="fear" 
                                    required
                                    placeholder="Ex: The dark"
                                    class="w-full px-5 py-4 bg-background/50 border border-white/10 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all placeholder-gray-600 text-white shadow-inner"
                                />
                            </div>

                            <div>
                                <label class="block text-sm font-medium text-gray-300 mb-2">Story Language</label>
                                <div class="grid grid-cols-3 gap-3">
                                    <button 
                                        v-for="lang in languages" 
                                        :key="lang.code"
                                        type="button"
                                        @click="form.language = lang.code"
                                        :class="[
                                            'py-3 rounded-xl border transition-all duration-200 text-sm font-medium',
                                            form.language === lang.code 
                                                ? 'bg-primary-600/20 border-primary-500 text-primary-200 shadow-[0_0_15px_rgba(68,76,231,0.3)]' 
                                                : 'bg-background/30 border-white/5 text-gray-500 hover:bg-background/50 hover:text-gray-300'
                                        ]"
                                    >
                                        {{ lang.label }}
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div class="pt-4">
                            <button 
                                type="submit" 
                                :disabled="loading"
                                class="w-full py-4 px-6 bg-primary hover:bg-primary-600 text-white font-bold rounded-xl shadow-lg shadow-primary-900/20 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-3 group"
                            >
                                <span v-if="!loading" class="transition-transform duration-300 group-hover:scale-110">Generate Story</span>
                                <span v-else class="flex items-center gap-2">
                                    <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Creating...
                                </span>
                            </button>
                        </div>
                        
                        <div v-if="error" class="text-red-400 text-sm text-center bg-red-900/20 p-3 rounded-lg border border-red-500/20">
                            {{ error }}
                        </div>
                    </form>
                </div>
            </transition>
        </div>
        

    </main>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
