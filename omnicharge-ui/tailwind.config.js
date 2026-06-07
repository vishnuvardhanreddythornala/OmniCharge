/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // OmniCharge Brand Palette — premium terracotta fintech aesthetic
        omni: {
          50:  '#FDE8E6', // Light Accent Background
          100: '#f9d2cc',
          200: '#f3b4ab',
          300: '#eb8f81',
          400: '#e06f5c',
          500: '#C65D3B', // Primary Accent (Terracotta Red)
          600: '#A94E32', // Primary Hover
          700: '#8b4028',
          800: '#733725',
          900: '#603123',
          950: '#34170f',
        },
        surface: {
          DEFAULT: '#FCFCFB',  // Page Background
          50:  '#FCFCFB',
          100: '#F9FAFB',
          200: '#F3F4F6',
          300: '#E5E7EB', // Borders
          400: '#D1D5DB',
          500: '#9CA3AF',
          600: '#6B7280', // Secondary Text
          700: '#4B5563',
          800: '#374151',
          900: '#1F2937', // Primary Text
          950: '#111827',
        },
        accent: {
          teal:    '#2dd4bf',
          emerald: '#16A34A', // Success
          amber:   '#D97706', // Warning
          rose:    '#DC2626', // Error
          sky:     '#38bdf8',
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
        'glow':      '0 0 20px rgba(198, 93, 59, 0.25)',
        'glow-lg':   '0 0 40px rgba(198, 93, 59, 0.2)',
        'glow-teal': '0 0 20px rgba(45, 212, 191, 0.3)',
        'elevated':  '0 12px 40px rgba(0, 0, 0, 0.12)',
        'card':      '0 8px 30px rgba(0, 0, 0, 0.08)',
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
          '0%, 100%': { boxShadow: '0 0 20px rgba(198, 93, 59, 0.25)' },
          '50%':      { boxShadow: '0 0 40px rgba(198, 93, 59, 0.5)' },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
