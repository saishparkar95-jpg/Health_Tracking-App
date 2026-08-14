/**
 * HealthTrack AI - Client-Side Logic (Step 1: Project Setup)
 * Demonstrates clean, non-inline event handling and DOM manipulation
 */

// Wait for the HTML DOM to load fully before attaching listeners
document.addEventListener('DOMContentLoaded', () => {
    console.log("⚡ HealthTrack AI (Step 1) initialized successfully!");

    // Smooth scroll button to explore features
    const exploreBtn = document.getElementById('explore-btn');
    if (exploreBtn) {
        exploreBtn.addEventListener('click', () => {
            const featuresSection = document.querySelector('.features-section');
            if (featuresSection) {
                featuresSection.scrollIntoView({ behavior: 'smooth' });
            }
        });
    }

    // Card hover micro-interaction
    const featureCards = document.querySelectorAll('.feature-card');
    featureCards.forEach(card => {
        card.addEventListener('mouseenter', () => {
            card.style.transition = 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)';
        });
    });
});
