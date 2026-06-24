/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // OmniCharge Brand Palette — Premium Stripe/Linear Aesthetic
        omni: {
          50:  '#EEF2FF', // Very light indigo for subtle active backgrounds
          100: '#E0E7FF', // Light indigo for hover on muted primary elements
          200: '#C7D2FE',
          300: '#A5B4FC', // Focus rings
          400: '#818CF8',
          500: '#6366F1',
          600: '#4F46E5', // Primary Brand (Deep Indigo)
          700: '#4338CA', // Primary Hover
          800: '#3730A3', // Primary Active
          900: '#312E81', 
          950: '#1E1B4B', 
        },
        surface: {
          DEFAULT: '#FCFCFD',  // Ultra-light premium background
          50:  '#FCFCFD',
          100: '#F1F5F9',      // Subtle form backgrounds
          200: '#E2E8F0',      // Borders
          300: '#CBD5E1',      // Strong borders
          400: '#94A3B8',      // Muted text
          500: '#64748B',      // Secondary text
          600: '#475569',      // Body text
          700: '#334155',
          800: '#1E293B',
          900: '#0F172A',      // Heading Text
          950: '#020617',
        },
        accent: {
          teal:    '#0D9488', // Wealth/Growth
          emerald: '#059669', // Success
          amber:   '#D97706', // Warning
          rose:    '#DC2626', // Error
          sky:     '#0284C7', // Info
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        display: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'hero-glow': 'radial-gradient(ellipse 80% 50% at 50% -20%, rgba(198, 93, 59, 0.15), transparent)',
        'card-shine': 'linear-gradient(135deg, rgba(255,255,255,0.8) 0%, transparent 50%, rgba(255,255,255,0.5) 100%)',
      },
      boxShadow: {
        'glow':      '0 0 20px rgba(79, 70, 229, 0.1)',
        'glow-lg':   '0 0 40px rgba(79, 70, 229, 0.15)',
        'glow-teal': '0 0 20px rgba(13, 148, 136, 0.1)',
        'elevated':  '0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05)',
        'card':      '0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02), 0 0 0 1px rgba(15, 23, 42, 0.03)',
        'premium':   '0 4px 14px 0 rgba(79, 70, 229, 0.39)', // Premium Stripe CTA shadow
      },
      animation: {
        'fade-in':       'fadeIn 0.5s ease-out',
        'slide-up':      'slideUp 0.5s ease-out',
        'slide-down':    'slideDown 0.3s ease-out',
        'scale-in':      'scaleIn 0.3s ease-out',
        'pulse-slow':    'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'shimmer':       'shimmer 2s linear infinite',
        'float':         'float 6s ease-in-out infinite',
        'glow-pulse':    'glowPulse 2s ease-in-out infinite',
      },
      keyframes: {
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%':   { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%':   { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%':   { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%':      { transform: 'translateY(-10px)' },
        },
        glowPulse: {
          '0%, 100%': { boxShadow: '0 0 20px rgba(0, 0, 0, 0.1)' },
          '50%':      { boxShadow: '0 0 40px rgba(0, 0, 0, 0.15)' },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
