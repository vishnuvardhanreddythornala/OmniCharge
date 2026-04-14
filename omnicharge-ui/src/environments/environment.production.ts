/**
 * Production environment configuration for Local Docker Desktop testing.
 * All API calls route through the Spring Cloud Gateway mapped to port 8080 on the host.
 */
export const environment = {
  production: true,
  apiBaseUrl: '', // Uses relative path. Routed dynamically by Nginx Reverse Proxy
  razorpayKeyId: '', // Will be fetched from backend
  googleClientId: '1084384475158-8sc296epqd9fj6hkplr0725hgbs6sl8h.apps.googleusercontent.com', // Google OAuth Client ID
};
