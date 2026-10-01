import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { usePayment } from '../hooks/usePayment';
import { useOrganization } from '../../organization/hooks/useOrganization';
import InvoiceItemsTable from '../components/InvoiceItemsTable';
import PaymentHistoryList from '../components/PaymentHistoryList';
import PaymentForm from '../components/PaymentForm';
import BackButton from '../../../shared/components/BackButton';
import {
  formatDate,
  formatMoney,
  hasPaymentInFlight,
  isInvoicePayable,
} from '../utils/paymentUtils';

/**
 * GET /api/invoices/{uuid} + GET .../members (to determine the current
 * user's role — submitting a payment is OWNER-only backend-side, see
 * PaymentService.requireOwner, so the form should never be offered to
 * someone who can't use it) + GET /api/payment-methods (catalog for the
 * form).
 *
 * orgPublicUuid comes from the URL, not from the invoice: InvoiceResponse
 * doesn't carry the organization uuid. It is only used for the OWNER
 * gate in the UI — the backend re-checks membership/ownership on every
 * call, so a mismatched URL can't grant anything.
 *
 * Like SubscriptionPage, one of the few pages allowed to import useAuth()
 * (needs the user's own publicUuid to resolve their membership/role).
 */
function InvoiceDetailPage() {
  const { orgPublicUuid, invoicePublicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchMembers } = useOrganization();
  const {
    paymentMethods,
    isPaymentMethodsLoading,
    paymentMethodsError,
    refreshPaymentMethods,
    fetchInvoice,
    isPaymentSubmitting,
    paymentError,
    submitPayment,
    clearPaymentError,
  } = usePayment();

  const [invoice, setInvoice] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [refreshError, setRefreshError] = useState(null);

  const organization = organizations.find((org) => org.publicUuid === orgPublicUuid) || null;

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      setRefreshError(null);
      clearPaymentError();
      try {
        const [invoiceData, memberList] = await Promise.all([
          fetchInvoice(invoicePublicUuid),
          fetchMembers(orgPublicUuid),
        ]);

        if (!cancelled) {
          setInvoice(invoiceData);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load invoice');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    refreshPaymentMethods().catch(() => {
      // Surfaced via context `paymentMethodsError` already.
    });
    load();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgPublicUuid, invoicePublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  // Single source of truth for the OWNER gate on this page — mirrors
  // PaymentService.requireOwner, which submitPayment calls.
  const isOwner = currentMembership?.roleCode === 'OWNER';

  async function handlePay(data) {
    setRefreshError(null);

    try {
      await submitPayment(invoicePublicUuid, data);
    } catch {
      // Surfaced via context `paymentError` already.
      return;
    }

    // The payment was created; reload the invoice so the new PENDING
    // payment shows up (and the form disappears, since a payment is now
    // in flight). A failure here is NOT a failed payment, so it gets its
    // own message instead of paymentError.
    try {
      setInvoice(await fetchInvoice(invoicePublicUuid));
    } catch (err) {
      setRefreshError(
        err?.response?.data?.message ||
          'Payment submitted, but the invoice could not be refreshed. Reload the page.'
      );
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="invoice-detail-page__error">{loadError}</p>;
  if (!invoice) return null;

  const isPayable = isInvoicePayable(invoice);
  const paymentInFlight = hasPaymentInFlight(invoice);
  const showForm = isPayable && !paymentInFlight && isOwner;

  return (
    <div className="invoice-detail-page">
      <BackButton />
      <h1>
        Invoice {invoice.invoiceNumber}
        {organization ? ` · ${organization.name}` : ''}
      </h1>

      <dl className="invoice-detail-page__details">
        <dt>Status</dt>
        <dd>{invoice.status}</dd>

        <dt>Total</dt>
        <dd>{formatMoney(invoice.totalAmount, invoice.currency)}</dd>

        <dt>Issued</dt>
        <dd>{formatDate(invoice.issuedAt)}</dd>

        <dt>Due</dt>
        <dd>{formatDate(invoice.dueAt)}</dd>

        {invoice.paidAt && (
          <>
            <dt>Paid</dt>
            <dd>{formatDate(invoice.paidAt)}</dd>
          </>
        )}
      </dl>

      <section className="invoice-detail-page__items">
        <h2>Items</h2>
        <InvoiceItemsTable
          items={invoice.items}
          totalAmount={invoice.totalAmount}
          currency={invoice.currency}
        />
      </section>

      <section className="invoice-detail-page__payments">
        <h2>Payments</h2>
        <PaymentHistoryList payments={invoice.payments} />
      </section>

      {refreshError && <p className="invoice-detail-page__error">{refreshError}</p>}

      {isPayable && (
        <section className="invoice-detail-page__pay">
          <h2>Pay this invoice</h2>

          {paymentInFlight && (
            <p className="invoice-detail-page__notice">
              A payment for this invoice is awaiting confirmation.
            </p>
          )}

          {!paymentInFlight && !isOwner && (
            <p className="invoice-detail-page__notice">
              Only the organization owner can pay invoices.
            </p>
          )}

          {paymentError && <p className="invoice-detail-page__error">{paymentError}</p>}
          {paymentMethodsError && (
            <p className="invoice-detail-page__error">{paymentMethodsError}</p>
          )}

          {showForm &&
            (isPaymentMethodsLoading && paymentMethods.length === 0 ? (
              <p>Loading payment methods...</p>
            ) : (
              <PaymentForm
                paymentMethods={paymentMethods}
                onSubmit={handlePay}
                isSubmitting={isPaymentSubmitting}
              />
            ))}
        </section>
      )}
    </div>
  );
}

export default InvoiceDetailPage;