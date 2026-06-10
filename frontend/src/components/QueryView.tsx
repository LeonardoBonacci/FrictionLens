import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { queryReports } from '../api/client';
import type { QueryResponse, Severity } from '../api/types';

function severityBadge(severity: Severity) {
  const cls = `severity severity-${severity.toLowerCase()}`;
  return <span className={cls}>{severity}</span>;
}

export default function QueryView() {
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState<QueryResponse | null>(null);

  const mutation = useMutation({
    mutationFn: queryReports,
    onSuccess: (data) => setResult(data),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!question.trim()) return;
    mutation.mutate({ question });
  }

  return (
    <div className="query-view">
      <h2>Ask a Question</h2>
      <p className="query-subtitle">Ask anything about workplace friction — powered by AI</p>

      <form className="query-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. What are the biggest tooling problems across teams?"
          disabled={mutation.isPending}
        />
        <button type="submit" disabled={mutation.isPending || !question.trim()}>
          {mutation.isPending ? 'Analyzing…' : 'Search'}
        </button>
      </form>

      {mutation.isPending && (
        <div className="loading-indicator">
          <div className="spinner" />
          <p>Searching reports and generating summary…</p>
        </div>
      )}

      {mutation.isError && <p className="error-msg">Query failed. Please try again.</p>}

      {result && (
        <div className="query-results">
          <div className="summary-card">
            <h3>Summary</h3>
            <p>{result.summary}</p>
            <span className="match-count">{result.totalMatches} matching reports</span>
          </div>

          {result.supportingReports.length > 0 && (
            <div className="supporting-reports">
              <h3>Supporting Reports</h3>
              {result.supportingReports.map((report) => (
                <div key={report.id} className="report-card">
                  <div className="report-header">
                    {severityBadge(report.severity)}
                    <span className="report-category">{report.category}</span>
                    <time className="report-time">
                      {new Date(report.createdAt).toLocaleDateString()}
                    </time>
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
          )}
        </div>
      )}
    </div>
  );
}
