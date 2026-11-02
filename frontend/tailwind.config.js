/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          900: '#0a1128',
          800: '#101f42',
          700: '#1c3166',
          600: '#2c4a87',
        },
        slate: {
          950: '#090d16',
        }
      }
    },
  },
  plugins: [],
}
