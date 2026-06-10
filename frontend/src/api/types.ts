export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ReportRequest {
  jobTitle: string;
  team: string;
  category: string;
  severity: Severity;
  blockerText: string;
}

export interface ReportResponse {
  id: string;
  jobTitle: string;
  team: string;
  category: string;
  severity: Severity;
  blockerText: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface QueryRequest {
  question: string;
  jobTitle?: string;
  team?: string;
  category?: string;
  severity?: Severity;
}

export interface ReportSnippet {
  id: string;
  jobTitle: string;
  team: string;
  category: string;
  severity: Severity;
  blockerText: string;
  createdAt: string;
}

export interface QueryResponse {
  summary: string;
  supportingReports: ReportSnippet[];
  totalMatches: number;
}

export interface TrendPoint {
  label: string;
  count: number;
}

export interface TrendResponse {
  groupBy: string;
  data: TrendPoint[];
}

export interface HealthResponse {
  status: string;
  ollama: string;
}

export interface ValidationError {
  status: number;
  error: string;
  fieldErrors: Record<string, string>;
}
