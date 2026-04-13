/**
 * Production environment configuration.
 * All API calls route through the production API gateway domain.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.omnicharge.com', // Replace with actual production domain when routing is set up
  razorpayKeyId: '', // Will be fetched from backend
  googleClientId: '1084384475158-8sc296epqd9fj6hkplr0725hgbs6sl8h.apps.googleusercontent.com', // Google OAuth Client ID
};
