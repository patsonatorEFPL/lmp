import { Injectable, signal } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import {
  Quotation, Invoice, Project, Issue, IssueComment, Address,
} from './portal.models';

@Injectable({ providedIn: 'root' })
export class PortalStubService {
  private readonly quotations = signal<Quotation[]>(SEED_QUOTATIONS);
  private readonly invoices = signal<Invoice[]>(SEED_INVOICES);
  private readonly projects = signal<Project[]>(SEED_PROJECTS);
  private readonly issues = signal<Issue[]>(SEED_ISSUES);
  private readonly addresses = signal<Address[]>(SEED_ADDRESSES);

  private readonly LATENCY_MS = 180;

  listQuotations(): Observable<Quotation[]> {
    return of(this.quotations()).pipe(delay(this.LATENCY_MS));
  }

  listInvoices(): Observable<Invoice[]> {
    return of(this.invoices()).pipe(delay(this.LATENCY_MS));
  }

  listProjects(): Observable<Project[]> {
    return of(this.projects()).pipe(delay(this.LATENCY_MS));
  }

  getProject(id: string): Observable<Project | undefined> {
    return of(this.projects().find((p) => p.id === id)).pipe(delay(this.LATENCY_MS));
  }

  listIssues(): Observable<Issue[]> {
    return of(this.issues()).pipe(delay(this.LATENCY_MS));
  }

  getIssue(id: string): Observable<Issue | undefined> {
    return of(this.issues().find((i) => i.id === id)).pipe(delay(this.LATENCY_MS));
  }

  addIssueComment(issueId: string, message: string): Observable<IssueComment> {
    const comment: IssueComment = {
      id: crypto.randomUUID(),
      author: 'Vous',
      authorRole: 'customer',
      message,
      createdAt: new Date().toISOString(),
    };
    this.issues.update((list) =>
      list.map((i) =>
        i.id === issueId
          ? { ...i, comments: [...i.comments, comment], status: i.status === 'Resolved' ? 'Replied' : i.status }
          : i,
      ),
    );
    return of(comment).pipe(delay(this.LATENCY_MS));
  }

  createIssue(subject: string, description: string, priority: Issue['priority']): Observable<Issue> {
    const issue: Issue = {
      id: `ISS-${String(this.issues().length + 1).padStart(4, '0')}`,
      subject,
      description,
      priority,
      status: 'Open',
      issueType: 'Question',
      raisedBy: 'vous@exemple.fr',
      openingDate: new Date().toISOString(),
      resolutionDate: null,
      resolutionByHours: null,
      comments: [],
      external CRMId: null,
      syncStatus: 'PENDING',
      lastSyncedAt: null,
    };
    this.issues.update((list) => [issue, ...list]);
    return of(issue).pipe(delay(this.LATENCY_MS));
  }

  listAddresses(): Observable<Address[]> {
    return of(this.addresses()).pipe(delay(this.LATENCY_MS));
  }

  saveAddress(addr: Address): Observable<Address> {
    const exists = this.addresses().some((a) => a.id === addr.id);
    this.addresses.update((list) =>
      exists ? list.map((a) => (a.id === addr.id ? addr : a)) : [...list, addr],
    );
    return of(addr).pipe(delay(this.LATENCY_MS));
  }

  deleteAddress(id: string): Observable<void> {
    this.addresses.update((list) => list.filter((a) => a.id !== id));
    return of(undefined).pipe(delay(this.LATENCY_MS));
  }

  acceptQuotation(id: string): Observable<Quotation | undefined> {
    this.quotations.update((list) =>
      list.map((q) => (q.id === id ? { ...q, status: 'Ordered' as const } : q)),
    );
    return of(this.quotations().find((q) => q.id === id)).pipe(delay(this.LATENCY_MS));
  }
}

const mkSync = (synced: boolean): Pick<Quotation, 'external CRMId' | 'syncStatus' | 'lastSyncedAt'> =>
  synced
    ? { external CRMId: `FRP-${Math.floor(Math.random() * 99999)}`, syncStatus: 'SYNCED', lastSyncedAt: '2026-04-15T09:00:00Z' }
    : { external CRMId: null, syncStatus: 'PENDING', lastSyncedAt: null };

const SEED_QUOTATIONS: Quotation[] = [];

const SEED_INVOICES: Invoice[] = [];

const SEED_PROJECTS: Project[] = [];

const SEED_ISSUES: Issue[] = [];

const SEED_ADDRESSES: Address[] = [];
