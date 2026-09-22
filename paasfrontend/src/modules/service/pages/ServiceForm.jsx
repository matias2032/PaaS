import { useEffect, useState } from 'react';
import { useService } from '../hooks/useService';
import { slugify } from '../../../shared/utils/slugify';
// Both ServiceForm and ProjectForm live at modules/<module>/components/,
// so these relative paths ('../hooks', '../../../shared/') are
// unchanged from where the file was originally drafted.

/**
 * Create-only form for a service — same reasoning as ProjectForm for
 * not handling edit mode: ServiceDetailPage will have its own inline
 * name/autoDeploy edit, and slug is never touched there either.
 *
 * Unlike ProjectForm, this also owns loading the serviceTypes catalog
 * (via refreshServiceTypes() on mount) — Service has no equivalent of
 * a GitConnectionsPage-style dedicated catalog page, and ServiceForm
 * is the only place in the module that needs the catalog, so it's
 * simpler to fetch it here rather than push that responsibility up
 * into CreateServicePage for no other consumer.
 *
 * Slug is never user-editable, same reasoning as ProjectForm: computed
 * from `name` via slugify() as the user types, shown as a read-only
 * preview. ServiceService.updateService ignores slug on update anyway.
 *
 * @param {{ projectPublicUuid: string, onSuccess?: (service: import('../types/service.types').ServiceResponse) => void }} props
 */
function ServiceForm({ projectPublicUuid, onSuccess }) {
  const {
    createService,
    isServicesLoading,
    servicesError,
    serviceTypes,
    isServiceTypesLoading,
    serviceTypesError,
    refreshServiceTypes,
  } = useService();

  const [name, setName] = useState('');
  const [serviceTypeCode, setServiceTypeCode] = useState('');
  const [autoDeploy, setAutoDeploy] = useState(false);

  const previewSlug = slugify(name);

  useEffect(() => {
    refreshServiceTypes().catch(() => {
      // Surfaced via context `serviceTypesError` already.
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();

    try {
      const created = await createService(projectPublicUuid, {
        name,
        slug: previewSlug,
        serviceTypeCode,
        autoDeploy,
      });
      onSuccess?.(created);
    } catch {
      // Error is already captured in context state (`servicesError`)
      // and rendered below — nothing else to do here.
    }
  }

  const displayError = servicesError || serviceTypesError;

  return (
    <form className="service-form" onSubmit={handleSubmit}>
      <div className="service-form__field">
        <label htmlFor="service-name">Name</label>
        <input
          id="service-name"
          type="text"
          value={name}
          maxLength={150}
          required
          onChange={(event) => setName(event.target.value)}
        />
      </div>

      <div className="service-form__field">
        <label htmlFor="service-slug">Slug</label>
        <input id="service-slug" type="text" value={previewSlug} readOnly disabled />
        <p className="service-form__hint">
          Generated automatically from the name — this will be the service's permanent
          identifier within this project.
        </p>
      </div>

      <div className="service-form__field">
        <label htmlFor="service-type">Type</label>
        <select
          id="service-type"
          value={serviceTypeCode}
          required
          disabled={isServiceTypesLoading || serviceTypes.length === 0}
          onChange={(event) => setServiceTypeCode(event.target.value)}
        >
          <option value="" disabled>
            Select a type
          </option>
          {serviceTypes.map((type) => (
            <option key={type.idServiceType} value={type.code}>
              {type.name}
            </option>
          ))}
        </select>
        <p className="service-form__hint">
          The service's type can't be changed after creation.
        </p>
      </div>

      <div className="service-form__field service-form__field--checkbox">
        <label htmlFor="service-auto-deploy">
          <input
            id="service-auto-deploy"
            type="checkbox"
            checked={autoDeploy}
            onChange={(event) => setAutoDeploy(event.target.checked)}
          />
          Auto-deploy
        </label>
      </div>

      {displayError && <p className="service-form__error">{displayError}</p>}

      <button
        type="submit"
        disabled={isServicesLoading || previewSlug === '' || serviceTypeCode === ''}
      >
        Create service
      </button>
    </form>
  );
}

export default ServiceForm;