import { useState } from 'react';

/**
 * Presentational form to pay an invoice. Doesn't call usePayment() or
 * any API itself — the owning page (InvoiceDetailPage) decides whether
 * to render it at all (OWNER + payable invoice + no payment in flight)
 * and handles the submission.
 *
 * There is no amount field on purpose: the backend always charges the
 * invoice total, so a client can never underpay.
 *
 * TODO(gateway): with a real gateway the transaction reference stops
 * being something the customer types in, and the hint text below
 * ("confirmed manually") no longer applies.
 *
 * @param {{
 *   paymentMethods: import('../types/payment.types').PaymentMethodResponse[],
 *   onSubmit: (data: import('../types/payment.types').SubmitPaymentRequest) => void,
 *   isSubmitting?: boolean,
 * }} props
 */
function PaymentForm({ paymentMethods, onSubmit, isSubmitting = false }) {
  const [paymentMethodCode, setPaymentMethodCode] = useState('');
  const [transactionReference, setTransactionReference] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    if (!paymentMethodCode) return;

    const reference = transactionReference.trim();
    onSubmit({
      paymentMethodCode,
      // Optional server-side; omit instead of sending an empty string.
      ...(reference ? { transactionReference: reference } : {}),
    });
  }

  return (
    <form className="payment-form" onSubmit={handleSubmit}>
      <label className="payment-form__field">
        <span>Payment method</span>
        <select
          value={paymentMethodCode}
          onChange={(event) => setPaymentMethodCode(event.target.value)}
          disabled={isSubmitting}
          required
        >
          <option value="">Select a method</option>
          {paymentMethods.map((method) => (
            <option key={method.code} value={method.code}>
              {method.name}
            </option>
          ))}
        </select>
      </label>

      <label className="payment-form__field">
        <span>Transaction reference (optional)</span>
        <input
          type="text"
          value={transactionReference}
          maxLength={255}
          onChange={(event) => setTransactionReference(event.target.value)}
          disabled={isSubmitting}
        />
      </label>

      <p className="payment-form__hint">
        Payments are confirmed manually by our team after we receive them.
      </p>

      <button
        type="submit"
        className="payment-form__submit"
        disabled={isSubmitting || !paymentMethodCode}
      >
        {isSubmitting ? 'Submitting...' : 'Submit payment'}
      </button>
    </form>
  );
}

export default PaymentForm;