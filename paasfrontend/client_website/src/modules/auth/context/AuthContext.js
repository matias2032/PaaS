import { createContext } from 'react';

/**
 * Raw context object. Kept in its own file (no components here) so that
 * both AuthProvider and useAuth can import it without breaking Vite's
 * Fast Refresh, which requires .jsx files to only export components.
 */
export const AuthContext = createContext(null);