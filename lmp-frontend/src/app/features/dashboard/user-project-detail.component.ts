import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideAngularModule, ArrowLeft, Loader2, Clock, CheckCircle, XCircle,
  AlertTriangle, Download, FileIcon, ListChecks, Timer,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { PortalStubService } from '../../core/stubs/portal-stub.service';
import { Project, ProjectStatus, ProjectPriority, TaskStatus } from '../../core/stubs/portal.models';

type StatusTone = 'neutral' | 'info' | 'warning' | 'success' | 'danger';
type Tab = 'tasks' | 'timesheets' | 'files';

@Component({
  selector: 'lmp-user-project-detail',
  standalone: true,
  imports: [DatePipe, NgClass, RouterLink, LucideAngularModule, HlmButton],
  template: `
    @if (loading()) {
      <div class="flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
      </div>
    } @else if (!project()) {
      <div class="flex flex-col items-center justify-center rounded-md border border-(--border) bg-(--card) py-16 text-center">
        <p class="text-sm font-medium text-(--foreground)">Projet introuvable</p>
        <a hlmBtn variant="ghost" class="mt-4 cursor-pointer" routerLink="/dashboard/projects">
          <lucide-icon [img]="ArrowLeftIcon" [size]="14" class="mr-1"></lucide-icon>
          Retour à la liste
        </a>
      </div>
    } @else {
      <div class="pb-4">
        <a routerLink="/dashboard/projects" class="inline-flex items-center gap-1 text-xs text-(--muted-foreground) hover:text-(--foreground)">
          <lucide-icon [img]="ArrowLeftIcon" [size]="12"></lucide-icon>
          Tous les projets
        </a>
        <div class="mt-3 flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="font-mono text-[11px] uppercase tracking-wider text-(--muted-foreground)">{{ project()!.id }}</p>
            <h1 class="mt-1 text-xl font-semibold text-(--foreground)">{{ project()!.projectName }}</h1>
            <p class="mt-1 max-w-2xl text-sm text-(--muted-foreground)">{{ project()!.description }}</p>
          </div>
          <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
            [ngClass]="toneClasses(statusTone(project()!.status))">
            <lucide-icon [img]="statusIcon(project()!.status)" [size]="13"></lucide-icon>
            {{ project()!.status }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3 rounded-md border border-(--border) bg-(--card) p-4 md:grid-cols-4">
        <div>
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Avancement</p>
          <div class="mt-1 flex items-center gap-2">
            <div class="h-1.5 w-full overflow-hidden rounded-full bg-(--muted)">
              <div class="h-full" [style.width.%]="project()!.percentComplete"
                [ngClass]="project()!.status === 'Completed' ? 'bg-emerald-500' : project()!.status === 'Cancelled' ? 'bg-zinc-400' : 'bg-(--primary)'"
              ></div>
            </div>
            <span class="shrink-0 text-xs font-medium text-(--foreground)">{{ project()!.percentComplete }}%</span>
          </div>
        </div>
        <div>
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Priorité</p>
          <p class="mt-0.5 inline-flex items-center gap-1 text-sm font-medium" [ngClass]="priorityClasses(project()!.priority)">
            <lucide-icon [img]="AlertTriangleIcon" [size]="12"></lucide-icon>
            {{ project()!.priority }}
          </p>
        </div>
        <div>
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Début prévu</p>
          <p class="mt-0.5 text-sm text-(--foreground)">
            {{ project()!.expectedStartDate ? (project()!.expectedStartDate | date: 'mediumDate') : '—' }}
          </p>
        </div>
        <div>
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Fin prévue</p>
          <p class="mt-0.5 text-sm text-(--foreground)">
            {{ project()!.expectedEndDate ? (project()!.expectedEndDate | date: 'mediumDate') : '—' }}
          </p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="mt-6 border-b border-(--border)">
        <div class="flex items-center gap-4">
          <button type="button" (click)="tab.set('tasks')"
            class="flex items-center gap-1.5 border-b-2 px-1 py-2 text-sm font-medium transition-colors"
            [ngClass]="tab() === 'tasks' ? 'border-(--primary) text-(--foreground)' : 'border-transparent text-(--muted-foreground) hover:text-(--foreground)'"
          >
            <lucide-icon [img]="ListChecksIcon" [size]="14"></lucide-icon>
            Tâches ({{ project()!.tasks.length }})
          </button>
          <button type="button" (click)="tab.set('timesheets')"
            class="flex items-center gap-1.5 border-b-2 px-1 py-2 text-sm font-medium transition-colors"
            [ngClass]="tab() === 'timesheets' ? 'border-(--primary) text-(--foreground)' : 'border-transparent text-(--muted-foreground) hover:text-(--foreground)'"
          >
            <lucide-icon [img]="TimerIcon" [size]="14"></lucide-icon>
            Feuilles de temps ({{ project()!.timesheets.length }})
          </button>
          <button type="button" (click)="tab.set('files')"
            class="flex items-center gap-1.5 border-b-2 px-1 py-2 text-sm font-medium transition-colors"
            [ngClass]="tab() === 'files' ? 'border-(--primary) text-(--foreground)' : 'border-transparent text-(--muted-foreground) hover:text-(--foreground)'"
          >
            <lucide-icon [img]="FileIcon" [size]="14"></lucide-icon>
            Fichiers ({{ project()!.attachments.length }})
          </button>
        </div>
      </div>

      <div class="mt-4">
        @if (tab() === 'tasks') {
          @if (project()!.tasks.length === 0) {
            <p class="rounded-md border border-dashed border-(--border) bg-(--card) p-6 text-center text-sm text-(--muted-foreground)">Aucune tâche.</p>
          } @else {
            <div class="overflow-hidden rounded-md border border-(--border) bg-(--card)">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-(--border) bg-(--muted)/40">
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Tâche</th>
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Statut</th>
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Assignée à</th>
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Échéance</th>
                  </tr>
                </thead>
                <tbody>
                  @for (t of project()!.tasks; track t.id) {
                    <tr class="border-b border-(--border) last:border-0">
                      <td class="px-4 py-3 text-(--foreground)">{{ t.subject }}</td>
                      <td class="px-4 py-3">
                        <span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
                          [ngClass]="toneClasses(taskTone(t.status))">
                          {{ t.status }}
                        </span>
                      </td>
                      <td class="px-4 py-3 text-(--muted-foreground)">{{ t.assignedTo ?? '—' }}</td>
                      <td class="px-4 py-3 text-(--muted-foreground)">
                        {{ t.expEndDate ? (t.expEndDate | date: 'mediumDate') : '—' }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        } @else if (tab() === 'timesheets') {
          @if (project()!.timesheets.length === 0) {
            <p class="rounded-md border border-dashed border-(--border) bg-(--card) p-6 text-center text-sm text-(--muted-foreground)">Aucune feuille de temps.</p>
          } @else {
            <div class="overflow-hidden rounded-md border border-(--border) bg-(--card)">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-(--border) bg-(--muted)/40">
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Activité</th>
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Date</th>
                    <th class="px-4 py-3 text-right text-[11px] uppercase tracking-wider text-(--muted-foreground)">Heures</th>
                    <th class="px-4 py-3 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  @for (ts of project()!.timesheets; track ts.id) {
                    <tr class="border-b border-(--border) last:border-0">
                      <td class="px-4 py-3 text-(--foreground)">{{ ts.activityType }}</td>
                      <td class="px-4 py-3 text-(--muted-foreground)">{{ ts.fromTime | date: 'mediumDate' }}</td>
                      <td class="px-4 py-3 text-right font-medium text-(--foreground)">{{ ts.hours }}h</td>
                      <td class="px-4 py-3 text-(--muted-foreground)">{{ ts.status }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        } @else {
          @if (project()!.attachments.length === 0) {
            <p class="rounded-md border border-dashed border-(--border) bg-(--card) p-6 text-center text-sm text-(--muted-foreground)">Aucun fichier.</p>
          } @else {
            <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
              @for (f of project()!.attachments; track f.id) {
                <div class="flex items-center justify-between gap-3 rounded-md border border-(--border) bg-(--card) p-4">
                  <div class="flex min-w-0 items-center gap-3">
                    <div class="flex h-9 w-9 items-center justify-center rounded-md bg-(--muted)">
                      <lucide-icon [img]="FileIcon" [size]="16" class="text-(--muted-foreground)"></lucide-icon>
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-medium text-(--foreground)">{{ f.fileName }}</p>
                      <p class="text-xs text-(--muted-foreground)">
                        {{ formatSize(f.fileSize) }} · {{ f.uploadedAt | date: 'mediumDate' }}
                      </p>
                    </div>
                  </div>
                  <a [href]="f.fileUrl" hlmBtn variant="ghost" size="icon" class="h-8 w-8 shrink-0 cursor-pointer">
                    <lucide-icon [img]="DownloadIcon" [size]="15"></lucide-icon>
                  </a>
                </div>
              }
            </div>
          }
        }
      </div>
    }
  `,
})
export class UserProjectDetailComponent implements OnInit {
  private readonly stub = inject(PortalStubService);
  private readonly route = inject(ActivatedRoute);

  readonly ArrowLeftIcon = ArrowLeft;
  readonly Loader2Icon = Loader2;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly DownloadIcon = Download;
  readonly FileIcon = FileIcon;
  readonly ListChecksIcon = ListChecks;
  readonly TimerIcon = Timer;

  readonly loading = signal(true);
  readonly project = signal<Project | undefined>(undefined);
  readonly tab = signal<Tab>('tasks');

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.stub.getProject(id).subscribe({
      next: (p) => { this.project.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  statusTone(s: ProjectStatus): StatusTone {
    switch (s) {
      case 'Open': return 'info';
      case 'Completed': return 'success';
      case 'Cancelled': return 'neutral';
    }
  }

  statusIcon(s: ProjectStatus) {
    switch (s) {
      case 'Completed': return CheckCircle;
      case 'Cancelled': return XCircle;
      default: return Clock;
    }
  }

  taskTone(s: TaskStatus): StatusTone {
    switch (s) {
      case 'Completed': return 'success';
      case 'Working': return 'info';
      case 'Pending Review': return 'warning';
      case 'Cancelled': return 'danger';
      case 'Open': return 'neutral';
    }
  }

  priorityClasses(p: ProjectPriority): string {
    switch (p) {
      case 'High': return 'text-red-600 dark:text-red-400';
      case 'Medium': return 'text-amber-600 dark:text-amber-400';
      case 'Low': return 'text-(--muted-foreground)';
    }
  }

  toneClasses(t: StatusTone): string {
    const map: Record<StatusTone, string> = {
      neutral: 'bg-zinc-100 text-zinc-700 dark:bg-zinc-900 dark:text-zinc-300',
      info: 'bg-blue-100 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300',
      warning: 'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
      success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
      danger: 'bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300',
    };
    return map[t];
  }
}
