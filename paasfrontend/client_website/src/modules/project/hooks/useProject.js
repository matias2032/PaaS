import { useContext } from 'react';
import { ProjectContext } from '../context/ProjectContext';

/**
 * Mirrors useBilling.js exactly: a thin context accessor that throws
 * if used outside <ProjectProvider>, so a missing provider fails
 * loudly at the call site instead of silently returning undefined.
 */
export function useProject() {
  const context = useContext(ProjectContext);

  if (context === undefined) {
    throw new Error('useProject must be used within a ProjectProvider');
  }

  return context;
}