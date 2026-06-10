import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { submitReport } from '../api/client';
import type { Severity, ValidationError } from '../api/types';

const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const INITIAL = {
  jobTitle: '',
  team: '',
  category: '',
  severity: 'MEDIUM' as Severity,
  blockerText: '',
};

export default function ReportForm() {
  const [form, setForm] = useState(INITIAL);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: submitReport,
    onSuccess: () => {
      setForm(INITIAL);
      setFieldErrors({});
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      queryClient.invalidateQueries({ queryKey: ['trends'] });
    },
    onError: (err: unknown) => {
      const ve = err as ValidationError;
      if (ve.fieldErrors) setFieldErrors(ve.fieldErrors);
    },
  });

  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) {
    setForm({ ...form, [e.target.name]: e.target.value });
    setFieldErrors({ ...fieldErrors, [e.target.name]: '' });
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFieldErrors({});
    mutation.mutate(form);
  }

  return (
    <form className="report-form" onSubmit={handleSubmit}>
      <h2>Report a Friction</h2>

      <label>
        Job Title
        <input name="jobTitle" value={form.jobTitle} onChange={handleChange} placeholder="e.g. Software Engineer" />
        {fieldErrors.jobTitle && <span className="field-error">{fieldErrors.jobTitle}</span>}
      </label>

      <label>
        Team
        <input name="team" value={form.team} onChange={handleChange} placeholder="e.g. Platform" />
        {fieldErrors.team && <span className="field-error">{fieldErrors.team}</span>}
      </label>

      <label>
        Category
        <input name="category" value={form.category} onChange={handleChange} placeholder="e.g. Tooling, Process" />
        {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
      </label>

      <label>
        Severity
        <select name="severity" value={form.severity} onChange={handleChange}>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        {fieldErrors.severity && <span className="field-error">{fieldErrors.severity}</span>}
      </label>

      <label>
        What's blocking you?
        <textarea
          name="blockerText"
          value={form.blockerText}
          onChange={handleChange}
          rows={5}
          placeholder="Describe the friction you're experiencing..."
        />
        {fieldErrors.blockerText && <span className="field-error">{fieldErrors.blockerText}</span>}
      </label>

      <button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? 'Submitting…' : 'Submit Report'}
      </button>

      {mutation.isSuccess && <p className="success-msg">Report submitted successfully!</p>}
      {mutation.isError && !Object.keys(fieldErrors).length && (
        <p className="error-msg">Failed to submit report. Please try again.</p>
      )}
    </form>
  );
}
