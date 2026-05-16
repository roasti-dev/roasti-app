import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	optimizeDeps: {
		// KMP generates ESM (.mjs) — Vite's pre-bundling breaks named exports
    	exclude: ['shared'],
		force: true,
    },
	server: {
        proxy: {
            '/api': {
                target: 'https://api.roasti.ru',
                changeOrigin: true,
            }
        }
    },
});
