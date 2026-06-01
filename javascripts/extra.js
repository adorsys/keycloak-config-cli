// Extra JavaScript for keycloak-config-cli documentation

// Add copy to clipboard functionality for code blocks
document.addEventListener('DOMContentLoaded', function() {
  // Highlight active nav items
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll('.md-tabs__link, .md-nav__link');
  
  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (href && currentPath.includes(href.replace('/', ''))) {
      link.classList.add('md-tabs__link--active');
    }
  });

  // Smooth scroll for anchor links
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      e.preventDefault();
      const target = document.querySelector(this.getAttribute('href'));
      if (target) {
        target.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }
    });
  });

  // Add hover effect to cards
  const cards = document.querySelectorAll('.feature-card, .guide-card, .community-card, .download-card, .blog-card');
  cards.forEach(card => {
    card.addEventListener('mouseenter', function() {
      this.style.transform = 'translateY(-4px)';
    });
    card.addEventListener('mouseleave', function() {
      this.style.transform = 'translateY(0)';
    });
  });
});

// Terminal animation for landing page
function typeTerminal(element, text, speed = 50) {
  let i = 0;
  element.textContent = '';
  
  function type() {
    if (i < text.length) {
      element.textContent += text.charAt(i);
      i++;
      setTimeout(type, speed);
    }
  }
  
  type();
}
