import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Address, Invoice, Issue, IssueComment, IssuePriority, Project, Quotation,
} from '../../shared/models/portal.models';

@Injectable({ providedIn: 'root' })
export class PortalService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/portal`;

  listQuotations(): Observable<Quotation[]> {
    return this.http.get<Quotation[]>(`${this.base}/quotations`);
  }

  acceptQuotation(id: string): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotations/${id}/accept`, {});
  }

  listInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.base}/invoices`);
  }

  listProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.base}/projects`);
  }

  getProject(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.base}/projects/${id}`);
  }

  listIssues(): Observable<Issue[]> {
    return this.http.get<Issue[]>(`${this.base}/issues`);
  }

  getIssue(id: string): Observable<Issue> {
    return this.http.get<Issue>(`${this.base}/issues/${id}`);
  }

  createIssue(subject: string, description: string, priority: IssuePriority): Observable<Issue> {
    return this.http.post<Issue>(`${this.base}/issues`, { subject, description, priority });
  }

  addIssueComment(issueId: string, message: string): Observable<IssueComment> {
    return this.http.post<IssueComment>(`${this.base}/issues/${issueId}/comments`, { message });
  }

  listAddresses(): Observable<Address[]> {
    return this.http.get<Address[]>(`${this.base}/addresses`);
  }

  saveAddress(addr: Address): Observable<Address> {
    if (this.isNewAddress(addr.id)) {
      return this.http.post<Address>(`${this.base}/addresses`, addr);
    }
    return this.http.put<Address>(`${this.base}/addresses/${addr.id}`, addr);
  }

  deleteAddress(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/addresses/${id}`);
  }

  private isNewAddress(id: string): boolean {
    return !id || id.startsWith('new-');
  }
}
