import { formatDate, formatMoney } from '../utils/paymentUtils';

/**
 * Presentational, read-only list of the payments attempted against one
 * invoice (newest first — sorted server-side). Shows failureReason only
 * for FAILED payments, which is the only status the backend sets it for.
 *
 * externalPaymentId is deliberately not shown: it is always null until a
 * real gateway is wired in backend-side.
 *
 * @param {{ payments: import('../types/payment.types').PaymentResponse[] }} props
 */
function PaymentHistoryList({ payments }) {
  if (payments.length === 0) {
    return <p className="payment-history-list__empty">No payments yet.</p>;
  }

  return (
    <ul className="payment-history-list">
      {payments.map((payment) => (
        <li key={payment.publicUuid} className="payment-history-list__item">
          <span className="payment-history-list__method">{payment.paymentMethodCode}</span>
          <span className="payment-history-list__amount">
            {formatMoney(payment.amount, payment.currency)}
          </span>
          <span className="payment-history-list__status">{payment.status}</span>
          <span className="payment-history-list__date">
            {payment.paidAt
              ? `Paid ${formatDate(payment.paidAt)}`
              : `Submitted ${formatDate(payment.createdAt)}`}
          </span>
          {payment.transactionReference && (
            <span className="payment-history-list__reference">
              Ref: {payment.transactionReference}
            </span>
          )}
          {payment.status === 'FAILED' && payment.failureReason && (
            <span className="payment-history-list__failure">{payment.failureReason}</span>
          )}
        </li>
      ))}
    </ul>
  );
}

export default PaymentHistoryList;