import { Link } from 'react-router-dom';
import { formatDate, formatMoney } from '../utils/paymentUtils';

/**
 * Presentational, read-only list of an organization's invoices (newest
 * first — the backend already sorts them). Each row links to the
 * invoice detail page, where payment happens. No actions here.
 *
 * @param {{
 *   orgPublicUuid: string,
 *   invoices: import('../types/payment.types').InvoiceResponse[],
 * }} props
 */
function InvoiceList({ orgPublicUuid, invoices }) {
  if (invoices.length === 0) {
    return <p className="invoice-list__empty">No invoices yet.</p>;
  }

  return (
    <ul className="invoice-list">
      {invoices.map((invoice) => (
        <li key={invoice.publicUuid} className="invoice-list__item">
          <Link
            to={`/organizations/${orgPublicUuid}/invoices/${invoice.publicUuid}`}
            className="invoice-list__number"
          >
            {invoice.invoiceNumber}
          </Link>
          <span className="invoice-list__status">{invoice.status}</span>
          <span className="invoice-list__amount">
            {formatMoney(invoice.totalAmount, invoice.currency)}
          </span>
          <span className="invoice-list__due">Due {formatDate(invoice.dueAt)}</span>
        </li>
      ))}
    </ul>
  );
}

export default InvoiceList;