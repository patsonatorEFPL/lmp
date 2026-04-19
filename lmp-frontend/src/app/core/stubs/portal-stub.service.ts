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

const SEED_QUOTATIONS: Quotation[] = [
  {
    id: 'QTN-2026-0007', title: 'Refonte site vitrine + SEO on-page',
    transactionDate: '2026-04-10', validTill: '2026-05-10', status: 'Open', currency: 'CAD',
    items: [
      { itemCode: 'WEB-001', description: 'Refonte site vitrine (10 pages)', qty: 1, rate: 3500, amount: 3500 },
      { itemCode: 'SEO-002', description: 'Audit SEO + optimisation on-page', qty: 1, rate: 800, amount: 800 },
    ],
    netTotal: 4300, taxAmount: 645, grandTotal: 4945,
    terms: 'Acompte 40% à la signature, solde à la livraison.', ...mkSync(true),
  },
  {
    id: 'QTN-2026-0006', title: 'Campagne Google Ads — trimestre 2',
    transactionDate: '2026-04-02', validTill: '2026-04-20', status: 'Replied', currency: 'CAD',
    items: [{ itemCode: 'ADS-010', description: 'Gestion Google Ads — 3 mois', qty: 3, rate: 750, amount: 2250 }],
    netTotal: 2250, taxAmount: 337.5, grandTotal: 2587.5,
    terms: 'Budget média facturé séparément.', ...mkSync(true),
  },
  {
    id: 'QTN-2026-0005', title: 'Pack démarrage — Fiche Google My Business',
    transactionDate: '2026-03-25', validTill: '2026-04-25', status: 'Ordered', currency: 'CAD',
    items: [{ itemCode: 'GMB-100', description: 'Configuration GMB + 10 posts', qty: 1, rate: 490, amount: 490 }],
    netTotal: 490, taxAmount: 73.5, grandTotal: 563.5,
    terms: 'Facturation après livraison.', ...mkSync(true),
  },
  {
    id: 'QTN-2026-0004', title: 'Audit e-commerce Shopify',
    transactionDate: '2026-03-12', validTill: '2026-03-30', status: 'Expired', currency: 'CAD',
    items: [{ itemCode: 'AUD-002', description: 'Audit technique Shopify + recommandations', qty: 1, rate: 1200, amount: 1200 }],
    netTotal: 1200, taxAmount: 180, grandTotal: 1380, terms: '', ...mkSync(true),
  },
  {
    id: 'QTN-2026-0003', title: 'Identité visuelle + logo',
    transactionDate: '2026-02-28', validTill: '2026-03-28', status: 'Lost', currency: 'CAD',
    items: [{ itemCode: 'BRD-001', description: 'Création logo + charte graphique', qty: 1, rate: 1800, amount: 1800 }],
    netTotal: 1800, taxAmount: 270, grandTotal: 2070, terms: '', ...mkSync(false),
  },
];

const SEED_INVOICES: Invoice[] = [
  {
    id: 'INV-2026-0012', postingDate: '2026-04-01', dueDate: '2026-05-01',
    status: 'Unpaid', currency: 'CAD',
    items: [{ itemCode: 'SEO-MTH', description: 'Référencement mensuel — avril 2026', qty: 1, rate: 990, amount: 990 }],
    grandTotal: 1138.5, outstandingAmount: 1138.5, paidAmount: 0, isReturn: false,
    pdfUrl: '/stub/invoices/INV-2026-0012.pdf', paymentUrl: '/payment/stub/INV-2026-0012', ...mkSync(true),
  },
  {
    id: 'INV-2026-0011', postingDate: '2026-03-28', dueDate: '2026-03-15',
    status: 'Overdue', currency: 'CAD',
    items: [{ itemCode: 'ADS-010', description: 'Gestion Google Ads — mars 2026', qty: 1, rate: 750, amount: 750 }],
    grandTotal: 862.5, outstandingAmount: 862.5, paidAmount: 0, isReturn: false,
    pdfUrl: '/stub/invoices/INV-2026-0011.pdf', paymentUrl: '/payment/stub/INV-2026-0011', ...mkSync(true),
  },
  {
    id: 'INV-2026-0010', postingDate: '2026-03-10', dueDate: '2026-04-10',
    status: 'Partly Paid', currency: 'CAD',
    items: [{ itemCode: 'WEB-001', description: 'Refonte site — acompte 40%', qty: 1, rate: 1400, amount: 1400 }],
    grandTotal: 1610, outstandingAmount: 805, paidAmount: 805, isReturn: false,
    pdfUrl: '/stub/invoices/INV-2026-0010.pdf', paymentUrl: '/payment/stub/INV-2026-0010', ...mkSync(true),
  },
  {
    id: 'INV-2026-0009', postingDate: '2026-02-15', dueDate: '2026-03-15',
    status: 'Paid', currency: 'CAD',
    items: [{ itemCode: 'SEO-MTH', description: 'Référencement mensuel — février 2026', qty: 1, rate: 990, amount: 990 }],
    grandTotal: 1138.5, outstandingAmount: 0, paidAmount: 1138.5, isReturn: false,
    pdfUrl: '/stub/invoices/INV-2026-0009.pdf', paymentUrl: null, ...mkSync(true),
  },
  {
    id: 'INV-2026-0008', postingDate: '2026-01-20', dueDate: '2026-02-20',
    status: 'Paid', currency: 'CAD',
    items: [{ itemCode: 'GMB-100', description: 'Configuration GMB', qty: 1, rate: 490, amount: 490 }],
    grandTotal: 563.5, outstandingAmount: 0, paidAmount: 563.5, isReturn: false,
    pdfUrl: '/stub/invoices/INV-2026-0008.pdf', paymentUrl: null, ...mkSync(true),
  },
];

const SEED_PROJECTS: Project[] = [
  {
    id: 'PROJ-2026-001', projectName: 'Refonte site — Boulangerie Aurore', status: 'Open',
    priority: 'High', percentComplete: 62,
    expectedStartDate: '2026-03-15', expectedEndDate: '2026-05-30', projectType: 'Site Web',
    description: 'Refonte complète du site vitrine avec module de commande en ligne et intégration Google My Business.',
    tasks: [
      { id: 'T-001', subject: 'Maquettes desktop + mobile', status: 'Completed', assignedTo: 'Léa M.', expEndDate: '2026-03-28', modified: '2026-03-26T10:00:00Z' },
      { id: 'T-002', subject: 'Intégration front Angular', status: 'Working', assignedTo: 'Karim B.', expEndDate: '2026-04-20', modified: '2026-04-14T16:30:00Z' },
      { id: 'T-003', subject: 'Back-office & CMS', status: 'Open', assignedTo: 'Karim B.', expEndDate: '2026-05-05', modified: '2026-04-10T09:15:00Z' },
      { id: 'T-004', subject: 'SEO on-page + contenu', status: 'Pending Review', assignedTo: 'Sophie L.', expEndDate: '2026-05-15', modified: '2026-04-12T14:22:00Z' },
      { id: 'T-005', subject: 'Mise en production', status: 'Open', assignedTo: null, expEndDate: '2026-05-30', modified: '2026-03-15T08:00:00Z' },
    ],
    timesheets: [
      { id: 'TS-1', activityType: 'Design', fromTime: '2026-03-20T09:00:00Z', toTime: '2026-03-20T17:00:00Z', hours: 7.5, status: 'Billed' },
      { id: 'TS-2', activityType: 'Développement', fromTime: '2026-04-02T10:00:00Z', toTime: '2026-04-02T18:00:00Z', hours: 7, status: 'Submitted' },
      { id: 'TS-3', activityType: 'Développement', fromTime: '2026-04-10T09:00:00Z', toTime: '2026-04-10T17:30:00Z', hours: 8, status: 'Submitted' },
    ],
    attachments: [
      { id: 'A-1', fileName: 'maquettes-v2.pdf', fileUrl: '/stub/files/maquettes-v2.pdf', fileSize: 2_450_000, uploadedAt: '2026-03-27T11:00:00Z' },
      { id: 'A-2', fileName: 'audit-seo-initial.pdf', fileUrl: '/stub/files/audit-seo-initial.pdf', fileSize: 890_000, uploadedAt: '2026-03-18T09:30:00Z' },
    ],
    ...mkSync(true),
  },
  {
    id: 'PROJ-2026-002', projectName: 'Stratégie SEO — Cabinet Dentaire Nord', status: 'Open',
    priority: 'Medium', percentComplete: 35,
    expectedStartDate: '2026-04-01', expectedEndDate: '2026-07-31', projectType: 'SEO',
    description: 'Programme de référencement sur 4 mois avec création de contenu et netlinking local.',
    tasks: [
      { id: 'T-101', subject: 'Audit SEO technique', status: 'Completed', assignedTo: 'Sophie L.', expEndDate: '2026-04-10', modified: '2026-04-09T11:00:00Z' },
      { id: 'T-102', subject: 'Recherche mots-clés', status: 'Completed', assignedTo: 'Sophie L.', expEndDate: '2026-04-15', modified: '2026-04-14T15:00:00Z' },
      { id: 'T-103', subject: 'Rédaction 10 articles', status: 'Working', assignedTo: 'Marie C.', expEndDate: '2026-05-20', modified: '2026-04-15T10:45:00Z' },
      { id: 'T-104', subject: 'Netlinking local', status: 'Open', assignedTo: null, expEndDate: '2026-06-30', modified: '2026-04-01T08:00:00Z' },
    ],
    timesheets: [{ id: 'TS-11', activityType: 'SEO', fromTime: '2026-04-05T09:00:00Z', toTime: '2026-04-05T13:00:00Z', hours: 4, status: 'Billed' }],
    attachments: [{ id: 'A-11', fileName: 'audit-seo-cabinet.pdf', fileUrl: '/stub/files/audit-seo-cabinet.pdf', fileSize: 1_120_000, uploadedAt: '2026-04-10T10:00:00Z' }],
    ...mkSync(true),
  },
  {
    id: 'PROJ-2026-003', projectName: 'Campagne Meta Ads — Boutique Zaza', status: 'Completed',
    priority: 'Medium', percentComplete: 100,
    expectedStartDate: '2026-01-15', expectedEndDate: '2026-03-15', projectType: 'Paid Ads',
    description: 'Campagne Facebook + Instagram de 60 jours pour la boutique Zaza.',
    tasks: [
      { id: 'T-201', subject: 'Création visuels', status: 'Completed', assignedTo: 'Léa M.', expEndDate: '2026-01-25', modified: '2026-01-24T14:00:00Z' },
      { id: 'T-202', subject: 'Setup campagnes Meta', status: 'Completed', assignedTo: 'Karim B.', expEndDate: '2026-02-01', modified: '2026-01-31T11:30:00Z' },
      { id: 'T-203', subject: 'Rapport final', status: 'Completed', assignedTo: 'Sophie L.', expEndDate: '2026-03-15', modified: '2026-03-14T16:00:00Z' },
    ],
    timesheets: [],
    attachments: [{ id: 'A-21', fileName: 'rapport-final-meta-ads.pdf', fileUrl: '/stub/files/rapport-final.pdf', fileSize: 3_200_000, uploadedAt: '2026-03-15T17:00:00Z' }],
    ...mkSync(true),
  },
  {
    id: 'PROJ-2026-004', projectName: 'Landing — Lancement produit XYZ', status: 'Cancelled',
    priority: 'Low', percentComplete: 15,
    expectedStartDate: '2026-02-01', expectedEndDate: '2026-03-01', projectType: 'Site Web',
    description: 'Page de lancement annulée par le client.',
    tasks: [], timesheets: [], attachments: [], ...mkSync(true),
  },
];

const SEED_ISSUES: Issue[] = [
  {
    id: 'ISS-0014', subject: 'Formulaire de contact ne renvoie plus d\'email',
    status: 'Open', priority: 'High', issueType: 'Bug',
    description: 'Depuis hier, les messages reçus via le formulaire du site ne sont plus envoyés dans ma boîte mail. J\'ai reçu 3 leads perdus.',
    raisedBy: 'marie@boulangerie-aurore.ca', openingDate: '2026-04-14T09:30:00Z',
    resolutionDate: null, resolutionByHours: 4,
    comments: [
      { id: 'C-1', author: 'Marie D.', authorRole: 'customer', message: 'Urgent SVP, c\'est ma principale source de leads.', createdAt: '2026-04-14T09:32:00Z' },
      { id: 'C-2', author: 'Karim B.', authorRole: 'agent', message: 'Bonjour Marie, je regarde immédiatement. Je reviens vers vous dans l\'heure.', createdAt: '2026-04-14T09:45:00Z' },
    ],
    ...mkSync(true),
  },
  {
    id: 'ISS-0013', subject: 'Demande ajout page "Équipe" sur le site',
    status: 'Replied', priority: 'Medium', issueType: 'Demande',
    description: 'Nous voudrions ajouter une page présentant les 4 membres de l\'équipe avec photos et bios.',
    raisedBy: 'contact@cabinet-dentaire-nord.ca', openingDate: '2026-04-10T14:00:00Z',
    resolutionDate: null, resolutionByHours: 48,
    comments: [
      { id: 'C-11', author: 'Dr. Chen', authorRole: 'customer', message: 'Pouvez-vous nous donner une estimation ?', createdAt: '2026-04-10T14:02:00Z' },
      { id: 'C-12', author: 'Léa M.', authorRole: 'agent', message: 'Bonjour, c\'est inclus dans votre forfait. Prévoir 2 jours après réception des photos et bios.', createdAt: '2026-04-11T10:15:00Z' },
    ],
    ...mkSync(true),
  },
  {
    id: 'ISS-0012', subject: 'Certificat SSL à renouveler',
    status: 'Resolved', priority: 'Medium', issueType: 'Maintenance',
    description: 'Le certificat du domaine expire le 30 avril.',
    raisedBy: 'marie@boulangerie-aurore.ca', openingDate: '2026-04-05T11:00:00Z',
    resolutionDate: '2026-04-06T09:00:00Z', resolutionByHours: 24,
    comments: [{ id: 'C-21', author: 'Karim B.', authorRole: 'agent', message: 'Renouvellement automatique activé, valide jusqu\'au 30 avril 2027.', createdAt: '2026-04-06T09:00:00Z' }],
    ...mkSync(true),
  },
  {
    id: 'ISS-0011', subject: 'Question facturation — acompte refonte',
    status: 'Closed', priority: 'Low', issueType: 'Question',
    description: 'Quand le solde de 60% sera-t-il facturé ?',
    raisedBy: 'marie@boulangerie-aurore.ca', openingDate: '2026-03-28T10:30:00Z',
    resolutionDate: '2026-03-28T14:00:00Z', resolutionByHours: 24,
    comments: [{ id: 'C-31', author: 'Sophie L.', authorRole: 'agent', message: 'Facturation à la recette finale du site, prévue fin mai.', createdAt: '2026-03-28T14:00:00Z' }],
    ...mkSync(true),
  },
];

const SEED_ADDRESSES: Address[] = [
  {
    id: 'ADDR-001', addressTitle: 'Siège social', addressType: 'Billing',
    addressLine1: '1205 Rue de la Rivière', addressLine2: 'Bureau 200',
    city: 'Québec', state: 'QC', pincode: 'G1V 2A3', country: 'Canada',
    phone: '+1 418 555 0199', emailId: 'facturation@boulangerie-aurore.ca',
    isPrimaryAddress: true, isShippingAddress: false, ...mkSync(true),
  },
  {
    id: 'ADDR-002', addressTitle: 'Boutique principale', addressType: 'Shipping',
    addressLine1: '88 Avenue Cartier', addressLine2: null,
    city: 'Québec', state: 'QC', pincode: 'G1R 1Y4', country: 'Canada',
    phone: '+1 418 555 0112', emailId: null,
    isPrimaryAddress: false, isShippingAddress: true, ...mkSync(true),
  },
];
