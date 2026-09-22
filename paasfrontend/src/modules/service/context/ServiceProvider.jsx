import { useCallback, useEffect, useState } from 'react';
import {
  listServiceTypes as listServiceTypesApi,
  createService as createServiceApi,
  listServices as listServicesApi,
  getService as getServiceApi,
  updateService as updateServiceApi,
  archiveService as archiveServiceApi,
  reactivateService as reactivateServiceApi,
  upsertServiceRepository as upsertServiceRepositoryApi,
  getServiceRepository as getServiceRepositoryApi,
  upsertServiceBuildConfig as upsertServiceBuildConfigApi,
  getServiceBuildConfig as getServiceBuildConfigApi,
  upsertServiceResourceConfig as upsertServiceResourceConfigApi,
  getServiceResourceConfig as getServiceResourceConfigApi,
  upsertEnvironmentVariable as upsertEnvironmentVariableApi,
  listEnvironmentVariables as listEnvironmentVariablesApi,
  deleteEnvironmentVariable as deleteEnvironmentVariableApi,
  createDomain as createDomainApi,
  listDomains as listDomainsApi,
  deleteDomain as deleteDomainApi,
  createDeployment as createDeploymentApi,
  listDeployments as listDeploymentsApi,
  getDeployment as getDeploymentApi,
} from '../api/serviceApi';
import { ServiceContext } from './ServiceContext';

export function ServiceProvider({ children }) {
  // Service types catalog (GET /api/service-types) — global,
  // read-only, seeded server-side (WEB_SERVICE/STATIC_SITE/WORKER/
  // DATABASE/CRON_JOB). Mirrors ProjectProvider's `gitProviders`:
  // fetched via refreshServiceTypes() and kept until the next
  // refresh, since it practically never changes.
  const [serviceTypes, setServiceTypes] = useState([]);
  const [isServiceTypesLoading, setIsServiceTypesLoading] = useState(false);
  const [serviceTypesError, setServiceTypesError] = useState(null);

  // Covers createService/listServices/getService/updateService/
  // archiveService/reactivateService. No service list is cached here
  // — see ServiceContext.js for why.
  const [isServicesLoading, setIsServicesLoading] = useState(false);
  const [servicesError, setServicesError] = useState(null);

  // Covers upsertServiceRepository/getServiceRepository.
  const [isRepositoryLoading, setIsRepositoryLoading] = useState(false);
  const [repositoryError, setRepositoryError] = useState(null);

  // Covers upsertServiceBuildConfig/getServiceBuildConfig.
  const [isBuildConfigLoading, setIsBuildConfigLoading] = useState(false);
  const [buildConfigError, setBuildConfigError] = useState(null);

  // Covers upsertServiceResourceConfig/getServiceResourceConfig.
  const [isResourceConfigLoading, setIsResourceConfigLoading] = useState(false);
  const [resourceConfigError, setResourceConfigError] = useState(null);

  // Covers upsertEnvironmentVariable/listEnvironmentVariables/
  // deleteEnvironmentVariable.
  const [isEnvironmentVariablesLoading, setIsEnvironmentVariablesLoading] = useState(false);
  const [environmentVariablesError, setEnvironmentVariablesError] = useState(null);

  // Covers createDomain/listDomains/deleteDomain.
  const [isDomainsLoading, setIsDomainsLoading] = useState(false);
  const [domainsError, setDomainsError] = useState(null);

  // Covers createDeployment/listDeployments/getDeployment.
  const [isDeploymentsLoading, setIsDeploymentsLoading] = useState(false);
  const [deploymentsError, setDeploymentsError] = useState(null);

  // Same global event every provider reacts to (dispatched by
  // httpClient on a real 401) — clears all service state so a
  // subsequent login never sees stale catalog/error data from a
  // previous session.
  useEffect(() => {
    function handleUnauthorized() {
      setServiceTypes([]);
      setServiceTypesError(null);
      setServicesError(null);
      setRepositoryError(null);
      setBuildConfigError(null);
      setResourceConfigError(null);
      setEnvironmentVariablesError(null);
      setDomainsError(null);
      setDeploymentsError(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // ---- Service types (catalog) ----

  const refreshServiceTypes = useCallback(async () => {
    setIsServiceTypesLoading(true);
    setServiceTypesError(null);
    try {
      const list = await listServiceTypesApi();
      setServiceTypes(list);
      return list;
    } catch (err) {
      setServiceTypesError(err?.response?.data?.message || 'Failed to load service types');
      throw err;
    } finally {
      setIsServiceTypesLoading(false);
    }
  }, []);

  // ---- Services (project-scoped — caller owns the list) ----

  async function fetchServices(projectPublicUuid) {
    setServicesError(null);
    try {
      return await listServicesApi(projectPublicUuid);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to load services');
      throw err;
    }
  }

  async function fetchService(publicUuid) {
    setServicesError(null);
    try {
      return await getServiceApi(publicUuid);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to load service');
      throw err;
    }
  }

  async function createService(projectPublicUuid, data) {
    setIsServicesLoading(true);
    setServicesError(null);
    try {
      return await createServiceApi(projectPublicUuid, data);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to create service');
      throw err;
    } finally {
      setIsServicesLoading(false);
    }
  }

  async function updateService(publicUuid, data) {
    setIsServicesLoading(true);
    setServicesError(null);
    try {
      return await updateServiceApi(publicUuid, data);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to update service');
      throw err;
    } finally {
      setIsServicesLoading(false);
    }
  }

  async function archiveService(publicUuid) {
    setIsServicesLoading(true);
    setServicesError(null);
    try {
      return await archiveServiceApi(publicUuid);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to archive service');
      throw err;
    } finally {
      setIsServicesLoading(false);
    }
  }

  // Mirrors archiveService exactly (always lands back on CREATED).
  async function reactivateService(publicUuid) {
    setIsServicesLoading(true);
    setServicesError(null);
    try {
      return await reactivateServiceApi(publicUuid);
    } catch (err) {
      setServicesError(err?.response?.data?.message || 'Failed to reactivate service');
      throw err;
    } finally {
      setIsServicesLoading(false);
    }
  }

  // ---- Repository (1:1 — caller owns the "not configured" state) ----

  // Does not clear repositoryError on a 404 "not configured" result —
  // callers should catch 404 themselves and treat it as an empty
  // state, not as an error to surface. Same pattern applies to
  // fetchServiceBuildConfig/fetchServiceResourceConfig below.
  async function fetchServiceRepository(servicePublicUuid) {
    setRepositoryError(null);
    try {
      return await getServiceRepositoryApi(servicePublicUuid);
    } catch (err) {
      if (err?.response?.status !== 404) {
        setRepositoryError(err?.response?.data?.message || 'Failed to load repository settings');
      }
      throw err;
    }
  }

  async function upsertServiceRepository(servicePublicUuid, data) {
    setIsRepositoryLoading(true);
    setRepositoryError(null);
    try {
      return await upsertServiceRepositoryApi(servicePublicUuid, data);
    } catch (err) {
      setRepositoryError(err?.response?.data?.message || 'Failed to save repository settings');
      throw err;
    } finally {
      setIsRepositoryLoading(false);
    }
  }

  // ---- Build config (1:1) ----

  async function fetchServiceBuildConfig(servicePublicUuid) {
    setBuildConfigError(null);
    try {
      return await getServiceBuildConfigApi(servicePublicUuid);
    } catch (err) {
      if (err?.response?.status !== 404) {
        setBuildConfigError(err?.response?.data?.message || 'Failed to load build config');
      }
      throw err;
    }
  }

  async function upsertServiceBuildConfig(servicePublicUuid, data) {
    setIsBuildConfigLoading(true);
    setBuildConfigError(null);
    try {
      return await upsertServiceBuildConfigApi(servicePublicUuid, data);
    } catch (err) {
      setBuildConfigError(err?.response?.data?.message || 'Failed to save build config');
      throw err;
    } finally {
      setIsBuildConfigLoading(false);
    }
  }

  // ---- Resource config (1:1) ----

  async function fetchServiceResourceConfig(servicePublicUuid) {
    setResourceConfigError(null);
    try {
      return await getServiceResourceConfigApi(servicePublicUuid);
    } catch (err) {
      if (err?.response?.status !== 404) {
        setResourceConfigError(err?.response?.data?.message || 'Failed to load resource config');
      }
      throw err;
    }
  }

  async function upsertServiceResourceConfig(servicePublicUuid, data) {
    setIsResourceConfigLoading(true);
    setResourceConfigError(null);
    try {
      return await upsertServiceResourceConfigApi(servicePublicUuid, data);
    } catch (err) {
      setResourceConfigError(err?.response?.data?.message || 'Failed to save resource config');
      throw err;
    } finally {
      setIsResourceConfigLoading(false);
    }
  }

  // ---- Environment variables (N — caller owns the list) ----

  async function fetchEnvironmentVariables(servicePublicUuid) {
    setEnvironmentVariablesError(null);
    try {
      return await listEnvironmentVariablesApi(servicePublicUuid);
    } catch (err) {
      setEnvironmentVariablesError(
        err?.response?.data?.message || 'Failed to load environment variables'
      );
      throw err;
    }
  }

  async function upsertEnvironmentVariable(servicePublicUuid, data) {
    setIsEnvironmentVariablesLoading(true);
    setEnvironmentVariablesError(null);
    try {
      return await upsertEnvironmentVariableApi(servicePublicUuid, data);
    } catch (err) {
      setEnvironmentVariablesError(
        err?.response?.data?.message || 'Failed to save environment variable'
      );
      throw err;
    } finally {
      setIsEnvironmentVariablesLoading(false);
    }
  }

  async function deleteEnvironmentVariable(servicePublicUuid, variableKey) {
    setIsEnvironmentVariablesLoading(true);
    setEnvironmentVariablesError(null);
    try {
      await deleteEnvironmentVariableApi(servicePublicUuid, variableKey);
    } catch (err) {
      setEnvironmentVariablesError(
        err?.response?.data?.message || 'Failed to delete environment variable'
      );
      throw err;
    } finally {
      setIsEnvironmentVariablesLoading(false);
    }
  }

  // ---- Domains (N — caller owns the list) ----

  async function fetchDomains(servicePublicUuid) {
    setDomainsError(null);
    try {
      return await listDomainsApi(servicePublicUuid);
    } catch (err) {
      setDomainsError(err?.response?.data?.message || 'Failed to load domains');
      throw err;
    }
  }

  async function createDomain(servicePublicUuid, data) {
    setIsDomainsLoading(true);
    setDomainsError(null);
    try {
      return await createDomainApi(servicePublicUuid, data);
    } catch (err) {
      setDomainsError(err?.response?.data?.message || 'Failed to create domain');
      throw err;
    } finally {
      setIsDomainsLoading(false);
    }
  }

  async function deleteDomain(domainPublicUuid) {
    setIsDomainsLoading(true);
    setDomainsError(null);
    try {
      await deleteDomainApi(domainPublicUuid);
    } catch (err) {
      setDomainsError(err?.response?.data?.message || 'Failed to delete domain');
      throw err;
    } finally {
      setIsDomainsLoading(false);
    }
  }

  // ---- Deployments (N — caller owns the list) ----

  async function fetchDeployments(servicePublicUuid) {
    setDeploymentsError(null);
    try {
      return await listDeploymentsApi(servicePublicUuid);
    } catch (err) {
      setDeploymentsError(err?.response?.data?.message || 'Failed to load deployments');
      throw err;
    }
  }

  async function fetchDeployment(publicUuid) {
    setDeploymentsError(null);
    try {
      return await getDeploymentApi(publicUuid);
    } catch (err) {
      setDeploymentsError(err?.response?.data?.message || 'Failed to load deployment');
      throw err;
    }
  }

  // "Log a deployment", not "Deploy now" — see createDeployment in
  // serviceApi.js and DeploymentRequest in service.types.js.
  async function createDeployment(servicePublicUuid, data) {
    setIsDeploymentsLoading(true);
    setDeploymentsError(null);
    try {
      return await createDeploymentApi(servicePublicUuid, data);
    } catch (err) {
      setDeploymentsError(err?.response?.data?.message || 'Failed to log deployment');
      throw err;
    } finally {
      setIsDeploymentsLoading(false);
    }
  }

  const value = {
    serviceTypes,
    isServiceTypesLoading,
    serviceTypesError,
    refreshServiceTypes,
    isServicesLoading,
    servicesError,
    fetchServices,
    fetchService,
    createService,
    updateService,
    archiveService,
    reactivateService,
    isRepositoryLoading,
    repositoryError,
    fetchServiceRepository,
    upsertServiceRepository,
    isBuildConfigLoading,
    buildConfigError,
    fetchServiceBuildConfig,
    upsertServiceBuildConfig,
    isResourceConfigLoading,
    resourceConfigError,
    fetchServiceResourceConfig,
    upsertServiceResourceConfig,
    isEnvironmentVariablesLoading,
    environmentVariablesError,
    fetchEnvironmentVariables,
    upsertEnvironmentVariable,
    deleteEnvironmentVariable,
    isDomainsLoading,
    domainsError,
    fetchDomains,
    createDomain,
    deleteDomain,
    isDeploymentsLoading,
    deploymentsError,
    fetchDeployments,
    fetchDeployment,
    createDeployment,
  };

  return <ServiceContext.Provider value={value}>{children}</ServiceContext.Provider>;
}