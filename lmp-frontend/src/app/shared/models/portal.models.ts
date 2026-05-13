export type SyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';

interface SyncMetadata {
  externalCrmId: string | null;
  syncStatus: SyncStatus;
  lastSyncedAt: string | null;
}

export type QuotationStatus =
  | 'Draft' | 'Open' | 'Replied' | 'Partially Ordered'
  | 'Ordered' | 'Lost' | 'Cancelled' | 'Expired';

export interface QuotationItem {
  itemCode: string;
  description: string;
  qty: number;
  rate: number;
  amount: number;
}

export interface Quotation extends SyncMetadata {
  id: string;
  title: string;
  transactionDate: string;
  validTill: string;
  status: QuotationStatus;
  currency: string;
  items: QuotationItem[];
  netTotal: number;
  taxAmount: number;
  grandTotal: number;
  terms: string;
}

export type InvoiceStatus =
  | 'Draft' | 'Submitted' | 'Paid' | 'Partly Paid' | 'Unpaid'
  | 'Overdue' | 'Return' | 'Credit Note Issued' | 'Cancelled';

export interface InvoiceItem {
  itemCode: string;
  description: string;
  qty: number;
  rate: number;
  amount: number;
}

export interface Invoice extends SyncMetadata {
  id: string;
  postingDate: string;
  dueDate: string;
  status: InvoiceStatus;
  currency: string;
  items: InvoiceItem[];
  grandTotal: number;
  outstandingAmount: number;
  paidAmount: number;
  isReturn: boolean;
  pdfUrl: string | null;
  paymentUrl: string | null;
}

export type ProjectStatus = 'Open' | 'Completed' | 'Cancelled';
export type ProjectPriority = 'Low' | 'Medium' | 'High';
export type TaskStatus = 'Open' | 'Working' | 'Pending Review' | 'Completed' | 'Cancelled';

export interface ProjectTask {
  id: string;
  subject: string;
  status: TaskStatus;
  assignedTo: string | null;
  expEndDate: string | null;
  modified: string;
}

export interface ProjectTimesheet {
  id: string;
  activityType: string;
  fromTime: string;
  toTime: string;
  hours: number;
  status: 'Draft' | 'Submitted' | 'Billed';
}

export interface ProjectAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  uploadedAt: string;
}

export interface Project extends SyncMetadata {
  id: string;
  projectName: string;
  status: ProjectStatus;
  priority: ProjectPriority;
  percentComplete: number;
  expectedStartDate: string | null;
  expectedEndDate: string | null;
  projectType: string | null;
  description: string;
  tasks: ProjectTask[];
  timesheets: ProjectTimesheet[];
  attachments: ProjectAttachment[];
}

export type IssueStatus = 'Open' | 'Replied' | 'On Hold' | 'Resolved' | 'Closed';
export type IssuePriority = 'Low' | 'Medium' | 'High' | 'Urgent';

export interface IssueComment {
  id: string;
  author: string;
  authorRole: 'customer' | 'agent';
  message: string;
  createdAt: string;
}

export interface Issue extends SyncMetadata {
  id: string;
  subject: string;
  status: IssueStatus;
  priority: IssuePriority;
  issueType: string;
  description: string;
  raisedBy: string;
  openingDate: string;
  resolutionDate: string | null;
  resolutionByHours: number | null;
  comments: IssueComment[];
}

export type AddressType = 'Billing' | 'Shipping' | 'Office' | 'Personal' | 'Other';

export interface Address extends SyncMetadata {
  id: string;
  addressTitle: string;
  addressType: AddressType;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  pincode: string;
  country: string;
  phone: string | null;
  emailId: string | null;
  isPrimaryAddress: boolean;
  isShippingAddress: boolean;
}
