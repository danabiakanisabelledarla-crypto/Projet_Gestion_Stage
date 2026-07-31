document.addEventListener("DOMContentLoaded", () => {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const revealElements = document.querySelectorAll(".about-reveal");

    if (reducedMotion || !("IntersectionObserver" in window)) {
        revealElements.forEach((element) => element.classList.add("is-visible"));
    } else {
        const revealObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) {
                    return;
                }

                entry.target.classList.add("is-visible");
                observer.unobserve(entry.target);
            });
        }, {
            threshold: 0.14,
            rootMargin: "0px 0px -40px"
        });

        revealElements.forEach((element) => revealObserver.observe(element));
    }

    const counters = document.querySelectorAll("[data-about-counter]");

    const animateCounter = (counter) => {
        if (counter.dataset.animated === "true") {
            return;
        }

        counter.dataset.animated = "true";
        const target = Number(counter.dataset.aboutCounter);
        const suffix = counter.dataset.suffix || "";

        if (reducedMotion || !Number.isFinite(target)) {
            counter.textContent = `${target}${suffix}`;
            return;
        }

        const start = performance.now();
        const duration = 1200;

        const update = (time) => {
            const progress = Math.min((time - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            counter.textContent = `${Math.round(target * eased)}${suffix}`;

            if (progress < 1) {
                requestAnimationFrame(update);
            }
        };

        requestAnimationFrame(update);
    };

    if (!("IntersectionObserver" in window)) {
        counters.forEach(animateCounter);
        return;
    }

    const counterObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) {
                return;
            }

            animateCounter(entry.target);
            observer.unobserve(entry.target);
        });
    }, { threshold: 0.5 });

    counters.forEach((counter) => counterObserver.observe(counter));
});
