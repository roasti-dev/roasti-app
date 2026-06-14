// place files you want to import through the `$lib` alias in this folder.
import { PUBLIC_API_HOST } from '$env/static/public'
import { createAuthApiClient } from 'shared'

console.log(PUBLIC_API_HOST)
export const authApiClient = createAuthApiClient(() => null, PUBLIC_API_HOST, false);