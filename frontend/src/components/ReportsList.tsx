import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchReports } from '../api/client';
import type { Severity } from '../api/types';

const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const PAGE_SIZE = 10;

function severityBadge(severity: Severity) {
  const cls = `severity severity-${severity.toLowerCase()}`;
  return <span className={cls}>{severity}</span>;
}

export default function ReportsList() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    jobTitle: '',
    team: '',
    category: '',
    severity: '',
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['reports', page, filters],
    queryFn: () => fetchReports({ page, size: PAGE_SIZE, ...filters }),
  });

  function handleFilterChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
    setPage(0);
  }

  return (
    <div className="reports-list">
      <h2>Friction Reports</h2>

      <div className="filters">
        <input name="team" value={filters.team} onChange={handleFilterChange} placeholder="Filter by team" />
        <input name="category" value={filters.category} onChange={handleFilterChange} placeholder="Filter by category" />
        <select name="severity" value={filters.severity} onChange={handleFilterChange}>
          <option value="">All severities</option>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      {isLoading && <p className="loading">Loading reports…</p>}
      {isError && <p className="error-msg">Failed to load reports.</p>}

      {data && data.content.length === 0 && <p className="empty">No reports found.</p>}

      {data && data.content.length > 0 && (
        <>
          <div className="reports-grid">
            {data.content.map((report) => (
              <div key={report.id} className="report-card">
                <div className="report-header">
                  {severityBadge(report.severity)}
                  <span className="report-category">{report.category}</span>
                  <time className="report-time">{new Date(report.createdAt).toLocaleDateString()}</time>
                </div>
                <p className="report-text">{report.blockerText}</p>
                <div className="report-meta">
                  <span>{report.jobTitle}</span>
                  <span className="separator">·</span>
                  <span>{report.team}</span>
                </div>
              </div>
            ))}
          </div>

          <div className="pagination">
            <button onClick={() => setPage(page - 1)} disabled={page === 0}>
              ← Previous
            </button>
            <span>
              Page {data.number + 1} of {data.totalPages}
            </span>
            <button onClick={() => setPage(page + 1)} disabled={page >= data.totalPages - 1}>
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}
