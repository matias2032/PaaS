import { formatMoney } from '../utils/paymentUtils';

/**
 * Presentational table of an invoice's line items plus its total.
 * Uses the backend's lineTotal/totalAmount as-is: the arithmetic is
 * done server-side so clients never have to redo it.
 *
 * @param {{
 *   items: import('../types/payment.types').InvoiceItemResponse[],
 *   totalAmount: number,
 *   currency: string,
 * }} props
 */
function InvoiceItemsTable({ items, totalAmount, currency }) {
  return (
    <table className="invoice-items-table">
      <thead>
        <tr>
          <th>Description</th>
          <th>Qty</th>
          <th>Unit price</th>
          <th>Total</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item, index) => (
          // Items have no uuid in the DTO, so description + index is the
          // best stable key available (the list is never reordered).
          <tr key={`${item.description}-${index}`}>
            <td>{item.description}</td>
            <td>{item.quantity}</td>
            <td>{formatMoney(item.unitPrice, currency)}</td>
            <td>{formatMoney(item.lineTotal, currency)}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <th colSpan={3}>Total</th>
          <td>{formatMoney(totalAmount, currency)}</td>
        </tr>
      </tfoot>
    </table>
  );
}

export default InvoiceItemsTable;