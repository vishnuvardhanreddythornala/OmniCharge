/**
 * Production environment configuration for Azure VM deployment.
 * All API calls route through the Nginx reverse proxy on the VM.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://omnicharge.centralindia.cloudapp.azure.com',
  razorpayKeyId: 'rzp_live_SgV8VMOB5gIRcd',
  googleClientId: '115505952714-he9njstpsdplqdifcjv2sil686qjrncq.apps.googleusercontent.com',
};
