<script>
  import { onMount } from 'svelte'
  import { authApiClient } from '$lib'
    import { RegisterRequestDto } from 'shared';

  onMount(async () => {
    try {
      const response = await authApiClient.register(new RegisterRequestDto(
      null,
      "hi!",
      "test@test.com",
      "test user",
      "testpassword",
      "testuser"
    ))

      console.log('User registered successfully:', response)
    } catch (error) {
      console.error('Error registering user:', error)
    }
  })
</script>

<h1>Welcome to SvelteKit</h1>
<p>Visit <a href="https://svelte.dev/docs/kit">svelte.dev/docs/kit</a> to read the documentation</p>
