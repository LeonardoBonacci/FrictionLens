import type {
  ReportRequest,
  ReportResponse,
  Page,
  QueryRequest,
  QueryResponse,
  TrendResponse,
} from './types';

const BASE = '/api';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }
  return res.json();
}

export async function submitReport(data: ReportRequest): Promise<ReportResponse> {
  const res = await fetch(`${BASE}/reports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function fetchReports(params: {
  page?: number;
  size?: number;
  jobTitle?: string;
  team?: string;
  category?: string;
  severity?: string;
}): Promise<Page<ReportResponse>> {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') searchParams.set(key, String(value));
  }
  const res = await fetch(`${BASE}/reports?${searchParams}`);
  return handleResponse(res);
}

export async function queryReports(data: QueryRequest): Promise<QueryResponse> {
  const res = await fetch(`${BASE}/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function fetchTrends(groupBy?: string): Promise<TrendResponse> {
  const params = groupBy ? `?groupBy=${groupBy}` : '';
  const res = await fetch(`${BASE}/trends${params}`);
  return handleResponse(res);
}
