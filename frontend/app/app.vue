<template>
  <div class="min-h-[100dvh] flex flex-col relative text-white">
    <!-- Decorative elements -->
    <div class="fixed top-0 left-0 w-full h-full overflow-hidden -z-10 pointer-events-none">
        <div class="absolute top-[-10%] left-[-10%] w-96 h-96 bg-primary-500/20 rounded-full blur-[100px] animate-pulse-slow"></div>
        <div class="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-primary-900/40 rounded-full blur-[100px] animate-pulse-slow" style="animation-delay: 1s;"></div>
    </div>

    <header class="fixed top-0 left-0 right-0 z-50 p-6 flex justify-center pointer-events-none">
      <NuxtLink to="/" class="pointer-events-auto backdrop-blur-md bg-white/5 border border-white/10 px-6 py-2 rounded-full shadow-lg hover:bg-white/10 transition-all">
        <h1 class="text-lg font-display font-bold text-white tracking-wider uppercase">
          <span class="text-primary-400">Temporal</span> Story
        </h1>
      </NuxtLink>
    </header>
    <main class="flex-grow flex flex-col">
      <NuxtPage />
    </main>
    <footer class="pb-4 text-center text-sm text-white/40">
      <p>&copy; {{ new Date().getFullYear() }} Alexandre Roman. Licensed under Apache 2.0.</p>
    </footer>
  </div>
</template>
