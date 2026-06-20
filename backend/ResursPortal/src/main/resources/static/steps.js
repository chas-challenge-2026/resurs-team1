// Multi-step form — state stored in hidden inputs, not proper state management
// TODO: replace with a proper wizard component in v2 (React 18)
var currentStep = 1;

function showStep(n) {
    // Hide all steps, show step n — jQuery show/hide, not a real state machine
    $('.step').hide();
    $('#step' + n).show();
    currentStep = n;
    $('#hiddenStep').val(n);

    // Update step indicator — manually, not via framework
    $('.step-item').removeClass('active done');
    for (var i = 1; i < n; i++) {
        $('#ind' + i).addClass('done');
    }
    $('#ind' + n).addClass('active');

    // Scroll to top — basic UX
    window.scrollTo(0, 0);
}

function nextStep(n) {
    // No validation before proceeding — just show next step
    // TODO: validate current step before advancing
    showStep(n);
}

function prevStep(n) {
    showStep(n);
}

$(document).ready(function() {
    // Always start at step 1 — no state persistence
    showStep(1);
});
