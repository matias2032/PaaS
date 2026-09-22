import { useEffect, useState } from 'react';
import { useService } from '../hooks/useService';
import { slugify } from '../../../shared/utils/slugify';

/**
 * Create-only form for a Service, mirroring the shape of
 * OrganizationForm/ProjectsListPage's inline create form. Used by
 * CreateServicePage: `<ServiceForm projectPublicUuid={...}
 * onSuccess={...} />`.
 *
 * serviceTypeCode is populated from the ServiceType catalog (GET
 * /api/service-types via useService().serviceTypes), fetched on
 * mount here — same pattern GitConnectionsPage uses for
 * refreshGitProviders(). Not update-capable: ServiceService.
 * updateService never reads serviceTypeCode (immutable after
 * create, see service.types.js), so there's no "edit" mode for this
 * form — ServiceDetailPage's own inline edit form only touches
 * name/autoDeploy.
 */
function ServiceForm({ projectPublicUuid, onSuccess }) {
  const {
    serviceTypes,
    isServiceTypesLoading,
    serviceTypesError,
    refreshServiceTypes,
    isServicesLoading,
    servicesError,
    createService,
  } = useService();

  const [form, setForm] = useState({
    name: '',
    serviceTypeCode: '',
    autoDeploy: true,
  });
  const [submitError, setSubmitError] = useState(null);

  // Same reasoning as ProjectForm/OrganizationForm: computed from
  // `name` as the user types, shown as a read-only preview, never
  // user-editable. ServiceService.updateService ignores slug on
  // update anyway (see service.types.js), so this is a create-time-
  // only concern.
  const previewSlug = slugify(form.name);

  useEffect(() => {
    refreshServiceTypes().catch(() => {
      // Surfaced via context `serviceTypesError` already.
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError(null);
    try {
      const created = await createService(projectPublicUuid, { ...form, slug: previewSlug });
      onSuccess?.(created);
    } catch (err) {
      setSubmitError(err?.response?.data?.message || 'Failed to create service');
    }
  }

  const displayError = submitError || servicesError;
  const isBusy = isServicesLoading;

  return (
    <form className="service-form" onSubmit={handleSubmit}>
      {displayError && <p className="service-form__error">{displayError}</p>}

      <div className="service-form__field">
        <label htmlFor="service-form-name">Name</label>
        <input
          id="service-form-name"
          name="name"
          value={form.name}
          onChange={handleChange}
          required
        />
      </div>

      <div className="service-form__field">
        <label htmlFor="service-form-slug">Slug</label>
        <input id="service-form-slug" type="text" value={previewSlug} readOnly disabled />
        <p className="service-form__hint">
          Generated automatically from the name — this will be the service&apos;s permanent
          identifier within this project.
        </p>
      </div>

      <div className="service-form__field">
        <label htmlFor="service-form-type">Service type</label>
        <select
          id="service-form-type"
          name="serviceTypeCode"
          value={form.serviceTypeCode}
          onChange={handleChange}
          required
          disabled={isServiceTypesLoading || serviceTypes.length === 0}
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
        {serviceTypesError && <p className="service-form__error">{serviceTypesError}</p>}
      </div>

      <div className="service-form__field service-form__field--checkbox">
        <label htmlFor="service-form-auto-deploy">
          <input
            id="service-form-auto-deploy"
            type="checkbox"
            name="autoDeploy"
            checked={form.autoDeploy}
            onChange={handleChange}
          />
          Auto-deploy
        </label>
      </div>

      <button
        type="submit"
        className="btn btn-primary"
        disabled={isBusy || !form.serviceTypeCode || previewSlug === ''}
      >
        Create service
      </button>
    </form>
  );
}

export default ServiceForm;