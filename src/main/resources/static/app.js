const form = document.getElementById('quote-form');
const submitButton = document.getElementById('submit-button');
const statusEl = document.getElementById('status');
const resultEl = document.getElementById('result');
const errorEl = document.getElementById('error');

const currencyFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const percentFormat = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 2 });

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const payload = {
        loanAmount: Number(form.loanAmount.value),
        loanTermInMonths: Number(form.loanTermInMonths.value),
        riskBand: form.riskBand.value,
    };

    showLoading();

    try {
        const response = await fetch('/api/quotes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        const body = await response.json().catch(() => null);

        if (!response.ok) {
            showError(body?.message || `Request failed (HTTP ${response.status}).`);
            return;
        }
        showResult(body);
    } catch (err) {
        showError('Could not reach the server. Please check your connection and try again.');
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = 'Generate Quote';
        statusEl.hidden = true;
    }
});

function showLoading() {
    submitButton.disabled = true;
    submitButton.textContent = 'Generating…';
    statusEl.textContent = 'Generating quote…';
    statusEl.hidden = false;
    resultEl.hidden = true;
    errorEl.hidden = true;
}

function showResult(quote) {
    document.getElementById('result-quoteId').textContent = quote.quoteId;
    document.getElementById('result-commissionRate').textContent = percentFormat.format(quote.commissionRate);
    document.getElementById('result-totalCommission').textContent = currencyFormat.format(quote.totalCommission);
    resultEl.hidden = false;
}

function showError(message) {
    errorEl.textContent = message;
    errorEl.hidden = false;
}
