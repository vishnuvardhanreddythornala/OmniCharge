/**
 * Development environment configuration.
 * All API calls route through the Spring Cloud Gateway on port 8080.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  razorpayKeyId: 'rzp_live_SgV8VMOB5gIRcd', // Public Live Key
  googleClientId: '1084384475158-8sc296epqd9fj6hkplr0725hgbs6sl8h.apps.googleusercontent.com', // Google OAuth Client ID
};
