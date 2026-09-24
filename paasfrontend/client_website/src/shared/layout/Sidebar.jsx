import { NavLink } from 'react-router-dom';

/**
 * Generic reusable sidebar navigation. Lives in shared/layout because
 * it's app-wide chrome, not tied to any single module — each module
 * (or the app shell) passes its own nav items instead of this
 * component hardcoding "auth", "organization", etc.
 *
 * No styling yet — only structure. Class names are already in place
 * so global.css can target them once styling begins.
 */
function Sidebar({ items = [], header, footer }) {
  return (
    <aside className="sidebar">
      {header && <div className="sidebar-header">{header}</div>}

      <nav className="sidebar-nav">
        <ul className="sidebar-nav-list">
          {items.map((item) => (
            <li key={item.path} className="sidebar-nav-item">
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  isActive ? 'sidebar-nav-link active' : 'sidebar-nav-link'
                }
              >
                {item.icon && (
                  <span className="sidebar-nav-icon">{item.icon}</span>
                )}
                <span className="sidebar-nav-label">{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {footer && <div className="sidebar-footer">{footer}</div>}
    </aside>
  );
}

export default Sidebar;