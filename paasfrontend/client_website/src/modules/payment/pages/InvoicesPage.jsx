import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { usePayment } from '../hooks/usePayment';
import { useOrganization } from '../../organization/hooks/useOrganization';
import InvoiceList from '../components/InvoiceList';
import BackButton from '../../../shared/components/BackButton';

/**
 * GET .../invoices (any member of the organization can read them).
 * Unlike SubscriptionPage this needs no members/useAuth lookup: there
 * is nothing OWNER-only on this page — paying happens on the invoice
 * detail page, which does its own OWNER gate.
 */
function InvoicesPage() {
  const { orgPublicUuid } = useParams();
  const { organizations, fetchOrganization } = useOrganization();
  const { fetchInvoices } = usePayment();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === orgPublicUuid) || null
  );
  const [invoices, setInvoices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const cachedOrg = organizations.find((org) => org.publicUuid === orgPublicUuid);
        const [org, invoiceList] = await Promise.all([
          cachedOrg ? Promise.resolve(cachedOrg) : fetchOrganization(orgPublicUuid),
          fetchInvoices(orgPublicUuid),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setInvoices(invoiceList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load invoices');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgPublicUuid]);

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="invoices-page__error">{loadError}</p>;

  return (
    <div className="invoices-page">
      <BackButton />
      <h1>Invoices{organization ? ` for ${organization.name}` : ''}</h1>

      <InvoiceList orgPublicUuid={orgPublicUuid} invoices={invoices} />

      <Link to={`/organizations/${orgPublicUuid}/subscription`} className="invoices-page__link">
        Back to billing
      </Link>
    </div>
  );
}

export default InvoicesPage;