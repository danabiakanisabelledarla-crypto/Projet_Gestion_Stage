document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("suivi-search-form");
    const emailInput = document.getElementById("suivi-email");
    const submitButton = document.getElementById("suivi-submit");

    if (!form || !emailInput || !submitButton) {
        return;
    }

    const updateButtonState = () => {
        if (submitButton.classList.contains("is-loading")) {
            return;
        }

        submitButton.disabled = !emailInput.validity.valid || emailInput.value.trim() === "";
    };

    emailInput.addEventListener("input", updateButtonState);
    emailInput.addEventListener("change", updateButtonState);

    form.addEventListener("submit", (event) => {
        if (!emailInput.validity.valid || emailInput.value.trim() === "") {
            event.preventDefault();
            emailInput.reportValidity();
            return;
        }

        if (submitButton.classList.contains("is-loading")) {
            event.preventDefault();
            return;
        }

        event.preventDefault();
        submitButton.classList.add("is-loading");
        submitButton.disabled = true;
        submitButton.setAttribute("aria-busy", "true");

        window.setTimeout(() => form.submit(), 1600);
    });

    updateButtonState();
});
